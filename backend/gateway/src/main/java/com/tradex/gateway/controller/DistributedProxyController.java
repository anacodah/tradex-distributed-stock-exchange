package com.tradex.gateway.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/api/distributed")
public class DistributedProxyController {

    private final RestTemplate restTemplate;
    private final com.tradex.gateway.client.NodeRmiClientService rmiClientService;

    public DistributedProxyController(RestTemplate restTemplate, com.tradex.gateway.client.NodeRmiClientService rmiClientService) {
        this.restTemplate = restTemplate;
        this.rmiClientService = rmiClientService;
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

    @PostMapping("/berkeley/drift/{nodeId}")
    public ResponseEntity<?> configureDrift(@PathVariable String nodeId, @RequestBody Map<String, Object> body) {
        try {
            return restTemplate.postForEntity(getNodeUrl(nodeId) + "/clock/berkeley/drift", body, Map.class);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Node " + nodeId + " unreachable"));
        }
    }

    @GetMapping("/berkeley/history/{nodeId}")
    public ResponseEntity<?> getBerkeleyHistory(@PathVariable String nodeId) {
        try {
            return restTemplate.getForEntity(getNodeUrl(nodeId) + "/clock/berkeley/history", List.class);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Node " + nodeId + " unreachable"));
        }
    }

    // ==========================================
    // VECTOR CLOCK PROXIES
    // ==========================================
    @GetMapping("/vector/{nodeId}")
    public ResponseEntity<?> getVectorClock(@PathVariable String nodeId) {
        try {
            return restTemplate.getForEntity(getNodeUrl(nodeId) + "/clock/vector", Map.class);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Node " + nodeId + " unreachable"));
        }
    }

    @PostMapping("/vector/event/{nodeId}")
    public ResponseEntity<?> triggerVectorEvent(@PathVariable String nodeId, @RequestBody(required = false) Map<String, String> body) {
        try {
            return restTemplate.postForEntity(getNodeUrl(nodeId) + "/clock/vector/event", body, Map.class);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Node " + nodeId + " unreachable"));
        }
    }

    @PostMapping("/vector/send")
    public ResponseEntity<?> sendVectorMessage(@RequestBody Map<String, String> body) {
        String fromNode = body.get("fromNode");
        String toNode = body.get("toNode");
        try {
            return restTemplate.postForEntity(getNodeUrl(fromNode) + "/clock/vector/send/" + toNode, null, Map.class);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to send vector message from " + fromNode + " to " + toNode));
        }
    }

    @GetMapping("/vector/events/{nodeId}")
    public ResponseEntity<?> getVectorEvents(@PathVariable String nodeId) {
        try {
            return restTemplate.getForEntity(getNodeUrl(nodeId) + "/clock/vector/events", List.class);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Node " + nodeId + " unreachable"));
        }
    }

    @PostMapping("/vector/compare")
    public ResponseEntity<?> compareVectorClocks(@RequestBody Map<String, Object> body) {
        try {
            return restTemplate.postForEntity(getNodeUrl("node1") + "/clock/vector/compare", body, Map.class);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Comparison failed: " + e.getMessage()));
        }
    }

    // ==========================================
    // ELECTION PROXIES (Phase 11 Shared Leader Management)
    // ==========================================
    @PostMapping("/election/bully/{initiatorNode}")
    public ResponseEntity<?> runBullyElection(@PathVariable String initiatorNode) {
        try {
            com.tradex.common.rmi.dto.ElectionExecutionDto res = rmiClientService.triggerElection("BULLY", initiatorNode, "Operator initiated Bully election via " + initiatorNode);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Bully election failed: " + e.getMessage()));
        }
    }

    @PostMapping("/election/ring/{initiatorNode}")
    public ResponseEntity<?> runRingElection(@PathVariable String initiatorNode) {
        try {
            com.tradex.common.rmi.dto.ElectionExecutionDto res = rmiClientService.triggerElection("RING", initiatorNode, "Operator initiated Ring election via " + initiatorNode);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Ring election failed: " + e.getMessage()));
        }
    }

    @GetMapping("/election/events")
    public ResponseEntity<?> getElectionEvents() {
        try {
            String leader = rmiClientService.getCurrentActiveLeader();
            return restTemplate.getForEntity("http://" + leader + ":8080/api/election/events", List.class);
        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }
    }
}
