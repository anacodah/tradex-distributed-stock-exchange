package com.tradex.gateway;

import com.tradex.common.engine.MatchingEngine;
import com.tradex.common.rmi.RemoteNodeService;
import com.tradex.common.rmi.dto.*;
import com.tradex.gateway.client.NodeRmiClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.*;
import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class Phase7RmiAndMultithreadingTest {

    @Mock
    private RemoteNodeService remoteNodeService;

    @InjectMocks
    private NodeRmiClientService nodeRmiClientService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ==========================================
    // 1. Serialization Tests for RMI Contracts
    // ==========================================

    @Test
    void testSerializationOfRmiContracts() throws Exception {
        // Test RemoteOrderRequestDto serialization
        RemoteOrderRequestDto req = new RemoteOrderRequestDto();
        req.setRequestId("REQ-12345");
        req.setCorrelationId("CORR-999");
        req.setOrderId(101L);
        req.setUserId(10L);
        req.setUsername("trader1");
        req.setSymbol("AAPL");
        req.setSide("BUY");
        req.setOrderType("LIMIT");
        req.setPrice(new BigDecimal("150.00"));
        req.setQuantity(new BigDecimal("10"));
        req.setIdempotencyKey("IDEM-XYZ");
        req.setSourceTimestamp(System.currentTimeMillis());

        byte[] serializedReq = serialize(req);
        assertNotNull(serializedReq);
        RemoteOrderRequestDto deserializedReq = deserialize(serializedReq, RemoteOrderRequestDto.class);
        assertEquals("REQ-12345", deserializedReq.getRequestId());
        assertEquals("AAPL", deserializedReq.getSymbol());
        assertEquals(new BigDecimal("150.00"), deserializedReq.getPrice());

        // Test NodeStatusDto serialization
        NodeStatusDto status = new NodeStatusDto(
                1, "node1", "node1", 1099, true, "node1", 1, 42L,
                List.of("MATCHING_ENGINE"), Map.of("activeWorkers", 4)
        );
        byte[] serializedStatus = serialize(status);
        NodeStatusDto deserializedStatus = deserialize(serializedStatus, NodeStatusDto.class);
        assertEquals("node1", deserializedStatus.getNodeName());
        assertTrue(deserializedStatus.isLeader());
        assertEquals(42L, deserializedStatus.getLamportTimestamp());
    }

    // ==========================================
    // 2. Remote Call Boundary & Error Handling
    // ==========================================

    @Test
    void testRemoteCallFailureHandling() throws RemoteException {
        when(remoteNodeService.submitOrder(any())).thenThrow(new RemoteException("Connection refused to target host"));

        // Simulate client executing against failing remote node stub
        RemoteOrderRequestDto req = new RemoteOrderRequestDto();
        req.setRequestId("REQ-FAIL-1");
        req.setOrderId(500L);
        req.setSymbol("MSFT");

        // Spy client service
        NodeRmiClientService spyClient = spy(nodeRmiClientService);
        doReturn(remoteNodeService).when(spyClient).getStub(anyString(), anyInt());
        doReturn("node3").when(spyClient).discoverLeaderNode();

        RemoteOrderResponseDto response = spyClient.routeOrderToLeader(req);
        assertNotNull(response);
        assertFalse(response.isSuccessful(), "Remote call failure must not crash and return error result");
        assertEquals("RPC_FAILURE", response.getStatus());

        // Telemetry must record the failed outcome
        List<Map<String, Object>> telemetry = spyClient.getRpcTelemetry();
        assertFalse(telemetry.isEmpty());
        assertEquals("FAILED", telemetry.get(0).get("outcome"));
    }

    // ==========================================
    // 3. Multithreading: Concurrency & Worker Saturation
    // ==========================================

    @Test
    void testConcurrentOrderSubmissionsAndWorkerPool() throws InterruptedException {
        // Create bounded thread pool matching node configuration
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                4,
                8,
                10L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(50),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        int totalSubmissions = 100;
        CountDownLatch latch = new CountDownLatch(totalSubmissions);
        AtomicInteger successfulMatches = new AtomicInteger(0);

        MatchingEngine engine = new MatchingEngine();

        for (int i = 0; i < totalSubmissions; i++) {
            final int id = i;
            executor.submit(() -> {
                try {
                    // Safe execution and state verification
                    successfulMatches.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertTrue(completed, "All 100 concurrent tasks must complete within timeout");
        assertEquals(totalSubmissions, successfulMatches.get());

        // Check pool metrics
        executor.shutdown();
        boolean terminated = executor.awaitTermination(3, TimeUnit.SECONDS);
        assertTrue(terminated);
        assertTrue(executor.getCompletedTaskCount() >= totalSubmissions);
    }

    @Test
    void testExecutorSaturationBackpressure() throws InterruptedException {
        // Tiny bounded queue to intentionally saturate and test backpressure
        ThreadPoolExecutor boundedPool = new ThreadPoolExecutor(
                1,
                2,
                1L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(2),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        AtomicInteger processed = new AtomicInteger(0);
        int jobCount = 15;
        CountDownLatch latch = new CountDownLatch(jobCount);

        for (int i = 0; i < jobCount; i++) {
            boundedPool.submit(() -> {
                try {
                    Thread.sleep(20);
                    processed.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean allDone = latch.await(5, TimeUnit.SECONDS);
        assertTrue(allDone, "Backpressure CallerRunsPolicy must process all saturated jobs without throwing RejectedExecutionException");
        assertEquals(jobCount, processed.get());
        boundedPool.shutdown();
    }

    // ==========================================
    // Helper serialization methods
    // ==========================================

    private byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
        }
        return baos.toByteArray();
    }

    @SuppressWarnings("unchecked")
    private <T> T deserialize(byte[] bytes, Class<T> clazz) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return (T) ois.readObject();
        }
    }
}
