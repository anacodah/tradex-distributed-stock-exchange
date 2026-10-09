package com.tradex.common.entity;

import jakarta.persistence.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "order_events")
public class OrderEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "leader_epoch")
    private Long leaderEpoch;

    @Column(name = "node_id", length = 32)
    private String nodeId;

    @Column(name = "lamport_timestamp")
    private Long lamportTimestamp;

    @Column(name = "vector_clock", length = 128)
    private String vectorClock;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt = ZonedDateTime.now();

    public OrderEvent() {}

    public OrderEvent(Long orderId, String eventType, String payload, String nodeId, Long lamportTimestamp) {
        this.orderId = orderId;
        this.eventType = eventType;
        this.payload = payload;
        this.nodeId = nodeId;
        this.lamportTimestamp = lamportTimestamp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public Long getLeaderEpoch() { return leaderEpoch; }
    public void setLeaderEpoch(Long leaderEpoch) { this.leaderEpoch = leaderEpoch; }

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public Long getLamportTimestamp() { return lamportTimestamp; }
    public void setLamportTimestamp(Long lamportTimestamp) { this.lamportTimestamp = lamportTimestamp; }

    public String getVectorClock() { return vectorClock; }
    public void setVectorClock(String vectorClock) { this.vectorClock = vectorClock; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }
}
