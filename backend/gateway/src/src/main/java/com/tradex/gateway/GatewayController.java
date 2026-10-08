package com.tradex.gateway;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class GatewayController {

    private final RestTemplate restTemplate;

    public GatewayController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "gateway");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> systemStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("gateway", "ONLINE");
        
        String[] nodes = {"node1", "node2", "node3"};
        for (String node : nodes) {
            try {
                // In docker compose, nodes will be accessible by service name.
                // Their internal ports are 8080 according to our configuration (or mapped externally).
                // Wait, in docker, node1's internal port is server.port. 
                // Let's use 8080 for all internally in Docker, mapped differently externally if needed.
                // We'll set SERVER_PORT=8080 internally, or use the mapped ports.
                // Inside the Docker network, all nodes run on port 8080.
                int port = 8080;

                ResponseEntity<Map> response = restTemplate.getForEntity("http://" + node + ":" + port + "/api/health", Map.class);
                status.put(node, response.getStatusCode().is2xxSuccessful() ? "ONLINE" : "OFFLINE");
            } catch (Exception e) {
                status.put(node, "OFFLINE");
            }
        }
        
        return ResponseEntity.ok(status);
    }
}
