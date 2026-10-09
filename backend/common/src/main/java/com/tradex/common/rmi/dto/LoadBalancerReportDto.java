package com.tradex.common.rmi.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoadBalancerReportDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String currentAlgorithm; // ROUND_ROBIN, LEAST_CONNECTIONS
    private String authoritativeLeader;
    private long leaderEpoch;
    private Map<String, NodeLoadStatsDto> nodeStats = new HashMap<>();
    private List<Map<String, Object>> recentRoutingDecisions = new ArrayList<>();
    private long totalDispatchedRequests;
    private long timestamp;

    public LoadBalancerReportDto() {}

    public String getCurrentAlgorithm() { return currentAlgorithm; }
    public void setCurrentAlgorithm(String currentAlgorithm) { this.currentAlgorithm = currentAlgorithm; }

    public String getAuthoritativeLeader() { return authoritativeLeader; }
    public void setAuthoritativeLeader(String authoritativeLeader) { this.authoritativeLeader = authoritativeLeader; }

    public long getLeaderEpoch() { return leaderEpoch; }
    public void setLeaderEpoch(long leaderEpoch) { this.leaderEpoch = leaderEpoch; }

    public Map<String, NodeLoadStatsDto> getNodeStats() { return nodeStats; }
    public void setNodeStats(Map<String, NodeLoadStatsDto> nodeStats) { this.nodeStats = nodeStats; }

    public List<Map<String, Object>> getRecentRoutingDecisions() { return recentRoutingDecisions; }
    public void setRecentRoutingDecisions(List<Map<String, Object>> recentRoutingDecisions) { this.recentRoutingDecisions = recentRoutingDecisions; }

    public long getTotalDispatchedRequests() { return totalDispatchedRequests; }
    public void setTotalDispatchedRequests(long totalDispatchedRequests) { this.totalDispatchedRequests = totalDispatchedRequests; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
