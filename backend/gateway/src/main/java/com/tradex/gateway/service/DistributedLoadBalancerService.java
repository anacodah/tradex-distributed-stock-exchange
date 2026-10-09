package com.tradex.gateway.service;

import com.tradex.common.rmi.dto.HeartbeatStatusDto;
import com.tradex.common.rmi.dto.LoadBalancerReportDto;
import com.tradex.common.rmi.dto.NodeLoadStatsDto;
import com.tradex.gateway.client.NodeRmiClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class DistributedLoadBalancerService {

    private static final Logger log = LoggerFactory.getLogger(DistributedLoadBalancerService.class);

    private final NodeRmiClientService rmiClientService;

    // Supported algorithms: ROUND_ROBIN, LEAST_CONNECTIONS
    private volatile String algorithm = "LEAST_CONNECTIONS";

    // Round-robin counter
    private final AtomicInteger roundRobinIndex = new AtomicInteger(0);

    // Real per-node execution metrics
    private final Map<String, NodeTracker> nodeTrackers = new ConcurrentHashMap<>();

    // Log of recent routing decisions (max 50)
    private final List<Map<String, Object>> recentRoutingDecisions = new CopyOnWriteArrayList<>();

    private final AtomicLong totalDispatched = new AtomicLong(0);

    private static class NodeTracker {
        final String nodeName;
        final AtomicLong totalRequests = new AtomicLong(0);
        final AtomicInteger activeRequests = new AtomicInteger(0);
        final AtomicLong completedRequests = new AtomicLong(0);
        final AtomicLong failedRequests = new AtomicLong(0);
        final AtomicLong totalLatencyMs = new AtomicLong(0);
        volatile long lastRequestTimestamp = System.currentTimeMillis();

        NodeTracker(String nodeName) {
            this.nodeName = nodeName;
        }
    }

    public DistributedLoadBalancerService(NodeRmiClientService rmiClientService) {
        this.rmiClientService = rmiClientService;
        for (String node : List.of("node1", "node2", "node3")) {
            nodeTrackers.put(node, new NodeTracker(node));
        }
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public synchronized void setAlgorithm(String algorithm) {
        if ("ROUND_ROBIN".equalsIgnoreCase(algorithm) || "LEAST_CONNECTIONS".equalsIgnoreCase(algorithm)) {
            this.algorithm = algorithm.toUpperCase();
            log.info("Distributed load balancing algorithm set to: {}", this.algorithm);
        } else {
            throw new IllegalArgumentException("Unknown algorithm: " + algorithm);
        }
    }

    /**
     * Route a request based on category and current policy.
     * @param category: READ_ONLY_MARKET_DATA, LEADER_ONLY_WRITE, ANALYTICS
     */
    public String selectTargetNode(String category) {
        String leader = rmiClientService.discoverLeaderNode();

        // 1. Leader-only operations (Authoritative order matching and critical state writes)
        if ("LEADER_ONLY_WRITE".equalsIgnoreCase(category)) {
            recordRoutingDecision(category, leader, "STRICT_LEADER_ROUTING", "Critical state write or order execution must target authoritative leader");
            return leader;
        }

        // 2. Read-only or analytics requests: Balance across healthy nodes
        List<String> healthyNodes = getHealthyCandidateNodes();
        if (healthyNodes.isEmpty()) {
            log.warn("No healthy nodes available in load balancer. Falling back to leader: {}", leader);
            recordRoutingDecision(category, leader, "EMERGENCY_FALLBACK", "No replica available; falling back to leader");
            return leader;
        }

        String target;
        String reason;
        if ("ROUND_ROBIN".equalsIgnoreCase(algorithm)) {
            int idx = Math.abs(roundRobinIndex.getAndIncrement() % healthyNodes.size());
            target = healthyNodes.get(idx);
            reason = "Round-robin sequence selection (index " + idx + ")";
        } else {
            // LEAST_CONNECTIONS: Pick healthy node with lowest active concurrent requests
            target = healthyNodes.stream()
                    .min(Comparator.comparingInt(n -> nodeTrackers.get(n).activeRequests.get()))
                    .orElse(healthyNodes.get(0));
            int active = nodeTrackers.get(target).activeRequests.get();
            reason = "Least active concurrent requests (" + active + " active)";
        }

        recordRoutingDecision(category, target, algorithm, reason);
        return target;
    }

    private List<String> getHealthyCandidateNodes() {
        List<String> list = new ArrayList<>();
        Map<String, HeartbeatStatusDto> hbMap = rmiClientService.getHeartbeatStatusMap();
        for (String node : List.of("node1", "node2", "node3")) {
            HeartbeatStatusDto hb = hbMap.get(node);
            // Considered healthy if reachable and not confirmed unavailable
            if (hb == null || (hb.isReachable() && !"CONFIRMED_UNAVAILABLE".equals(hb.getHealthStatus()))) {
                list.add(node);
            }
        }
        return list;
    }

    public void startRequest(String targetNode) {
        NodeTracker tracker = nodeTrackers.get(targetNode);
        if (tracker != null) {
            tracker.totalRequests.incrementAndGet();
            tracker.activeRequests.incrementAndGet();
            tracker.lastRequestTimestamp = System.currentTimeMillis();
            totalDispatched.incrementAndGet();
        }
    }

    public void completeRequest(String targetNode, long latencyMs, boolean successful) {
        NodeTracker tracker = nodeTrackers.get(targetNode);
        if (tracker != null) {
            tracker.activeRequests.decrementAndGet();
            if (tracker.activeRequests.get() < 0) {
                tracker.activeRequests.set(0);
            }
            if (successful) {
                tracker.completedRequests.incrementAndGet();
            } else {
                tracker.failedRequests.incrementAndGet();
            }
            tracker.totalLatencyMs.addAndGet(Math.max(1, latencyMs));
        }
    }

    private void recordRoutingDecision(String category, String targetNode, String strategy, String reason) {
        Map<String, Object> item = new HashMap<>();
        item.put("timestamp", System.currentTimeMillis());
        item.put("category", category);
        item.put("targetNode", targetNode);
        item.put("strategy", strategy);
        item.put("reason", reason);

        recentRoutingDecisions.add(0, item);
        if (recentRoutingDecisions.size() > 50) {
            recentRoutingDecisions.remove(recentRoutingDecisions.size() - 1);
        }
    }

    public LoadBalancerReportDto getReport() {
        LoadBalancerReportDto report = new LoadBalancerReportDto();
        report.setCurrentAlgorithm(algorithm);
        String leader = rmiClientService.discoverLeaderNode();
        report.setAuthoritativeLeader(leader);
        report.setLeaderEpoch(rmiClientService.getCurrentLeaderEpoch());
        report.setTotalDispatchedRequests(totalDispatched.get());
        report.setTimestamp(System.currentTimeMillis());
        report.setRecentRoutingDecisions(new ArrayList<>(recentRoutingDecisions));

        Map<String, HeartbeatStatusDto> hbMap = rmiClientService.getHeartbeatStatusMap();
        Map<String, NodeLoadStatsDto> statsMap = new HashMap<>();

        for (String node : List.of("node1", "node2", "node3")) {
            NodeTracker tracker = nodeTrackers.get(node);
            HeartbeatStatusDto hb = hbMap.get(node);
            boolean healthy = hb == null || (hb.isReachable() && !"CONFIRMED_UNAVAILABLE".equals(hb.getHealthStatus()));

            NodeLoadStatsDto dto = new NodeLoadStatsDto();
            dto.setNodeName(node);
            dto.setHealthy(healthy);
            dto.setRole(node.equals(leader) ? "PRIMARY_LEADER" : "FOLLOWER_REPLICA");

            if (tracker != null) {
                long total = tracker.totalRequests.get();
                long completed = tracker.completedRequests.get();
                long failed = tracker.failedRequests.get();
                long totLat = tracker.totalLatencyMs.get();

                dto.setTotalRequests(total);
                dto.setActiveRequests(tracker.activeRequests.get());
                dto.setCompletedRequests(completed);
                dto.setFailedRequests(failed);
                dto.setLastRequestTimestamp(tracker.lastRequestTimestamp);

                long finished = completed + failed;
                dto.setAvgLatencyMs(finished > 0 ? (double) totLat / finished : 0.0);
                dto.setErrorRatePercent(finished > 0 ? ((double) failed / finished) * 100.0 : 0.0);
            }

            statsMap.put(node, dto);
        }

        report.setNodeStats(statsMap);
        return report;
    }

    /**
     * Run simulated safe non-financial load test requests across cluster.
     */
    public Map<String, Object> runSafeLoadTest(int requestCount, int concurrency) {
        int count = Math.min(Math.max(requestCount, 1), 200);
        long start = System.currentTimeMillis();

        Map<String, Integer> distribution = new HashMap<>();
        for (String n : List.of("node1", "node2", "node3")) {
            distribution.put(n, 0);
        }

        Random rand = new Random();
        for (int i = 0; i < count; i++) {
            // Categorize into READ_ONLY_MARKET_DATA (90%) and LEADER_ONLY_WRITE (10%)
            String category = (i % 10 == 0) ? "LEADER_ONLY_WRITE" : "READ_ONLY_MARKET_DATA";
            String target = selectTargetNode(category);

            startRequest(target);
            long latency = 5 + rand.nextInt(25);
            completeRequest(target, latency, true);

            distribution.put(target, distribution.getOrDefault(target, 0) + 1);
        }

        long elapsed = System.currentTimeMillis() - start;
        return Map.of(
                "totalRequests", count,
                "algorithm", algorithm,
                "elapsedMs", elapsed,
                "distribution", distribution,
                "status", "COMPLETED"
        );
    }
}
