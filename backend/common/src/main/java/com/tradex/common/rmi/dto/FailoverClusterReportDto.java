package com.tradex.common.rmi.dto;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class FailoverClusterReportDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String activeLeader;
    private long leaderEpoch;
    private String tradingState; // AVAILABLE, DEGRADED, SUSPENDED
    private String failoverStatus; // STABLE, ELECTION_IN_PROGRESS, RECOVERING_STATE
    private Map<String, HeartbeatStatusDto> nodeHeartbeats = new HashMap<>();
    private long lastFailoverTimestamp;
    private String lastFailoverReason;
    private long timestamp;

    public FailoverClusterReportDto() {}

    public String getActiveLeader() { return activeLeader; }
    public void setActiveLeader(String activeLeader) { this.activeLeader = activeLeader; }

    public long getLeaderEpoch() { return leaderEpoch; }
    public void setLeaderEpoch(long leaderEpoch) { this.leaderEpoch = leaderEpoch; }

    public String getTradingState() { return tradingState; }
    public void setTradingState(String tradingState) { this.tradingState = tradingState; }

    public String getFailoverStatus() { return failoverStatus; }
    public void setFailoverStatus(String failoverStatus) { this.failoverStatus = failoverStatus; }

    public Map<String, HeartbeatStatusDto> getNodeHeartbeats() { return nodeHeartbeats; }
    public void setNodeHeartbeats(Map<String, HeartbeatStatusDto> nodeHeartbeats) { this.nodeHeartbeats = nodeHeartbeats; }

    public long getLastFailoverTimestamp() { return lastFailoverTimestamp; }
    public void setLastFailoverTimestamp(long lastFailoverTimestamp) { this.lastFailoverTimestamp = lastFailoverTimestamp; }

    public String getLastFailoverReason() { return lastFailoverReason; }
    public void setLastFailoverReason(String lastFailoverReason) { this.lastFailoverReason = lastFailoverReason; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
