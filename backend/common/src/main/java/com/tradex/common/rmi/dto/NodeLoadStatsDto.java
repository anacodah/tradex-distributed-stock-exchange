package com.tradex.common.rmi.dto;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class NodeLoadStatsDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String nodeName;
    private boolean healthy;
    private String role; // PRIMARY_LEADER, FOLLOWER_REPLICA
    private long totalRequests;
    private int activeRequests;
    private long completedRequests;
    private long failedRequests;
    private double avgLatencyMs;
    private double errorRatePercent;
    private long lastRequestTimestamp;

    public NodeLoadStatsDto() {}

    public NodeLoadStatsDto(String nodeName, boolean healthy, String role) {
        this.nodeName = nodeName;
        this.healthy = healthy;
        this.role = role;
    }

    public String getNodeName() { return nodeName; }
    public void setNodeName(String nodeName) { this.nodeName = nodeName; }

    public boolean isHealthy() { return healthy; }
    public void setHealthy(boolean healthy) { this.healthy = healthy; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public long getTotalRequests() { return totalRequests; }
    public void setTotalRequests(long totalRequests) { this.totalRequests = totalRequests; }

    public int getActiveRequests() { return activeRequests; }
    public void setActiveRequests(int activeRequests) { this.activeRequests = activeRequests; }

    public long getCompletedRequests() { return completedRequests; }
    public void setCompletedRequests(long completedRequests) { this.completedRequests = completedRequests; }

    public long getFailedRequests() { return failedRequests; }
    public void setFailedRequests(long failedRequests) { this.failedRequests = failedRequests; }

    public double getAvgLatencyMs() { return avgLatencyMs; }
    public void setAvgLatencyMs(double avgLatencyMs) { this.avgLatencyMs = avgLatencyMs; }

    public double getErrorRatePercent() { return errorRatePercent; }
    public void setErrorRatePercent(double errorRatePercent) { this.errorRatePercent = errorRatePercent; }

    public long getLastRequestTimestamp() { return lastRequestTimestamp; }
    public void setLastRequestTimestamp(long lastRequestTimestamp) { this.lastRequestTimestamp = lastRequestTimestamp; }
}
