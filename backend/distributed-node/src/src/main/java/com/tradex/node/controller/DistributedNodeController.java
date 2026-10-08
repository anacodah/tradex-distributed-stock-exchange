package com.tradex.node.controller;

import com.tradex.node.entity.DistributedEvent;
import com.tradex.node.service.BerkeleyService;
import com.tradex.node.service.LamportClockService;
import com.tradex.node.service.LeaderElectionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/api")
public class DistributedNodeController {

    @Value("${node.id:1}")
    private String nodeId;

    @Value("${node.name:node1}")
    private String nodeName;

    private final LamportClockService clockService;
    private final BerkeleyService berkeleyService;
    private final LeaderElectionService electionService;
    private final RestTemplate restTemplate;

    public DistributedNodeController(LamportClockService clockService,
                                     BerkeleyService berkeleyService,
                                     LeaderElectionService electionService,
                                     RestTemplate restTemplate) {
        this.clockService = clockService;
        this.berkeleyService = berkeleyService;
        this.electionService = electionService;
        this.restTemplate = restTemplate;
    }

    // ==========================================
    // HEALTH CHECK
    // ==========================================
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "nodeId", nodeId,
                "nodeName", nodeName
        ));
    }

    // ==========================================
    // EXTENDED NODE INFO
    // ==========================================
    @GetMapping("/node/info")
    public ResponseEntity<Map<String, Object>> nodeInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("nodeId", nodeId);
        info.put("nodeName", nodeName);
        info.put("status", "ONLINE");
        info.put("priority", electionService.getPriority());
        info.put("leader", electionService.getCurrentLeader());
        info.put("isLeader", electionService.isLeader());
        info.put("lamportClock", clockService.getClock());
        info.put("clockOffsetMs", berkeleyService.getClockOffsetMs());
        info.put("health", "GOOD");
        info.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(info);
    }

    // ==========================================
    // LAMPORT CLOCK APIs
    // ==========================================
    @GetMapping("/clock")
    public ResponseEntity<Map<String, Object>> getClock() {
        return ResponseEntity.ok(Map.of(
                "nodeId", nodeId,
                "nodeName", nodeName,
                "lamportTimestamp", clockService.getClock()
        ));
    }

    @GetMapping("/clock/events")
    public ResponseEntity<List<DistributedEvent>> getClockEvents() {
        return ResponseEntity.ok(clockService.getRecentEvents());
    }

    @PostMapping("/clock/event")
    public ResponseEntity<DistributedEvent> createLocalEvent(@RequestBody(required = false) Map<String, String> body) {
        String desc = (body != null && body.containsKey("description")) ? body.get("description") : "Manual local event triggered";
        return ResponseEntity.ok(clockService.recordLocalEvent(desc));
    }

    @PostMapping("/clock/send/{targetNode}")
    public ResponseEntity<Map<String, Object>> sendMessage(@PathVariable String targetNode) {
        long sentTimestamp = clockService.prepareSendEvent(targetNode, "Message sent to " + targetNode);

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("sourceNode", nodeName);
            payload.put("sentTimestamp", sentTimestamp);
            payload.put("message", "Hello from " + nodeName);

            ResponseEntity<Map> response = restTemplate.postForEntity("http://" + targetNode + ":8080/api/clock/receive", payload, Map.class);
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "sentTimestamp", sentTimestamp, "targetResponse", response.getBody()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "FAILED", "error", e.getMessage()));
        }
    }

    @PostMapping("/clock/receive")
    public ResponseEntity<Map<String, Object>> receiveMessage(@RequestBody Map<String, Object> payload) {
        String sourceNode = (String) payload.get("sourceNode");
        long sentTimestamp = ((Number) payload.get("sentTimestamp")).longValue();
        String msg = (String) payload.get("message");

        DistributedEvent event = clockService.recordReceiveEvent(sourceNode, sentTimestamp, "Received message from " + sourceNode + ": " + msg);

        return ResponseEntity.ok(Map.of(
                "status", "RECEIVED",
                "newClock", event.getLamportTimestamp(),
                "nodeName", nodeName
        ));
    }

    // ==========================================
    // BERKELEY ALGORITHM APIs
    // ==========================================
    @GetMapping("/clock/berkeley/status")
    public ResponseEntity<Map<String, Object>> berkeleyStatus() {
        return ResponseEntity.ok(Map.of(
                "nodeName", nodeName,
                "clockOffsetMs", berkeleyService.getClockOffsetMs()
        ));
    }

    @PostMapping("/clock/berkeley/synchronize")
    public ResponseEntity<Map<String, Object>> berkeleySync() {
        return ResponseEntity.ok(berkeleyService.synchronizeClocks());
    }

    @PostMapping("/clock/berkeley/adjust")
    public ResponseEntity<Map<String, Object>> berkeleyAdjust(@RequestBody Map<String, Object> body) {
        double adjustmentMs = ((Number) body.get("adjustmentMs")).doubleValue();
        berkeleyService.applyAdjustment(adjustmentMs);
        return ResponseEntity.ok(Map.of("status", "ADJUSTED", "newOffsetMs", berkeleyService.getClockOffsetMs()));
    }

    @GetMapping("/clock/berkeley/history")
    public ResponseEntity<List<Map<String, Object>>> berkeleyHistory() {
        return ResponseEntity.ok(berkeleyService.getSyncHistory());
    }

    // ==========================================
    // LEADER ELECTION APIs
    // ==========================================
    @GetMapping("/election/status")
    public ResponseEntity<Map<String, Object>> electionStatus() {
        return ResponseEntity.ok(Map.of(
                "nodeId", nodeId,
                "nodeName", nodeName,
                "priority", electionService.getPriority(),
                "currentLeader", electionService.getCurrentLeader(),
                "isLeader", electionService.isLeader()
        ));
    }

    @PostMapping("/election/bully/start")
    public ResponseEntity<Map<String, Object>> startBully() {
        return ResponseEntity.ok(electionService.startBullyElection());
    }

    @PostMapping("/election/bully/receive-election")
    public ResponseEntity<Map<String, Object>> receiveBullyElection(@RequestBody Map<String, Object> body) {
        String initiator = (String) body.get("initiator");
        // Higher priority node responds OK
        clockService.logEvent("ELECTION", initiator, nodeName, null, "Received Bully election from " + initiator + ", responding OK");
        return ResponseEntity.ok(Map.of("ok", true, "responder", nodeName));
    }

    @PostMapping("/election/bully/coordinator")
    public ResponseEntity<Map<String, Object>> receiveBullyCoordinator(@RequestBody Map<String, Object> body) {
        String newLeader = (String) body.get("leader");
        electionService.setLeader(newLeader);
        clockService.logEvent("LEADER_CHANGED", newLeader, nodeName, null, "New Bully Leader announced: " + newLeader);
        return ResponseEntity.ok(Map.of("status", "ACKNOWLEDGED", "currentLeader", newLeader));
    }

    @PostMapping("/election/ring/start")
    public ResponseEntity<Map<String, Object>> startRing() {
        return ResponseEntity.ok(electionService.startRingElection());
    }

    @PostMapping("/election/ring/pass")
    public ResponseEntity<Map<String, Object>> passRingToken(@RequestBody Map<String, Object> body) {
        List<Integer> candidates = new ArrayList<>((List<Integer>) body.get("candidates"));
        List<String> ringPath = new ArrayList<>((List<String>) body.get("ringPath"));

        candidates.add(electionService.getPriority());
        ringPath.add(nodeName);

        clockService.logEvent("ELECTION", (String) body.get("originNode"), nodeName, null, "Ring token passed to " + nodeName);

        return ResponseEntity.ok(Map.of(
                "candidates", candidates,
                "ringPath", ringPath
        ));
    }

    @PostMapping("/election/ring/coordinator")
    public ResponseEntity<Map<String, Object>> receiveRingCoordinator(@RequestBody Map<String, Object> body) {
        String newLeader = (String) body.get("leader");
        electionService.setLeader(newLeader);
        clockService.logEvent("LEADER_CHANGED", newLeader, nodeName, null, "New Ring Leader announced: " + newLeader);
        return ResponseEntity.ok(Map.of("status", "ACKNOWLEDGED", "currentLeader", newLeader));
    }

    @GetMapping("/election/events")
    public ResponseEntity<List<Map<String, Object>>> getElectionHistory() {
        return ResponseEntity.ok(electionService.getElectionHistory());
    }
}
