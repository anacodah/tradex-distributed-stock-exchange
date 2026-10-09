package com.tradex.common.rmi.dto;

import java.io.Serializable;

public class ClockSyncDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private int sourceNodeId;
    private String sourceNodeName;
    private long lamportTimestamp;
    private long physicalTimestamp;
    private String correlationId;

    public ClockSyncDto() {}

    public ClockSyncDto(int sourceNodeId, String sourceNodeName, long lamportTimestamp,
                        long physicalTimestamp, String correlationId) {
        this.sourceNodeId = sourceNodeId;
        this.sourceNodeName = sourceNodeName;
        this.lamportTimestamp = lamportTimestamp;
        this.physicalTimestamp = physicalTimestamp;
        this.correlationId = correlationId;
    }

    public int getSourceNodeId() { return sourceNodeId; }
    public void setSourceNodeId(int sourceNodeId) { this.sourceNodeId = sourceNodeId; }

    public String getSourceNodeName() { return sourceNodeName; }
    public void setSourceNodeName(String sourceNodeName) { this.sourceNodeName = sourceNodeName; }

    public long getLamportTimestamp() { return lamportTimestamp; }
    public void setLamportTimestamp(long lamportTimestamp) { this.lamportTimestamp = lamportTimestamp; }

    public long getPhysicalTimestamp() { return physicalTimestamp; }
    public void setPhysicalTimestamp(long physicalTimestamp) { this.physicalTimestamp = physicalTimestamp; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
}
