package com.tradex.node;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class NodeController {

    @Value("${node.id:0}")
    private String nodeId;

    @Value("${node.name:unknown}")
    private String nodeName;

    @Value("${server.port:8080}")
    private int port;

    private final RestTemplate restTemplate;

    public NodeController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("node", nodeName);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/node/info")
    public ResponseEntity<Map<String, Object>> nodeInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("nodeId", nodeId);
        info.put("nodeName", nodeName);
        info.put("status", "ONLINE");
        info.put("role", "FOLLOWER");
        info.put("port", port);
        info.put("health", "GOOD");
        info.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(info);
    }

    @GetMapping("/node/ping-peers")
    public ResponseEntity<Map<String, Object>> pingPeers() {
        String[] peers = {"node1", "node2", "node3"};
        Map<String, Object> results = new HashMap<>();
        
        for (String peer : peers) {
            if (peer.equals(nodeName)) {
                results.put(peer, "SELF");
                continue;
            }
            try {
                // Inside the Docker network, all nodes run on port 8080
                int peerPort = 8080;
                
                ResponseEntity<Map> response = restTemplate.getForEntity("http://" + peer + ":" + peerPort + "/api/health", Map.class);
                results.put(peer, response.getStatusCode().is2xxSuccessful() ? "ONLINE" : "ERROR");
            } catch (Exception e) {
                results.put(peer, "OFFLINE");
            }
        }
        
        return ResponseEntity.ok(results);
    }
}
