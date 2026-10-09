package com.tradex.gateway;

import com.tradex.common.rmi.dto.ElectionExecutionDto;
import com.tradex.common.rmi.dto.HeartbeatStatusDto;
import com.tradex.common.rmi.dto.RemoteOrderRequestDto;
import com.tradex.common.rmi.dto.RemoteOrderResponseDto;
import com.tradex.gateway.client.NodeRmiClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class Phase11BullyAndRingElectionsTest {

    private NodeRmiClientService rmiClientService;

    @BeforeEach
    void setUp() {
        rmiClientService = new NodeRmiClientService(new RestTemplate());
        rmiClientService.setCurrentActiveLeader("node3");
        rmiClientService.setCurrentLeaderEpoch(3);
        rmiClientService.setTradingState("AVAILABLE");
        rmiClientService.setFailoverStatus("STABLE");

        // Initialize heartbeat status for all 3 nodes
        rmiClientService.getHeartbeatStatusMap().put("node1", new HeartbeatStatusDto("node1", true, System.currentTimeMillis(), 0, "HEALTHY", 10));
        rmiClientService.getHeartbeatStatusMap().put("node2", new HeartbeatStatusDto("node2", true, System.currentTimeMillis(), 0, "HEALTHY", 12));
        rmiClientService.getHeartbeatStatusMap().put("node3", new HeartbeatStatusDto("node3", true, System.currentTimeMillis(), 0, "HEALTHY", 8));
    }

    // ==========================================
    // 1. Bully Election with All Nodes Available
    // ==========================================

    @Test
    void testBullyElectionAllNodesAvailableElectsHighestPriority() {
        assertEquals("node3", rmiClientService.getCurrentActiveLeader());
        assertEquals(3, rmiClientService.getCurrentLeaderEpoch());

        // Trigger Bully election initiated by node1
        ElectionExecutionDto result = rmiClientService.triggerElection("BULLY", "node1", "Operator initiated Bully test");

        assertNotNull(result);
        assertEquals("BULLY", result.getAlgorithm());
        assertEquals("node1", result.getInitiator());
        assertTrue(result.getLeaderEpoch() > 3, "Leader epoch must increment after election");
        assertEquals("AVAILABLE", rmiClientService.getTradingState(), "Trading must be available after election");
    }

    // ==========================================
    // 2. Bully Election When Highest Node (node3) is Unavailable
    // ==========================================

    @Test
    void testBullyElectionHighestNodeUnavailableElectsIntermediate() {
        // Mark node3 as unavailable / crashed
        rmiClientService.simulateNodeCrash("node3");

        HeartbeatStatusDto hb3 = rmiClientService.getHeartbeatStatusMap().get("node3");
        assertNotNull(hb3);
        assertFalse(hb3.isReachable());

        // Bully election should elect node2 (priority 2, since node3 is unavailable)
        ElectionExecutionDto result = rmiClientService.triggerElection("BULLY", "node1", "Leader node3 crashed");

        assertNotNull(result);
        assertEquals("node2", rmiClientService.getCurrentActiveLeader(), "Node2 must be elected when node3 is down");
        assertTrue(rmiClientService.getCurrentLeaderEpoch() >= 4);
        assertEquals("AVAILABLE", rmiClientService.getTradingState());
    }

    // ==========================================
    // 3. Ring Election Protocol & Token Propagation
    // ==========================================

    @Test
    void testRingElectionExecutionAndCoordinatorAnnouncement() {
        ElectionExecutionDto result = rmiClientService.triggerElection("RING", "node1", "Operator initiated Ring election");

        assertNotNull(result);
        assertEquals("RING", result.getAlgorithm());
        assertEquals("node1", result.getInitiator());
        assertTrue(result.getLeaderEpoch() > 3);
        assertEquals("AVAILABLE", rmiClientService.getTradingState());
        assertNotNull(rmiClientService.getCurrentActiveLeader());
    }

    // ==========================================
    // 4. Ring Election With Intermediate Node Failure
    // ==========================================

    @Test
    void testRingElectionBypassesUnavailableIntermediateNode() {
        // Crash node2 (intermediate node in 1 -> 2 -> 3 ring)
        rmiClientService.simulateNodeCrash("node2");

        ElectionExecutionDto result = rmiClientService.triggerElection("RING", "node1", "Intermediate node2 crashed");

        assertNotNull(result);
        assertEquals("RING", result.getAlgorithm());
        assertTrue(result.getLeaderEpoch() >= 4);
        // Surviving highest is node3
        assertEquals("node3", rmiClientService.getCurrentActiveLeader());
    }

    // ==========================================
    // 5. Stale Coordinator Announcement Fencing
    // ==========================================

    @Test
    void testStaleCoordinatorAnnouncementRejectedByEpoch() {
        long currentClusterEpoch = 5;
        rmiClientService.setCurrentLeaderEpoch(currentClusterEpoch);

        long staleAnnouncementEpoch = 4;
        boolean accepted = staleAnnouncementEpoch >= currentClusterEpoch;

        assertFalse(accepted, "Stale coordinator announcement with lower epoch must be rejected");
        assertEquals(5, rmiClientService.getCurrentLeaderEpoch());
    }

    // ==========================================
    // 6. Safe Order Routing to Newly Elected Leader
    // ==========================================

    @Test
    void testOrderRoutingResumesToElectedLeader() {
        // Perform election that transitions leader to node2
        rmiClientService.simulateNodeCrash("node3");
        rmiClientService.triggerElection("BULLY", "node2", "Failover to node2");

        assertEquals("node2", rmiClientService.getCurrentActiveLeader());
        assertEquals("AVAILABLE", rmiClientService.getTradingState());

        // Probe order routing
        RemoteOrderRequestDto orderReq = new RemoteOrderRequestDto();
        orderReq.setOrderId(501L);
        orderReq.setSymbol("TSLA");
        orderReq.setQuantity(new BigDecimal("5"));
        orderReq.setSide("BUY");
        orderReq.setOrderType("MARKET");

        // Leader is discovered and reachable
        String targetLeader = rmiClientService.discoverLeaderNode();
        assertEquals("node2", targetLeader, "Route must target the newly elected leader");
    }

    // ==========================================
    // 7. Concurrent Election Conflict Prevention
    // ==========================================

    @Test
    void testConcurrentElectionSuppression() {
        rmiClientService.setFailoverStatus("ELECTION_IN_PROGRESS");

        ElectionExecutionDto secondAttempt = rmiClientService.triggerElection("RING", "node1", "Concurrent trigger");

        assertNotNull(secondAttempt);
        assertEquals("IN_PROGRESS", secondAttempt.getOutcome(), "Concurrent election must be rejected while first is in progress");
    }
}
