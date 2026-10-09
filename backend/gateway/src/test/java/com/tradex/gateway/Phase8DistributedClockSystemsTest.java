package com.tradex.gateway;

import com.tradex.common.model.VectorClock;
import com.tradex.common.model.VectorClock.CausalOrder;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class Phase8DistributedClockSystemsTest {

    // ==========================================
    // 1. Lamport Logical Clocks: Standard Rules
    // ==========================================

    @Test
    void testLamportReceiveRule() {
        // Standard rule: clock on receive = max(local, received) + 1
        long localClock = 5;
        long receivedClock = 12;

        long newClock = Math.max(localClock, receivedClock) + 1;
        assertEquals(13, newClock, "Lamport receive rule must advance clock to max(local, received) + 1");

        long receivedLower = 3;
        long newClockFromLower = Math.max(localClock, receivedLower) + 1;
        assertEquals(6, newClockFromLower, "Lamport receive rule must advance past local clock even when received is lower");
    }

    @Test
    void testLamportDeterministicTieBreaker() {
        // Tie-breaker rule for total order: (L, NodeId)
        record LamportEvent(long timestamp, String nodeId, String desc) implements Comparable<LamportEvent> {
            @Override
            public int compareTo(LamportEvent other) {
                int cmp = Long.compare(this.timestamp, other.timestamp);
                if (cmp != 0) return cmp;
                return this.nodeId.compareTo(other.nodeId);
            }
        }

        LamportEvent e1 = new LamportEvent(10, "node1", "Order Placed");
        LamportEvent e2 = new LamportEvent(10, "node2", "Fund Reserved");
        LamportEvent e3 = new LamportEvent(11, "node1", "Order Filled");

        List<LamportEvent> list = new ArrayList<>(List.of(e3, e2, e1));
        Collections.sort(list);

        assertEquals("node1", list.get(0).nodeId());
        assertEquals("node2", list.get(1).nodeId());
        assertEquals(11, list.get(2).timestamp());
    }

    // ==========================================
    // 2. Vector Clocks: Causality & Concurrency
    // ==========================================

    @Test
    void testVectorClockEquality() {
        VectorClock vc1 = new VectorClock(Map.of("node1", 2L, "node2", 3L, "node3", 1L));
        VectorClock vc2 = new VectorClock(Map.of("node1", 2L, "node2", 3L, "node3", 1L));

        assertEquals(CausalOrder.EQUAL, vc1.compareCausality(vc2));
        assertEquals(vc1, vc2);
    }

    @Test
    void testVectorClockCausalPrecedence() {
        // e1: node1 increments: [1, 0, 0]
        VectorClock e1 = new VectorClock(Map.of("node1", 1L, "node2", 0L, "node3", 0L));

        // e2: node1 sends to node2: node2 updates [1, 1, 0]
        VectorClock e2 = new VectorClock(Map.of("node1", 1L, "node2", 1L, "node3", 0L));

        // e1 causally preceded e2 (e1 -> e2)
        assertTrue(e1.happenedBefore(e2), "e1 must happen before e2");
        assertFalse(e2.happenedBefore(e1));
        assertEquals(CausalOrder.BEFORE, e1.compareCausality(e2));
        assertEquals(CausalOrder.AFTER, e2.compareCausality(e1));
    }

    @Test
    void testVectorClockConcurrentEvents() {
        // node1 does local event: [2, 0, 0]
        VectorClock vc1 = new VectorClock(Map.of("node1", 2L, "node2", 0L, "node3", 0L));

        // node2 independently does local event: [0, 2, 0]
        VectorClock vc2 = new VectorClock(Map.of("node1", 0L, "node2", 2L, "node3", 0L));

        // Neither dominates: vc1 || vc2
        assertTrue(vc1.isConcurrentWith(vc2), "Independent events without communication must be concurrent");
        assertTrue(vc2.isConcurrentWith(vc1));
        assertEquals(CausalOrder.CONCURRENT, vc1.compareCausality(vc2));
        assertFalse(vc1.happenedBefore(vc2));
        assertFalse(vc2.happenedBefore(vc1));
    }

    @Test
    void testVectorClockMergeAfterConcurrency() {
        VectorClock vc1 = new VectorClock(Map.of("node1", 3L, "node2", 1L, "node3", 0L));
        VectorClock vc2 = new VectorClock(Map.of("node1", 1L, "node2", 4L, "node3", 0L));

        assertTrue(vc1.isConcurrentWith(vc2));

        // Node 3 receives both and merges
        VectorClock merged = new VectorClock();
        merged.merge(vc1);
        merged.merge(vc2);
        merged.increment("node3");

        // Component-wise maximum: max(3, 1)=3, max(1, 4)=4, 1 for node3
        assertEquals(3L, merged.getClock("node1"));
        assertEquals(4L, merged.getClock("node2"));
        assertEquals(1L, merged.getClock("node3"));

        // Now merged happens AFTER both vc1 and vc2
        assertTrue(vc1.happenedBefore(merged));
        assertTrue(vc2.happenedBefore(merged));
    }

    // ==========================================
    // 3. Physical Clocks & Berkeley Synchronization
    // ==========================================

    @Test
    void testBerkeleyOffsetAverageAndAdjustmentCalculation() {
        // Cluster of 3 nodes with offsets relative to true time
        Map<String, Double> originalOffsets = new LinkedHashMap<>();
        originalOffsets.put("node1", -60.0);
        originalOffsets.put("node2", 30.0);
        originalOffsets.put("node3", 0.0);

        // Average offset calculation
        double sum = originalOffsets.values().stream().mapToDouble(Double::doubleValue).sum();
        double avgOffset = sum / originalOffsets.size(); // (-60 + 30 + 0) / 3 = -10.0 ms
        assertEquals(-10.0, avgOffset, 0.001);

        Map<String, Double> adjustments = new HashMap<>();
        Map<String, Double> finalOffsets = new HashMap<>();

        for (Map.Entry<String, Double> e : originalOffsets.entrySet()) {
            double adj = avgOffset - e.getValue();
            adjustments.put(e.getKey(), adj);
            finalOffsets.put(e.getKey(), e.getValue() + adj);
        }

        // node1 adjustment: -10 - (-60) = +50ms
        assertEquals(50.0, adjustments.get("node1"), 0.001);
        // node2 adjustment: -10 - 30 = -40ms
        assertEquals(-40.0, adjustments.get("node2"), 0.001);
        // node3 adjustment: -10 - 0 = -10ms
        assertEquals(-10.0, adjustments.get("node3"), 0.001);

        // All nodes converge to average (-10.0 ms)
        for (double finalOffset : finalOffsets.values()) {
            assertEquals(-10.0, finalOffset, 0.001, "All nodes must converge to the same average offset");
        }
    }

    @Test
    void testBerkeleyGracefulHandlingOfOfflineNode() {
        Map<String, Double> activeOffsets = new HashMap<>();
        activeOffsets.put("node1", 20.0);
        activeOffsets.put("node2", 40.0);
        // node3 is offline

        double avg = activeOffsets.values().stream().mapToDouble(Double::doubleValue).sum() / activeOffsets.size();
        assertEquals(30.0, avg, 0.001);

        double node1Adj = avg - activeOffsets.get("node1");
        double node2Adj = avg - activeOffsets.get("node2");
        assertEquals(10.0, node1Adj, 0.001);
        assertEquals(-10.0, node2Adj, 0.001);
    }
}
