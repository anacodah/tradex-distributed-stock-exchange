package com.tradex.common.rmi.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class NodeStatusDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private int nodeId;
    private String nodeName;
    private String host;
    private int rmiPort;
    private boolean leader;
    private String currentLeader;
    private long termEpoch;
    private long lamportTimestamp;
    private List<String> capabilities;
    private Map<String, Object> workerPoolMetrics;

    public NodeStatusDto() {}

    public NodeStatusDto(int nodeId, String nodeName, String host, int rmiPort, boolean leader,
                         String currentLeader, long termEpoch, long lamportTimestamp,
                         List<String> capabilities, Map<String, Object> workerPoolMetrics) {
        this.nodeId = nodeId;
        this.nodeName = nodeName;
        this.host = host;
        this.rmiPort = rmiPort;
        this.leader = leader;
        this.currentLeader = currentLeader;
        this.termEpoch = termEpoch;
        this.lamportTimestamp = lamportTimestamp;
        this.capabilities = capabilities;
        this.workerPoolMetrics = workerPoolMetrics;
    }

    public int getNodeId() { return nodeId; }
    public void setNodeId(int nodeId) { this.nodeId = nodeId; }

    public String getNodeName() { return nodeName; }
    public void setNodeName(String nodeName) { this.nodeName = nodeName; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getRmiPort() { return rmiPort; }
    public void setRmiPort(int rmiPort) { this.rmiPort = rmiPort; }

    public boolean isLeader() { return leader; }
    public void setLeader(boolean leader) { this.leader = leader; }

    public String getCurrentLeader() { return currentLeader; }
    public void setCurrentLeader(String currentLeader) { this.currentLeader = currentLeader; }

    public long getTermEpoch() { return termEpoch; }
    public void setTermEpoch(long termEpoch) { this.termEpoch = termEpoch; }

    public long getLamportTimestamp() { return lamportTimestamp; }
    public void setLamportTimestamp(long lamportTimestamp) { this.lamportTimestamp = lamportTimestamp; }

    public List<String> getCapabilities() { return capabilities; }
    public void setCapabilities(List<String> capabilities) { this.capabilities = capabilities; }

    public Map<String, Object> getWorkerPoolMetrics() { return workerPoolMetrics; }
    public void setWorkerPoolMetrics(Map<String, Object> workerPoolMetrics) { this.workerPoolMetrics = workerPoolMetrics; }

    @Override
    public String toString() {
        return "NodeStatusDto{" +
                "nodeId=" + nodeId +
                ", nodeName='" + nodeName + '\'' +
                ", leader=" + leader +
                ", currentLeader='" + currentLeader + '\'' +
                ", termEpoch=" + termEpoch +
                '}';
    }
}
