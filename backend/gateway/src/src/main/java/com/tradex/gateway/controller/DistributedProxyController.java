package com.tradex.gateway.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/api/distributed")
public class DistributedProxyController {

    private final RestTemplate restTemplate;

    public DistributedProxyController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private String getNodeUrl(String nodeId) {
        // e.g. "1" or "node1" -> "http://node1:8080/api"
        String name = nodeId.startsWith("node") ? nodeId : "node" + nodeId;
        return "http://" + name + ":8080/api";
    }

    // ==========================================
    // NODE STATE & CLUSTER STATUS
    // ==========================================
    @GetMapping("/nodes")
    public ResponseEntity<List<Map<String, Object>>> getClusterNodes() {
        String[] nodes = {"node1", "node2", "node3"};
        List<Map<String, Object>> clusterInfo = new ArrayList<>();

        for (String node : nodes) {
            try {
                ResponseEntity<Map> response = restTemplate.getForEntity("http://" + node + ":8080/api/node/info", Map.class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    clusterInfo.add(response.getBody());
                }
            } catch (Exception e) {
                Map<String, Object> offlineNode = new HashMap<>();
                offlineNode.put("nodeId", node.replace("node", ""));
                offlineNode.put("nodeName", node);
                offlineNode.put("status", "OFFLINE");
                offlineNode.put("priority", Integer.parseInt(node.replace("node", "")));
                offlineNode.put("health", "DOWN");
                clusterInfo.add(offlineNode);
            }
        }
        return ResponseEntity.ok(clusterInfo);
    }

    // ==========================================
    // LAMPORT CLOCK PROXIES
    // ==========================================
    @GetMapping("/clock/{nodeId}")
    public ResponseEntity<?> getClock(@PathVariable String nodeId) {
        try {
            return restTemplate.getForEntity(getNodeUrl(nodeId) + "/clock", Map.class);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Node " + nodeId + " unreachable"));
        }
    }

    @PostMapping("/clock/event/{nodeId}")
    public ResponseEntity<?> triggerLocalEvent(@PathVariable String nodeId, @RequestBody(required = false) Map<String, String> body) {
        try {
            return restTemplate.postForEntity(getNodeUrl(nodeId) + "/clock/event", body, Map.class);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Node " + nodeId + " unreachable"));
        }
    }

    @PostMapping("/clock/send")
    public ResponseEntity<?> sendLamportMessage(@RequestBody Map<String, String> body) {
        String fromNode = body.get("fromNode");
        String toNode = body.get("toNode");
        try {
            return restTemplate.postForEntity(getNodeUrl(fromNode) + "/clock/send/" + toNode, null, Map.class);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to send message from " + fromNode + " to " + toNode));
        }
    }

    @GetMapping("/events")
    public ResponseEntity<List<Map<String, Object>>> getAggregatedEvents() {
        String[] nodes = {"node1", "node2", "node3"};
        List<Map<String, Object>> allEvents = new ArrayList<>();

        for (String node : nodes) {
            try {
                ResponseEntity<List> response = restTemplate.getForEntity("http://" + node + ":8080/api/clock/events", List.class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    allEvents.addAll(response.getBody());
                }
            } catch (Exception ignored) {}
        }
        return ResponseEntity.ok(allEvents);
    }

    // ==========================================
    // BERKELEY PROXIES
    // ==========================================
    @PostMapping("/berkeley/sync/{coordinatorNode}")
    public ResponseEntity<?> runBerkeleySync(@PathVariable String coordinatorNode) {
        try {
            return restTemplate.postForEntity(getNodeUrl(coordinatorNode) + "/clock/berkeley/synchronize", null, Map.class);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Coordinator node " + coordinatorNode + " unreachable"));
        }
    }

    // ==========================================
    // ELECTION PROXIES
    // ==========================================
    @PostMapping("/election/bully/{initiatorNode}")
    public ResponseEntity<?> runBullyElection(@PathVariable String initiatorNode) {
        try {
            return restTemplate.postForEntity(getNodeUrl(initiatorNode) + "/api/election/bully/start", null, Map.class);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Initiator node " + initiatorNode + " unreachable"));
        }
    }

    @PostMapping("/election/ring/{initiatorNode}")
    public ResponseEntity<?> runRingElection(@PathVariable String initiatorNode) {
        try {
            return restTemplate.postForEntity(getNodeUrl(initiatorNode) + "/api/election/ring/start", null, Map.class);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Initiator node " + initiatorNode + " unreachable"));
        }
    }
}
