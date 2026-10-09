package com.tradex.gateway.controller;

import com.tradex.common.rmi.dto.NodeStatusDto;
import com.tradex.gateway.client.NodeRmiClientService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cluster")
public class ClusterTelemetryController {

    private final NodeRmiClientService rmiClientService;
    private final com.tradex.gateway.service.DistributedLoadBalancerService loadBalancerService;

    public ClusterTelemetryController(NodeRmiClientService rmiClientService,
                                      com.tradex.gateway.service.DistributedLoadBalancerService loadBalancerService) {
        this.rmiClientService = rmiClientService;
        this.loadBalancerService = loadBalancerService;
    }

    /**
     * Get real-time health, role, and worker pool metrics for all nodes in the cluster.
     */
    @GetMapping("/nodes")
    public ResponseEntity<List<NodeStatusDto>> getClusterNodes() {
        return ResponseEntity.ok(rmiClientService.getClusterStatus());
    }

    /**
     * Get real-time inter-node RMI invocation telemetry (source, destination, latency, method, outcome).
     */
    @GetMapping("/telemetry")
    public ResponseEntity<List<Map<String, Object>>> getRpcTelemetry() {
        return ResponseEntity.ok(rmiClientService.getRpcTelemetry());
    }

    /**
     * Get authoritative leader node name.
     */
    @GetMapping("/leader")
    public ResponseEntity<Map<String, String>> getLeader() {
        String leader = rmiClientService.discoverLeaderNode();
        return ResponseEntity.ok(Map.of("leader", leader));
    }

    /**
     * Get real-time Primary-Backup replication status, sequence numbers, and lag monitoring.
     */
    @GetMapping("/replication")
    public ResponseEntity<com.tradex.common.rmi.dto.ClusterReplicationStatusDto> getReplicationStatus() {
        return ResponseEntity.ok(rmiClientService.getReplicationStatus());
    }

    /**
     * Get real-time cluster failover, heartbeats, leader epoch, and trading availability.
     */
    @GetMapping("/failover")
    public ResponseEntity<com.tradex.common.rmi.dto.FailoverClusterReportDto> getFailoverReport() {
        return ResponseEntity.ok(rmiClientService.getFailoverReport());
    }

    /**
     * Controlled fault injection: simulate node crash / network partition.
     */
    @PostMapping("/fault/crash/{nodeId}")
    public ResponseEntity<Map<String, Object>> simulateCrash(@PathVariable String nodeId) {
        return ResponseEntity.ok(rmiClientService.simulateNodeCrash(nodeId));
    }

    /**
     * Controlled fault injection: recover node from simulated crash.
     */
    @PostMapping("/fault/recover/{nodeId}")
    public ResponseEntity<Map<String, Object>> simulateRecovery(@PathVariable String nodeId) {
        return ResponseEntity.ok(rmiClientService.simulateNodeRecovery(nodeId));
    }

    /**
     * Get real-time load balancing metrics and routing logs.
     */
    @GetMapping("/load-balancer")
    public ResponseEntity<com.tradex.common.rmi.dto.LoadBalancerReportDto> getLoadBalancerReport() {
        return ResponseEntity.ok(loadBalancerService.getReport());
    }

    /**
     * Set active backend load balancing algorithm (ROUND_ROBIN or LEAST_CONNECTIONS).
     */
    @PostMapping("/load-balancer/algorithm")
    public ResponseEntity<Map<String, String>> setAlgorithm(@RequestBody Map<String, String> body) {
        String algo = body.get("algorithm");
        loadBalancerService.setAlgorithm(algo);
        return ResponseEntity.ok(Map.of("algorithm", loadBalancerService.getAlgorithm(), "status", "UPDATED"));
    }

    /**
     * Trigger safe non-financial load test requests across cluster nodes.
     */
    @PostMapping("/load-balancer/load-test")
    public ResponseEntity<Map<String, Object>> triggerLoadTest(@RequestBody(required = false) Map<String, Object> body) {
        int count = body != null && body.containsKey("count") ? ((Number) body.get("count")).intValue() : 50;
        int concurrency = body != null && body.containsKey("concurrency") ? ((Number) body.get("concurrency")).intValue() : 5;
        return ResponseEntity.ok(loadBalancerService.runSafeLoadTest(count, concurrency));
    }
}
