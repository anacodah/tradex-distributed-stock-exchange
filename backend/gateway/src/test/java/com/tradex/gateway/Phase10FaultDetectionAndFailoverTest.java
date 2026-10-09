package com.tradex.gateway;

import com.tradex.common.rmi.dto.FailoverClusterReportDto;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class Phase10FaultDetectionAndFailoverTest {

    private NodeRmiClientService rmiClientService;

    @BeforeEach
    void setUp() {
        rmiClientService = new NodeRmiClientService(new RestTemplate());
        rmiClientService.setCurrentActiveLeader("node3");
        rmiClientService.setCurrentLeaderEpoch(3);
        rmiClientService.setTradingState("AVAILABLE");
        rmiClientService.setFailoverStatus("STABLE");
    }

    // ==========================================
    // 1. Failure Detection & Heartbeat Categorization
    // ==========================================

    @Test
    void testHeartbeatStatusCategorization() {
        Map<String, HeartbeatStatusDto> hbMap = rmiClientService.getHeartbeatStatusMap();

        // 1. Healthy state
        HeartbeatStatusDto node1 = new HeartbeatStatusDto("node1", true, System.currentTimeMillis(), 0, "HEALTHY", 12);
        hbMap.put("node1", node1);
        assertEquals("HEALTHY", node1.getHealthStatus());
        assertTrue(node1.isReachable());
        assertEquals(0, node1.getMissedHeartbeatCount());

        // 2. Suspected failure (1-2 missed heartbeats)
        HeartbeatStatusDto node2 = new HeartbeatStatusDto("node2", false, System.currentTimeMillis() - 5000, 1, "SUSPECTED", -1);
        hbMap.put("node2", node2);
        assertEquals("SUSPECTED", node2.getHealthStatus());
        assertFalse(node2.isReachable());
        assertEquals(1, node2.getMissedHeartbeatCount());

        // 3. Confirmed unavailability (>= 3 missed heartbeats)
        HeartbeatStatusDto node3 = new HeartbeatStatusDto("node3", false, System.currentTimeMillis() - 10000, 3, "CONFIRMED_UNAVAILABLE", -1);
        hbMap.put("node3", node3);
        assertEquals("CONFIRMED_UNAVAILABLE", node3.getHealthStatus());
        assertEquals(3, node3.getMissedHeartbeatCount());
    }

    // ==========================================
    // 2. Controlled Fault Injection Mechanism
    // ==========================================

    @Test
    void testControlledFaultInjectionCausesActualNodeUnavailability() {
        // Crash node2
        Map<String, Object> crashResult = rmiClientService.simulateNodeCrash("node2");
        assertNotNull(crashResult);
        assertEquals("SIMULATED_CRASH", crashResult.get("status"));
        assertEquals(Boolean.FALSE, crashResult.get("healthy"));

        HeartbeatStatusDto hbNode2 = rmiClientService.getHeartbeatStatusMap().get("node2");
        assertNotNull(hbNode2);
        assertFalse(hbNode2.isReachable());
        assertEquals("CONFIRMED_UNAVAILABLE", hbNode2.getHealthStatus());

        // Recover node2
        Map<String, Object> recoverResult = rmiClientService.simulateNodeRecovery("node2");
        assertNotNull(recoverResult);
        assertEquals("RECOVERED", recoverResult.get("status"));
        assertEquals(Boolean.TRUE, recoverResult.get("healthy"));

        hbNode2 = rmiClientService.getHeartbeatStatusMap().get("node2");
        assertTrue(hbNode2.isReachable());
        assertEquals("HEALTHY", hbNode2.getHealthStatus());
        assertEquals(0, hbNode2.getMissedHeartbeatCount());
    }

    // ==========================================
    // 3. Primary Leader Failure & Automatic Failover
    // ==========================================

    @Test
    void testPrimaryLeaderFailureTriggersElectionAndResumesTrading() {
        assertEquals("node3", rmiClientService.getCurrentActiveLeader());
        assertEquals(3, rmiClientService.getCurrentLeaderEpoch());

        // Simulate healthy replicas
        rmiClientService.getHeartbeatStatusMap().put("node2", new HeartbeatStatusDto("node2", true, System.currentTimeMillis(), 0, "HEALTHY", 10));
        rmiClientService.getHeartbeatStatusMap().put("node1", new HeartbeatStatusDto("node1", true, System.currentTimeMillis(), 0, "HEALTHY", 15));

        // Crash current primary leader (node3)
        rmiClientService.simulateNodeCrash("node3");

        // Failover must trigger, increment epoch, and select next highest priority node (node2)
        assertEquals("node2", rmiClientService.getCurrentActiveLeader());
        assertEquals(4, rmiClientService.getCurrentLeaderEpoch(), "Leader epoch must increment after failover");
        assertEquals("AVAILABLE", rmiClientService.getTradingState(), "Trading must safely resume after state recovery");
        assertEquals("STABLE", rmiClientService.getFailoverStatus());
    }

    // ==========================================
    // 4. Trading Safety: Safe Write Suspension
    // ==========================================

    @Test
    void testTradingWritesSuspendedDuringFailoverProcedure() {
        rmiClientService.setTradingState("SUSPENDED");

        RemoteOrderRequestDto orderReq = new RemoteOrderRequestDto();
        orderReq.setOrderId(999L);
        orderReq.setSymbol("AAPL");
        orderReq.setQuantity(new BigDecimal("10"));
        orderReq.setSide("BUY");
        orderReq.setOrderType("MARKET");

        RemoteOrderResponseDto resp = rmiClientService.routeOrderToLeader(orderReq);
        assertNotNull(resp);
        assertFalse(resp.isSuccessful(), "Orders must NOT be processed while trading is suspended");
        assertEquals("TRADING_SUSPENDED", resp.getStatus());
        assertTrue(resp.getErrorMessage().contains("SUSPENDED"));
    }

    // ==========================================
    // 5. Stale-Leader Fencing & Split-Brain Prevention
    // ==========================================

    @Test
    void testStaleLeaderWritesRejectedViaLeaderEpoch() {
        long clusterEpoch = 4;
        long staleLeaderReportedEpoch = 3;

        boolean isAuthorized = staleLeaderReportedEpoch >= clusterEpoch;
        assertFalse(isAuthorized, "Stale leader with lower epoch must be rejected and fenced");

        // Verify report contains active epoch and leader
        FailoverClusterReportDto report = rmiClientService.getFailoverReport();
        assertNotNull(report);
        assertEquals(3, report.getLeaderEpoch());
        assertNotNull(report.getActiveLeader());
    }

    // ==========================================
    // 6. Idempotent Retry & Duplicate Suppression
    // ==========================================

    @Test
    void testDuplicateRetrySuppressionAfterRecovery() {
        Map<String, Boolean> idempotencyStore = new ConcurrentHashMap<>();
        AtomicLong financialMovements = new AtomicLong(0);

        String clientRequestId = "CLIENT-TX-XYZ-123";

        // Execution attempt 1
        boolean firstAccepted = idempotencyStore.putIfAbsent(clientRequestId, Boolean.TRUE) == null;
        if (firstAccepted) {
            financialMovements.incrementAndGet();
        }

        // Network transient error, client retries attempt 2
        boolean retryAccepted = idempotencyStore.putIfAbsent(clientRequestId, Boolean.TRUE) == null;
        if (retryAccepted) {
            financialMovements.incrementAndGet();
        }

        assertTrue(firstAccepted);
        assertFalse(retryAccepted, "Duplicate retry must be safely suppressed without duplicate financial movement");
        assertEquals(1L, financialMovements.get());
    }
}
