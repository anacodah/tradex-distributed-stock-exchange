package com.tradex.common.entity;

import jakarta.persistence.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "replication_log", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"leader_epoch", "sequence_number"})
})
public class ReplicationLogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "leader_node", nullable = false, length = 50)
    private String leaderNode;

    @Column(name = "leader_epoch", nullable = false)
    private Long leaderEpoch;

    @Column(name = "sequence_number", nullable = false)
    private Long sequenceNumber;

    @Column(name = "entry_type", nullable = false, length = 50)
    private String entryType;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "term", nullable = false)
    private Long term;

    @Column(name = "acknowledged_nodes", columnDefinition = "TEXT")
    private String acknowledgedNodes;

    @Column(name = "committed")
    private Boolean committed = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt = ZonedDateTime.now();

    public ReplicationLogEntry() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLeaderNode() { return leaderNode; }
    public void setLeaderNode(String leaderNode) { this.leaderNode = leaderNode; }

    public Long getLeaderEpoch() { return leaderEpoch; }
    public void setLeaderEpoch(Long leaderEpoch) { this.leaderEpoch = leaderEpoch; }

    public Long getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(Long sequenceNumber) { this.sequenceNumber = sequenceNumber; }

    public String getEntryType() { return entryType; }
    public void setEntryType(String entryType) { this.entryType = entryType; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public Long getTerm() { return term; }
    public void setTerm(Long term) { this.term = term; }

    public String getAcknowledgedNodes() { return acknowledgedNodes; }
    public void setAcknowledgedNodes(String acknowledgedNodes) { this.acknowledgedNodes = acknowledgedNodes; }

    public Boolean getCommitted() { return committed; }
    public void setCommitted(Boolean committed) { this.committed = committed; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }
}
