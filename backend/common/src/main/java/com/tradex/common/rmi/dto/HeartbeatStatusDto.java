package com.tradex.common.rmi.dto;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class HeartbeatStatusDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String nodeName;
    private boolean reachable;
    private long lastHeartbeatTimeMs;
    private int missedHeartbeatCount;
    private String healthStatus; // HEALTHY, SUSPECTED, CONFIRMED_UNAVAILABLE
    private long latencyMs;

    public HeartbeatStatusDto() {}

    public HeartbeatStatusDto(String nodeName, boolean reachable, long lastHeartbeatTimeMs,
                              int missedHeartbeatCount, String healthStatus, long latencyMs) {
        this.nodeName = nodeName;
        this.reachable = reachable;
        this.lastHeartbeatTimeMs = lastHeartbeatTimeMs;
        this.missedHeartbeatCount = missedHeartbeatCount;
        this.healthStatus = healthStatus;
        this.latencyMs = latencyMs;
    }

    public String getNodeName() { return nodeName; }
    public void setNodeName(String nodeName) { this.nodeName = nodeName; }

    public boolean isReachable() { return reachable; }
    public void setReachable(boolean reachable) { this.reachable = reachable; }

    public long getLastHeartbeatTimeMs() { return lastHeartbeatTimeMs; }
    public void setLastHeartbeatTimeMs(long lastHeartbeatTimeMs) { this.lastHeartbeatTimeMs = lastHeartbeatTimeMs; }

    public int getMissedHeartbeatCount() { return missedHeartbeatCount; }
    public void setMissedHeartbeatCount(int missedHeartbeatCount) { this.missedHeartbeatCount = missedHeartbeatCount; }

    public String getHealthStatus() { return healthStatus; }
    public void setHealthStatus(String healthStatus) { this.healthStatus = healthStatus; }

    public long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }
}
