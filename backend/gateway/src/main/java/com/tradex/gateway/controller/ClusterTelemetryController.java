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

    public ClusterTelemetryController(NodeRmiClientService rmiClientService) {
        this.rmiClientService = rmiClientService;
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
}
