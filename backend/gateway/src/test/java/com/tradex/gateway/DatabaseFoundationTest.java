package com.tradex.gateway;

import com.tradex.common.entity.*;
import com.tradex.common.model.VectorClock;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseFoundationTest {

    @Test
    void testVectorClockOperations() {
        VectorClock vc1 = new VectorClock();
        vc1.increment("node1");
        vc1.increment("node1");
        vc1.increment("node2");

        assertEquals(2L, vc1.getClock("node1"));
        assertEquals(1L, vc1.getClock("node2"));
        assertEquals(0L, vc1.getClock("node3"));

        VectorClock vc2 = new VectorClock();
        vc2.increment("node2");
        vc2.increment("node2");
        vc2.increment("node3");

        vc1.merge(vc2);
        assertEquals(2L, vc1.getClock("node1"));
        assertEquals(2L, vc1.getClock("node2"));
        assertEquals(1L, vc1.getClock("node3"));

        String serialized = vc1.serialize();
        VectorClock deserialized = VectorClock.deserialize(serialized);
        assertEquals(2L, deserialized.getClock("node1"));
        assertEquals(2L, deserialized.getClock("node2"));
        assertEquals(1L, deserialized.getClock("node3"));
    }

    @Test
    void testEntityInstantiations() {
        Company company = new Company("NVDA", "NVIDIA Corp", "Technology");
        assertEquals("NVDA", company.getSymbol());

        Instrument instrument = new Instrument("NVDA", "NVIDIA Corp");
        assertEquals("EQUITY", instrument.getType());

        UserProfile profile = new UserProfile(1L, "Test", "User");
        assertEquals(1L, profile.getUserId());

        IdempotencyRecord record = new IdempotencyRecord("key-123", "/api/orders", "{\"status\":\"EXECUTED\"}", 200);
        assertEquals("key-123", record.getIdempotencyKey());

        LeaderLease lease = new LeaderLease("node3", 1L, java.time.ZonedDateTime.now());
        assertEquals("node3", lease.getLeaderName());
    }
}
