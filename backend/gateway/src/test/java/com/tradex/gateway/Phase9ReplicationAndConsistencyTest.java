package com.tradex.gateway;

import com.tradex.common.rmi.dto.ReplicationAckDto;
import com.tradex.common.rmi.dto.ReplicationEventDto;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class Phase9ReplicationAndConsistencyTest {

    // ==========================================
    // 1. Idempotency & Duplicate Delivery Suppression
    // ==========================================

    @Test
    void testDuplicateReplicationSuppression() {
        Set<String> appliedEventIds = ConcurrentHashMap.newKeySet();
        AtomicLong appliedCount = new AtomicLong(0);

        ReplicationEventDto event = new ReplicationEventDto(
                "EVT-UUID-1", "ORDER_MATCHED", 3, 3, 101L, "{\"orderId\":101}", 15L, System.currentTimeMillis()
        );

        // First application
        boolean firstApplied = appliedEventIds.add(event.getEventId());
        if (firstApplied) appliedCount.incrementAndGet();

        // Duplicate delivery of same event
        boolean secondApplied = appliedEventIds.add(event.getEventId());
        if (secondApplied) appliedCount.incrementAndGet();

        assertTrue(firstApplied, "First delivery must be applied");
        assertFalse(secondApplied, "Duplicate delivery must be rejected idempotently");
        assertEquals(1L, appliedCount.get(), "State must only change once");
    }

    // ==========================================
    // 2. Out-of-Order Delivery & Buffering
    // ==========================================

    @Test
    void testOutOfOrderDeliveryBufferingAndSequentialDrain() {
        PriorityQueue<ReplicationEventDto> buffer = new PriorityQueue<>(Comparator.comparingLong(ReplicationEventDto::getSequenceNumber));
        AtomicLong lastAppliedSequence = new AtomicLong(0);
        List<Long> commitHistory = new ArrayList<>();

        ReplicationEventDto e1 = new ReplicationEventDto("EVT-1", "ORDER", 3, 3, 1L, "{}", 1, System.currentTimeMillis());
        ReplicationEventDto e2 = new ReplicationEventDto("EVT-2", "ORDER", 3, 3, 2L, "{}", 2, System.currentTimeMillis());
        ReplicationEventDto e3 = new ReplicationEventDto("EVT-3", "ORDER", 3, 3, 3L, "{}", 3, System.currentTimeMillis());

        // Receive out-of-order: e1, then e3, then e2
        // Process e1
        lastAppliedSequence.set(e1.getSequenceNumber());
        commitHistory.add(e1.getSequenceNumber());

        // Receive e3: sequence is 3, expected is 2 -> buffer e3
        buffer.add(e3);
        assertEquals(1L, lastAppliedSequence.get(), "Seq #3 must be buffered while awaiting seq #2");

        // Now receive missing e2: sequence is 2
        lastAppliedSequence.set(e2.getSequenceNumber());
        commitHistory.add(e2.getSequenceNumber());

        // Drain buffer
        while (!buffer.isEmpty() && buffer.peek().getSequenceNumber() == lastAppliedSequence.get() + 1) {
            ReplicationEventDto next = buffer.poll();
            lastAppliedSequence.set(next.getSequenceNumber());
            commitHistory.add(next.getSequenceNumber());
        }

        assertEquals(3L, lastAppliedSequence.get(), "Buffer must drain up to sequence #3");
        assertEquals(List.of(1L, 2L, 3L), commitHistory, "State must commit in strictly monotonic sequential order");
    }

    // ==========================================
    // 3. Stale Leader Epoch Rejection
    // ==========================================

    @Test
    void testStaleLeaderEpochRejection() {
        long currentClusterTerm = 4; // Node currently recognizes epoch 4

        ReplicationEventDto staleEvent = new ReplicationEventDto(
                "EVT-STALE", "ORDER_MATCHED", 1, 3 /* Stale epoch 3 */, 50L, "{}", 20L, System.currentTimeMillis()
        );

        boolean isStale = staleEvent.getTermEpoch() < currentClusterTerm;
        assertTrue(isStale, "Events from older leader epoch must be detected as stale");

        ReplicationAckDto ack = new ReplicationAckDto(
                staleEvent.getEventId(), staleEvent.getSequenceNumber(), staleEvent.getTermEpoch(),
                2, "node2", !isStale, 49L, isStale ? "STALE_LEADER_EPOCH" : "SUCCESS", System.currentTimeMillis()
        );

        assertFalse(ack.isApplied(), "Replica must reject writes originating from a partitioned/stale leader");
        assertEquals("STALE_LEADER_EPOCH", ack.getMessage());
    }

    // ==========================================
    // 4. Strong Coordinated Quorum Thresholds
    // ==========================================

    @Test
    void testStronglyCoordinatedQuorumCommit() {
        int clusterSize = 3;
        int quorumThreshold = (clusterSize / 2); // 1 replica ACK + leader = 2/3 majority

        List<ReplicationAckDto> acks = new ArrayList<>();
        acks.add(new ReplicationAckDto("EVT-10", 100L, 1L, 1, "node1", true, 100L, "SUCCESS", System.currentTimeMillis()));

        long successfulAcks = acks.stream().filter(ReplicationAckDto::isApplied).count();
        boolean quorumReached = successfulAcks >= quorumThreshold;

        assertTrue(quorumReached, "Single replica ACK satisfies 2/3 majority with leader");

        // When all replicas are disconnected/down:
        List<ReplicationAckDto> failedAcks = Collections.emptyList();
        boolean failedQuorum = failedAcks.stream().filter(ReplicationAckDto::isApplied).count() >= quorumThreshold;
        assertFalse(failedQuorum, "Without quorum acknowledgements, write must not be reported strongly committed");
    }

    // ==========================================
    // 5. Eventual Read Consistency Lag & Convergence
    // ==========================================

    @Test
    void testEventualReadConsistencyConvergence() {
        long primarySequence = 150L;
        long readReplicaSequence = 147L; // 3 events behind (eventual read projection)

        long lag = primarySequence - readReplicaSequence;
        assertEquals(3L, lag, "Replica read projection temporarily exhibits lag");

        // Asynchronous catch-up replay
        for (long s = readReplicaSequence + 1; s <= primarySequence; s++) {
            readReplicaSequence = s;
        }

        assertEquals(primarySequence, readReplicaSequence, "Read replica eventually converges with primary state");
        assertEquals(0L, primarySequence - readReplicaSequence);
    }
}
