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
    private final com.tradex.node.service.VectorClockService vectorClockService;
    private final com.tradex.node.service.NodeRmiServiceImpl nodeRmiService;
    private final RestTemplate restTemplate;

    public DistributedNodeController(LamportClockService clockService,
                                     BerkeleyService berkeleyService,
                                     LeaderElectionService electionService,
                                     com.tradex.node.service.VectorClockService vectorClockService,
                                     com.tradex.node.service.NodeRmiServiceImpl nodeRmiService,
                                     RestTemplate restTemplate) {
        this.clockService = clockService;
        this.berkeleyService = berkeleyService;
        this.electionService = electionService;
        this.vectorClockService = vectorClockService;
        this.nodeRmiService = nodeRmiService;
        this.restTemplate = restTemplate;
    }

    // ==========================================
    // CONTROLLED FAULT INJECTION APIS
    // ==========================================
    @PostMapping("/fault/crash")
    public ResponseEntity<Map<String, Object>> simulateCrash() {
        nodeRmiService.setSimulatedFailure(true);
        clockService.logEvent("NODE_CRASHED", nodeName, "ALL", null, "Controlled Fault Injection: Simulating crash of " + nodeName);
        return ResponseEntity.ok(Map.of("status", "SIMULATED_CRASH", "nodeName", nodeName, "healthy", false));
    }

    @PostMapping("/fault/recover")
    public ResponseEntity<Map<String, Object>> recoverFromCrash() {
        nodeRmiService.setSimulatedFailure(false);
        clockService.logEvent("NODE_RECOVERED", nodeName, "ALL", null, "Node " + nodeName + " recovered from crash. State catch-up initialized.");
        return ResponseEntity.ok(Map.of("status", "RECOVERED", "nodeName", nodeName, "healthy", true));
    }

    // ==========================================
    // HEALTH CHECK
    // ==========================================
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        if (nodeRmiService.isSimulatedFailure()) {
            return ResponseEntity.status(503).body(Map.of(
                    "status", "DOWN",
                    "nodeId", nodeId,
                    "nodeName", nodeName,
                    "reason", "Controlled fault injection failure"
            ));
        }
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
    // BERKELEY ALGORITHM & PHYSICAL CLOCKS APIs
    // ==========================================
    @GetMapping("/clock/berkeley/status")
    public ResponseEntity<Map<String, Object>> berkeleyStatus() {
        return ResponseEntity.ok(Map.of(
                "nodeName", nodeName,
                "clockOffsetMs", berkeleyService.getClockOffsetMs(),
                "driftRateMsPerSec", berkeleyService.getDriftRateMsPerSec(),
                "simulatedPhysicalTimeMs", berkeleyService.getSimulatedPhysicalTimeMs(),
                "wallClockTimeMs", System.currentTimeMillis()
        ));
    }

    @PostMapping("/clock/berkeley/drift")
    public ResponseEntity<Map<String, Object>> configureDrift(@RequestBody Map<String, Object> body) {
        if (body.containsKey("driftRateMsPerSec")) {
            berkeleyService.setDriftRateMsPerSec(((Number) body.get("driftRateMsPerSec")).doubleValue());
        }
        if (body.containsKey("offsetMs")) {
            berkeleyService.setClockOffsetMs(((Number) body.get("offsetMs")).doubleValue());
        }
        return ResponseEntity.ok(Map.of(
                "status", "UPDATED",
                "driftRateMsPerSec", berkeleyService.getDriftRateMsPerSec(),
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
    // VECTOR CLOCK APIs
    // ==========================================
    @GetMapping("/clock/vector")
    public ResponseEntity<Map<String, Object>> getVectorClock() {
        var vc = vectorClockService.getVectorClock();
        return ResponseEntity.ok(Map.of(
                "nodeName", nodeName,
                "vector", vc.getClockMap(),
                "vectorString", vc.serialize()
        ));
    }

    @PostMapping("/clock/vector/event")
    public ResponseEntity<Map<String, Object>> createVectorEvent(@RequestBody(required = false) Map<String, String> body) {
        String desc = (body != null && body.containsKey("description")) ? body.get("description") : "Local event on " + nodeName;
        var vc = vectorClockService.recordLocalEvent(desc);
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "vector", vc.getClockMap()));
    }

    @PostMapping("/clock/vector/send/{targetNode}")
    public ResponseEntity<Map<String, Object>> sendVectorMessage(@PathVariable String targetNode) {
        var sentVc = vectorClockService.prepareSendEvent(targetNode, "MSG");
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("sourceNode", nodeName);
            payload.put("vector", sentVc.getClockMap());
            payload.put("message", "Vector ping from " + nodeName);

            ResponseEntity<Map> response = restTemplate.postForEntity("http://" + targetNode + ":8080/api/clock/vector/receive", payload, Map.class);
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "sentVector", sentVc.getClockMap(), "targetResponse", response.getBody()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "FAILED", "error", e.getMessage()));
        }
    }

    @PostMapping("/clock/vector/receive")
    public ResponseEntity<Map<String, Object>> receiveVectorMessage(@RequestBody Map<String, Object> payload) {
        String sourceNode = (String) payload.get("sourceNode");
        Map<String, Object> rawMap = (Map<String, Object>) payload.get("vector");
        Map<String, Long> converted = new HashMap<>();
        if (rawMap != null) {
            rawMap.forEach((k, v) -> converted.put(k, ((Number) v).longValue()));
        }
        com.tradex.common.model.VectorClock incoming = new com.tradex.common.model.VectorClock(converted);
        var newVc = vectorClockService.recordReceiveEvent(sourceNode, incoming, "MSG");
        return ResponseEntity.ok(Map.of(
                "status", "RECEIVED",
                "newVector", newVc.getClockMap()
        ));
    }

    @GetMapping("/clock/vector/events")
    public ResponseEntity<List<Map<String, Object>>> getVectorEvents() {
        return ResponseEntity.ok(vectorClockService.getVectorEventHistory());
    }

    @PostMapping("/clock/vector/compare")
    public ResponseEntity<Map<String, Object>> compareVectors(@RequestBody Map<String, Object> body) {
        Map<String, Object> v1Map = (Map<String, Object>) body.get("v1");
        Map<String, Object> v2Map = (Map<String, Object>) body.get("v2");

        Map<String, Long> c1 = new HashMap<>();
        Map<String, Long> c2 = new HashMap<>();
        if (v1Map != null) v1Map.forEach((k, v) -> c1.put(k, ((Number) v).longValue()));
        if (v2Map != null) v2Map.forEach((k, v) -> c2.put(k, ((Number) v).longValue()));

        var vc1 = new com.tradex.common.model.VectorClock(c1);
        var vc2 = new com.tradex.common.model.VectorClock(c2);

        var relation = vc1.compareCausality(vc2);
        return ResponseEntity.ok(Map.of(
                "v1", vc1.getClockMap(),
                "v2", vc2.getClockMap(),
                "relationship", relation.name(),
                "v1HappenedBeforeV2", relation == com.tradex.common.model.VectorClock.CausalOrder.BEFORE,
                "isConcurrent", relation == com.tradex.common.model.VectorClock.CausalOrder.CONCURRENT
        ));
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
    public ResponseEntity<com.tradex.common.rmi.dto.ElectionExecutionDto> startBully() {
        return ResponseEntity.ok(electionService.startBullyElection());
    }

    @PostMapping("/election/bully/receive-election")
    public ResponseEntity<Map<String, Object>> receiveBullyElection(@RequestBody Map<String, Object> body) {
        String initiator = (String) body.get("initiator");
        clockService.logEvent("ELECTION", initiator, nodeName, null, "Received Bully election from " + initiator + ", responding OK");
        return ResponseEntity.ok(Map.of("ok", true, "responder", nodeName));
    }

    @PostMapping("/election/bully/coordinator")
    public ResponseEntity<Map<String, Object>> receiveBullyCoordinator(@RequestBody Map<String, Object> body) {
        String newLeader = (String) body.get("leader");
        long epoch = body.containsKey("epoch") ? ((Number) body.get("epoch")).longValue() : electionService.getLeaderEpoch();
        boolean accepted = electionService.updateLeader(newLeader, epoch);
        clockService.logEvent("LEADER_CHANGED", newLeader, nodeName, null, "New Bully Leader: " + newLeader + " (Epoch #" + epoch + ", accepted=" + accepted + ")");
        return ResponseEntity.ok(Map.of("status", accepted ? "ACKNOWLEDGED" : "STALE_REJECTED", "currentLeader", newLeader, "epoch", epoch));
    }

    @PostMapping("/election/ring/start")
    public ResponseEntity<com.tradex.common.rmi.dto.ElectionExecutionDto> startRing() {
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
        long epoch = body.containsKey("epoch") ? ((Number) body.get("epoch")).longValue() : electionService.getLeaderEpoch();
        boolean accepted = electionService.updateLeader(newLeader, epoch);
        clockService.logEvent("LEADER_CHANGED", newLeader, nodeName, null, "New Ring Leader: " + newLeader + " (Epoch #" + epoch + ", accepted=" + accepted + ")");
        return ResponseEntity.ok(Map.of("status", accepted ? "ACKNOWLEDGED" : "STALE_REJECTED", "currentLeader", newLeader, "epoch", epoch));
    }

    @GetMapping("/election/events")
    public ResponseEntity<List<com.tradex.common.rmi.dto.ElectionExecutionDto>> getElectionHistory() {
        return ResponseEntity.ok(electionService.getElectionHistory());
    }
}
