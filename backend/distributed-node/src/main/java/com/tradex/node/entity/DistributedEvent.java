package com.tradex.node.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "distributed_events")
public class DistributedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nodeId;
    private String eventType; // LOCAL_EVENT, MESSAGE_SENT, MESSAGE_RECEIVED, ELECTION, CLOCK_SYNC, etc.
    private long lamportTimestamp;

    private String sourceNode;
    @Column(name = "dest_node")
    private String destinationNode;
    private Long receivedTimestamp;

    private LocalDateTime wallClockTime;

    @Column(length = 1000)
    private String description;

    public DistributedEvent() {
        this.wallClockTime = LocalDateTime.now();
    }

    public DistributedEvent(String nodeId, String eventType, long lamportTimestamp, String sourceNode,
                            String destinationNode, Long receivedTimestamp, String description) {
        this.nodeId = nodeId;
        this.eventType = eventType;
        this.lamportTimestamp = lamportTimestamp;
        this.sourceNode = sourceNode;
        this.destinationNode = destinationNode;
        this.receivedTimestamp = receivedTimestamp;
        this.description = description;
        this.wallClockTime = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public long getLamportTimestamp() { return lamportTimestamp; }
    public void setLamportTimestamp(long lamportTimestamp) { this.lamportTimestamp = lamportTimestamp; }

    public String getSourceNode() { return sourceNode; }
    public void setSourceNode(String sourceNode) { this.sourceNode = sourceNode; }

    public String getDestinationNode() { return destinationNode; }
    public void setDestinationNode(String destinationNode) { this.destinationNode = destinationNode; }

    public Long getReceivedTimestamp() { return receivedTimestamp; }
    public void setReceivedTimestamp(Long receivedTimestamp) { this.receivedTimestamp = receivedTimestamp; }

    public LocalDateTime getWallClockTime() { return wallClockTime; }
    public void setWallClockTime(LocalDateTime wallClockTime) { this.wallClockTime = wallClockTime; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
