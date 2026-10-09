package com.tradex.common.rmi.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ElectionExecutionDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String electionId;
    private String algorithm; // BULLY, RING
    private String initiator;
    private String oldLeader;
    private String newLeader;
    private long leaderEpoch;
    private List<String> participants = new ArrayList<>();
    private List<String> unavailableNodes = new ArrayList<>();
    private List<String> messages = new ArrayList<>();
    private List<String> ringPath = new ArrayList<>();
    private long startTimeMs;
    private long endTimeMs;
    private long durationMs;
    private String outcome; // SUCCESS, FAILED
    private String failureReason;
    private String tradingState; // AVAILABLE, DEGRADED, SUSPENDED

    public ElectionExecutionDto() {}

    public String getElectionId() { return electionId; }
    public void setElectionId(String electionId) { this.electionId = electionId; }

    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }

    public String getInitiator() { return initiator; }
    public void setInitiator(String initiator) { this.initiator = initiator; }

    public String getOldLeader() { return oldLeader; }
    public void setOldLeader(String oldLeader) { this.oldLeader = oldLeader; }

    public String getNewLeader() { return newLeader; }
    public void setNewLeader(String newLeader) { this.newLeader = newLeader; }

    public long getLeaderEpoch() { return leaderEpoch; }
    public void setLeaderEpoch(long leaderEpoch) { this.leaderEpoch = leaderEpoch; }

    public List<String> getParticipants() { return participants; }
    public void setParticipants(List<String> participants) { this.participants = participants; }

    public List<String> getUnavailableNodes() { return unavailableNodes; }
    public void setUnavailableNodes(List<String> unavailableNodes) { this.unavailableNodes = unavailableNodes; }

    public List<String> getMessages() { return messages; }
    public void setMessages(List<String> messages) { this.messages = messages; }

    public List<String> getRingPath() { return ringPath; }
    public void setRingPath(List<String> ringPath) { this.ringPath = ringPath; }

    public long getStartTimeMs() { return startTimeMs; }
    public void setStartTimeMs(long startTimeMs) { this.startTimeMs = startTimeMs; }

    public long getEndTimeMs() { return endTimeMs; }
    public void setEndTimeMs(long endTimeMs) { this.endTimeMs = endTimeMs; }

    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public String getTradingState() { return tradingState; }
    public void setTradingState(String tradingState) { this.tradingState = tradingState; }
}
