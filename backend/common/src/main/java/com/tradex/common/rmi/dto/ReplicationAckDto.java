package com.tradex.common.rmi.dto;

import java.io.Serializable;

public class ReplicationAckDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String eventId;
    private long sequenceNumber;
    private long termEpoch;
    private int replicaNodeId;
    private String replicaNodeName;
    private boolean applied;
    private long lastCommittedSequence;
    private String message;
    private long ackTimestamp;

    public ReplicationAckDto() {}

    public ReplicationAckDto(String eventId, long sequenceNumber, long termEpoch, int replicaNodeId,
                             String replicaNodeName, boolean applied, long lastCommittedSequence,
                             String message, long ackTimestamp) {
        this.eventId = eventId;
        this.sequenceNumber = sequenceNumber;
        this.termEpoch = termEpoch;
        this.replicaNodeId = replicaNodeId;
        this.replicaNodeName = replicaNodeName;
        this.applied = applied;
        this.lastCommittedSequence = lastCommittedSequence;
        this.message = message;
        this.ackTimestamp = ackTimestamp;
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public long getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(long sequenceNumber) { this.sequenceNumber = sequenceNumber; }

    public long getTermEpoch() { return termEpoch; }
    public void setTermEpoch(long termEpoch) { this.termEpoch = termEpoch; }

    public int getReplicaNodeId() { return replicaNodeId; }
    public void setReplicaNodeId(int replicaNodeId) { this.replicaNodeId = replicaNodeId; }

    public String getReplicaNodeName() { return replicaNodeName; }
    public void setReplicaNodeName(String replicaNodeName) { this.replicaNodeName = replicaNodeName; }

    public boolean isApplied() { return applied; }
    public void setApplied(boolean applied) { this.applied = applied; }

    public long getLastCommittedSequence() { return lastCommittedSequence; }
    public void setLastCommittedSequence(long lastCommittedSequence) { this.lastCommittedSequence = lastCommittedSequence; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getAckTimestamp() { return ackTimestamp; }
    public void setAckTimestamp(long ackTimestamp) { this.ackTimestamp = ackTimestamp; }
}
