package com.tradex.common.rmi.dto;

import java.io.Serializable;

public class ReplicationEventDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String eventId;
    private String eventType; // ORDER_CREATED, ORDER_FILLED, TRADE_EXECUTED, WALLET_UPDATED
    private long sourceNodeId;
    private long termEpoch;
    private long sequenceNumber;
    private String payloadJson;
    private long lamportTimestamp;
    private long timestamp;

    public ReplicationEventDto() {}

    public ReplicationEventDto(String eventId, String eventType, long sourceNodeId, long termEpoch,
                               long sequenceNumber, String payloadJson, long lamportTimestamp, long timestamp) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.sourceNodeId = sourceNodeId;
        this.termEpoch = termEpoch;
        this.sequenceNumber = sequenceNumber;
        this.payloadJson = payloadJson;
        this.lamportTimestamp = lamportTimestamp;
        this.timestamp = timestamp;
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public long getSourceNodeId() { return sourceNodeId; }
    public void setSourceNodeId(long sourceNodeId) { this.sourceNodeId = sourceNodeId; }

    public long getTermEpoch() { return termEpoch; }
    public void setTermEpoch(long termEpoch) { this.termEpoch = termEpoch; }

    public long getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(long sequenceNumber) { this.sequenceNumber = sequenceNumber; }

    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }

    public long getLamportTimestamp() { return lamportTimestamp; }
    public void setLamportTimestamp(long lamportTimestamp) { this.lamportTimestamp = lamportTimestamp; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
