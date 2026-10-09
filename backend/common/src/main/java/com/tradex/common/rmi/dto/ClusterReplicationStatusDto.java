package com.tradex.common.rmi.dto;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ClusterReplicationStatusDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String leaderNode;
    private long leaderEpoch;
    private long highestCommittedSequence;
    private String consistencyModel; // STRONGLY_COORDINATED vs EVENTUAL_READ_PROJECTION
    private int quorumThreshold;
    private Map<String, Long> replicaCommittedSequences = new HashMap<>();
    private Map<String, Long> replicaLags = new HashMap<>();
    private Map<String, String> replicaStates = new HashMap<>(); // SYNCHRONIZED, REPLAYING, LAGGING, DISCONNECTED
    private long totalEventsReplicated;
    private long timestamp;

    public ClusterReplicationStatusDto() {}

    public String getLeaderNode() { return leaderNode; }
    public void setLeaderNode(String leaderNode) { this.leaderNode = leaderNode; }

    public long getLeaderEpoch() { return leaderEpoch; }
    public void setLeaderEpoch(long leaderEpoch) { this.leaderEpoch = leaderEpoch; }

    public long getHighestCommittedSequence() { return highestCommittedSequence; }
    public void setHighestCommittedSequence(long highestCommittedSequence) { this.highestCommittedSequence = highestCommittedSequence; }

    public String getConsistencyModel() { return consistencyModel; }
    public void setConsistencyModel(String consistencyModel) { this.consistencyModel = consistencyModel; }

    public int getQuorumThreshold() { return quorumThreshold; }
    public void setQuorumThreshold(int quorumThreshold) { this.quorumThreshold = quorumThreshold; }

    public Map<String, Long> getReplicaCommittedSequences() { return replicaCommittedSequences; }
    public void setReplicaCommittedSequences(Map<String, Long> replicaCommittedSequences) { this.replicaCommittedSequences = replicaCommittedSequences; }

    public Map<String, Long> getReplicaLags() { return replicaLags; }
    public void setReplicaLags(Map<String, Long> replicaLags) { this.replicaLags = replicaLags; }

    public Map<String, String> getReplicaStates() { return replicaStates; }
    public void setReplicaStates(Map<String, String> replicaStates) { this.replicaStates = replicaStates; }

    public long getTotalEventsReplicated() { return totalEventsReplicated; }
    public void setTotalEventsReplicated(long totalEventsReplicated) { this.totalEventsReplicated = totalEventsReplicated; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
