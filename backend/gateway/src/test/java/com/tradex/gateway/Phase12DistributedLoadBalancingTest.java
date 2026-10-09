package com.tradex.gateway;

import com.tradex.common.rmi.dto.HeartbeatStatusDto;
import com.tradex.common.rmi.dto.LoadBalancerReportDto;
import com.tradex.common.rmi.dto.NodeLoadStatsDto;
import com.tradex.gateway.client.NodeRmiClientService;
import com.tradex.gateway.service.DistributedLoadBalancerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class Phase12DistributedLoadBalancingTest {

    private NodeRmiClientService rmiClientService;
    private DistributedLoadBalancerService loadBalancerService;

    @BeforeEach
    void setUp() {
        rmiClientService = new NodeRmiClientService(new RestTemplate());
        rmiClientService.setCurrentActiveLeader("node3");
        rmiClientService.setCurrentLeaderEpoch(3);
        rmiClientService.setTradingState("AVAILABLE");
        rmiClientService.setFailoverStatus("STABLE");

        // Mark all 3 nodes healthy initially
        rmiClientService.getHeartbeatStatusMap().put("node1", new HeartbeatStatusDto("node1", true, System.currentTimeMillis(), 0, "HEALTHY", 10));
        rmiClientService.getHeartbeatStatusMap().put("node2", new HeartbeatStatusDto("node2", true, System.currentTimeMillis(), 0, "HEALTHY", 12));
        rmiClientService.getHeartbeatStatusMap().put("node3", new HeartbeatStatusDto("node3", true, System.currentTimeMillis(), 0, "HEALTHY", 8));

        loadBalancerService = new DistributedLoadBalancerService(rmiClientService);
    }

    // ==========================================
    // 1. Strict Leader-Only Write Routing
    // ==========================================

    @Test
    void testLeaderOnlyWriteOperationsRouteStrictlyToCurrentLeader() {
        assertEquals("node3", rmiClientService.getCurrentActiveLeader());

        String target = loadBalancerService.selectTargetNode("LEADER_ONLY_WRITE");
        assertEquals("node3", target, "Authoritative order matching and state writes must strictly target current leader");

        // Transition leader to node2
        rmiClientService.setCurrentActiveLeader("node2");
        String newTarget = loadBalancerService.selectTargetNode("LEADER_ONLY_WRITE");
        assertEquals("node2", newTarget, "Updated leader must receive authoritative write operations");
    }

    // ==========================================
    // 2. Round-Robin Distribution for Read Operations
    // ==========================================

    @Test
    void testRoundRobinDistributionAcrossHealthyNodes() {
        loadBalancerService.setAlgorithm("ROUND_ROBIN");
        assertEquals("ROUND_ROBIN", loadBalancerService.getAlgorithm());

        List<String> selections = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            selections.add(loadBalancerService.selectTargetNode("READ_ONLY_MARKET_DATA"));
        }

        // Must balance across nodes
        assertTrue(selections.contains("node1"));
        assertTrue(selections.contains("node2"));
        assertTrue(selections.contains("node3"));
    }

    // ==========================================
    // 3. Health-Aware Exclusion of Unhealthy Nodes
    // ==========================================

    @Test
    void testExclusionOfUnhealthyNodeFromDistribution() {
        loadBalancerService.setAlgorithm("ROUND_ROBIN");

        // Crash node2
        rmiClientService.simulateNodeCrash("node2");

        Set<String> selectedNodes = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            selectedNodes.add(loadBalancerService.selectTargetNode("READ_ONLY_MARKET_DATA"));
        }

        assertFalse(selectedNodes.contains("node2"), "Unhealthy/crashed node2 must be strictly excluded from routing");
        assertTrue(selectedNodes.contains("node1"));
        assertTrue(selectedNodes.contains("node3"));
    }

    // ==========================================
    // 4. Least Connections Selection Strategy
    // ==========================================

    @Test
    void testLeastConnectionsSelectsNodeWithLowestActiveRequests() {
        loadBalancerService.setAlgorithm("LEAST_CONNECTIONS");

        // Put active load on node1 and node2
        loadBalancerService.startRequest("node1");
        loadBalancerService.startRequest("node1");
        loadBalancerService.startRequest("node2");

        // node3 currently has 0 active requests -> Least Connections must select node3
        String chosen = loadBalancerService.selectTargetNode("READ_ONLY_MARKET_DATA");
        assertEquals("node3", chosen, "Node with 0 active requests must be chosen under Least Connections");

        // Complete request on node1
        loadBalancerService.completeRequest("node1", 15, true);
    }

    // ==========================================
    // 5. Per-Node Load Statistics and Report
    // ==========================================

    @Test
    void testRealTimeNodeLoadStatisticsTracking() {
        loadBalancerService.startRequest("node1");
        loadBalancerService.completeRequest("node1", 20, true);

        loadBalancerService.startRequest("node2");
        loadBalancerService.completeRequest("node2", 30, false); // failed request

        LoadBalancerReportDto report = loadBalancerService.getReport();
        assertNotNull(report);
        assertEquals("LEAST_CONNECTIONS", report.getCurrentAlgorithm());
        assertEquals("node3", report.getAuthoritativeLeader());

        NodeLoadStatsDto stats1 = report.getNodeStats().get("node1");
        assertNotNull(stats1);
        assertEquals(1L, stats1.getTotalRequests());
        assertEquals(1L, stats1.getCompletedRequests());
        assertEquals(0L, stats1.getFailedRequests());
        assertEquals(0.0, stats1.getErrorRatePercent());

        NodeLoadStatsDto stats2 = report.getNodeStats().get("node2");
        assertNotNull(stats2);
        assertEquals(1L, stats2.getTotalRequests());
        assertEquals(0L, stats2.getCompletedRequests());
        assertEquals(1L, stats2.getFailedRequests());
        assertEquals(100.0, stats2.getErrorRatePercent());
    }

    // ==========================================
    // 6. Safe Non-Financial Load Test Generator
    // ==========================================

    @Test
    void testSafeNonFinancialLoadTestExecution() {
        Map<String, Object> result = loadBalancerService.runSafeLoadTest(30, 5);

        assertNotNull(result);
        assertEquals(30, result.get("totalRequests"));
        assertEquals("COMPLETED", result.get("status"));

        Map<String, Integer> distribution = (Map<String, Integer>) result.get("distribution");
        assertNotNull(distribution);
        int totalDistributed = distribution.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(30, totalDistributed);
    }
}
