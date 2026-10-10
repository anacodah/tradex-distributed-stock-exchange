package com.tradex.node.service;

import com.tradex.common.rmi.dto.ElectionExecutionDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class LeaderElectionService {

    private static final Logger log = LoggerFactory.getLogger(LeaderElectionService.class);

    @Value("${node.id:1}")
    private int nodeId;

    @Value("${node.name:node1}")
    private String nodeName;

    private int priority;
    private volatile String currentLeader = "node3"; // Default initial highest node
    private volatile boolean isLeader = false;
    private final AtomicLong leaderEpoch = new AtomicLong(3); // Start at initial bootstrap epoch 3

    private final RestTemplate restTemplate;
    private final LamportClockService lamportClockService;

    private final List<ElectionExecutionDto> electionHistory = new CopyOnWriteArrayList<>();

    public LeaderElectionService(RestTemplate restTemplate, LamportClockService lamportClockService) {
        this.restTemplate = restTemplate;
        this.lamportClockService = lamportClockService;
    }

    @PostConstruct
    public synchronized void init() {
        this.priority = nodeId; // Priority 1, 2, 3
        this.isLeader = nodeName.equals(currentLeader);
        log.info("LeaderElectionService initialized for node {} (priority={}, currentLeader={}, isLeader={})",
                nodeName, priority, currentLeader, isLeader);
    }

    public synchronized int getPriority() {
        if (priority == 0) priority = nodeId;
        return priority;
    }

    public String getCurrentLeader() {
        return currentLeader;
    }

    public boolean isLeader() {
        return isLeader;
    }

    public long getLeaderEpoch() {
        return leaderEpoch.get();
    }

    public synchronized boolean updateLeader(String leader, long epoch) {
        if (epoch < this.leaderEpoch.get()) {
            log.warn("Ignored stale leader announcement from {} with epoch {} (current epoch is {})", leader, epoch, this.leaderEpoch.get());
            return false;
        }
        this.leaderEpoch.set(epoch);
        this.currentLeader = leader;
        this.isLeader = nodeName.equals(leader);
        log.info("Node {} updated authoritative leader to {} with epoch {}", nodeName, leader, epoch);
        return true;
    }

    public synchronized void setLeader(String leader) {
        this.currentLeader = leader;
        this.isLeader = nodeName.equals(leader);
    }

    // ==========================================
    // BULLY ALGORITHM
    // ==========================================
    public synchronized ElectionExecutionDto startBullyElection() {
        init();
        long startTime = System.currentTimeMillis();
        long newEpoch = leaderEpoch.incrementAndGet();

        ElectionExecutionDto report = new ElectionExecutionDto();
        report.setElectionId(UUID.randomUUID().toString());
        report.setAlgorithm("BULLY");
        report.setInitiator(nodeName);
        report.setOldLeader(currentLeader);
        report.setLeaderEpoch(newEpoch);
        report.setStartTimeMs(startTime);

        List<String> messages = new ArrayList<>();
        List<String> participants = new ArrayList<>();
        List<String> unavailableNodes = new ArrayList<>();

        participants.add(nodeName);
        messages.add(nodeName + " initiated Bully election for new Primary Matching Engine (Priority " + priority + ", Epoch #" + newEpoch + ")");

        String[] allNodes = {"node1", "node2", "node3"};
        boolean higherNodeResponded = false;

        // Send ELECTION message to nodes with higher priority
        for (String peer : allNodes) {
            int peerPriority = Integer.parseInt(peer.replace("node", ""));
            if (peerPriority > this.priority) {
                messages.add(nodeName + " -> " + peer + " : ELECTION (Epoch " + newEpoch + ")");
                try {
                    ResponseEntity<Map> response = restTemplate.postForEntity("http://" + peer + ":8080/api/election/bully/receive-election", 
                            Map.of("initiator", nodeName, "priority", priority, "epoch", newEpoch), Map.class);
                    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                        Boolean ok = (Boolean) response.getBody().get("ok");
                        if (Boolean.TRUE.equals(ok)) {
                            higherNodeResponded = true;
                            participants.add(peer);
                            messages.add(peer + " -> " + nodeName + " : OK (Higher priority active)");
                        }
                    }
                } catch (Exception e) {
                    unavailableNodes.add(peer);
                    messages.add(peer + " is UNREACHABLE / OFFLINE (Timeout)");
                }
            }
        }

        String newLeader;
        if (!higherNodeResponded) {
            // No higher priority node responded, this node becomes leader!
            newLeader = nodeName;
            updateLeader(newLeader, newEpoch);
            messages.add(nodeName + " : BECOME AUTHORITATIVE PRIMARY LEADER (Highest reachable priority)");
            announceCoordinatorBully(newLeader, newEpoch, messages, participants, unavailableNodes);
        } else {
            // Wait for higher node to announce
            newLeader = currentLeader;
        }

        lamportClockService.logEvent("ELECTION", nodeName, "ALL", null, 
                "Bully Election completed. Old Engine: " + report.getOldLeader() + ", New Primary Engine: " + newLeader + " (Epoch " + newEpoch + ")");

        long endTime = System.currentTimeMillis();
        report.setNewLeader(newLeader);
        report.setEndTimeMs(endTime);
        report.setDurationMs(endTime - startTime);
        report.setParticipants(participants);
        report.setUnavailableNodes(unavailableNodes);
        report.setMessages(messages);
        report.setOutcome("SUCCESS");
        report.setTradingState("AVAILABLE");

        electionHistory.add(0, report);
        return report;
    }

    private void announceCoordinatorBully(String leader, long epoch, List<String> messages, List<String> participants, List<String> unavailableNodes) {
        String[] allNodes = {"node1", "node2", "node3"};
        for (String peer : allNodes) {
            if (!peer.equals(nodeName)) {
                try {
                    messages.add(nodeName + " -> " + peer + " : COORDINATOR (" + leader + ", Epoch #" + epoch + ")");
                    restTemplate.postForEntity("http://" + peer + ":8080/api/election/bully/coordinator", 
                            Map.of("leader", leader, "epoch", epoch), Map.class);
                    if (!participants.contains(peer)) participants.add(peer);
                } catch (Exception e) {
                    if (!unavailableNodes.contains(peer)) unavailableNodes.add(peer);
                    messages.add(peer + " failed to receive COORDINATOR announcement (Unreachable)");
                }
            }
        }
    }

    // ==========================================
    // RING ALGORITHM
    // ==========================================
    public synchronized ElectionExecutionDto startRingElection() {
        init();
        long startTime = System.currentTimeMillis();
        long newEpoch = leaderEpoch.incrementAndGet();

        ElectionExecutionDto report = new ElectionExecutionDto();
        report.setElectionId(UUID.randomUUID().toString());
        report.setAlgorithm("RING");
        report.setInitiator(nodeName);
        report.setOldLeader(currentLeader);
        report.setLeaderEpoch(newEpoch);
        report.setStartTimeMs(startTime);

        List<String> messages = new ArrayList<>();
        List<String> ringPath = new ArrayList<>();
        List<String> unavailableNodes = new ArrayList<>();
        List<Integer> candidates = new ArrayList<>();
        
        candidates.add(priority);
        ringPath.add(nodeName);
        messages.add(nodeName + " initiated Ring election for new Primary Matching Engine with priority " + priority + " (Epoch #" + newEpoch + ")");

        String nextNode = getNextRingNode(nodeName);

        // Pass election token around the logical ring
        passRingMessage(nextNode, candidates, ringPath, unavailableNodes, messages, nodeName, newEpoch);

        int maxPriority = Collections.max(candidates);
        String newLeader = "node" + maxPriority;
        updateLeader(newLeader, newEpoch);

        // Announce leader coordinator around the ring
        announceRingCoordinator(getNextRingNode(nodeName), newLeader, newEpoch, messages, unavailableNodes, nodeName);

        lamportClockService.logEvent("ELECTION", nodeName, "ALL", null, 
                "Ring Election completed. Path: " + ringPath + ", New Primary Engine: " + newLeader + " (Epoch #" + newEpoch + ")");

        long endTime = System.currentTimeMillis();
        report.setNewLeader(newLeader);
        report.setEndTimeMs(endTime);
        report.setDurationMs(endTime - startTime);
        report.setRingPath(ringPath);
        report.setParticipants(new ArrayList<>(ringPath));
        report.setUnavailableNodes(unavailableNodes);
        report.setMessages(messages);
        report.setOutcome("SUCCESS");
        report.setTradingState("AVAILABLE");

        electionHistory.add(0, report);
        return report;
    }

    private boolean passRingMessage(String targetNode, List<Integer> candidates, List<String> ringPath, List<String> unavailableNodes,
                                    List<String> messages, String originNode, long epoch) {
        if (targetNode.equals(originNode)) {
            messages.add("Ring election token completed full circuit back to initiator " + originNode);
            return true;
        }

        try {
            messages.add(ringPath.get(ringPath.size() - 1) + " -> " + targetNode + " : ELECTION_TOKEN candidates=" + candidates + " (Epoch #" + epoch + ")");
            Map<String, Object> req = new HashMap<>();
            req.put("originNode", originNode);
            req.put("candidates", candidates);
            req.put("ringPath", ringPath);
            req.put("epoch", epoch);

            ResponseEntity<Map> response = restTemplate.postForEntity("http://" + targetNode + ":8080/api/election/ring/pass", req, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Integer> updatedCandidates = (List<Integer>) response.getBody().get("candidates");
                List<String> updatedPath = (List<String>) response.getBody().get("ringPath");
                if (updatedCandidates != null) {
                    candidates.clear();
                    candidates.addAll(updatedCandidates);
                }
                if (updatedPath != null) {
                    ringPath.clear();
                    ringPath.addAll(updatedPath);
                }
                return passRingMessage(getNextRingNode(targetNode), candidates, ringPath, unavailableNodes, messages, originNode, epoch);
            }
        } catch (Exception e) {
            unavailableNodes.add(targetNode);
            messages.add(targetNode + " is UNREACHABLE in Ring topology. Bypassing to next ring hop.");
            String bypassNode = getNextRingNode(targetNode);
            return passRingMessage(bypassNode, candidates, ringPath, unavailableNodes, messages, originNode, epoch);
        }
        return false;
    }

    private void announceRingCoordinator(String targetNode, String newLeader, long epoch, List<String> messages, List<String> unavailableNodes, String originNode) {
        if (targetNode.equals(originNode)) return;

        try {
            messages.add(originNode + " -> " + targetNode + " : COORDINATOR " + newLeader + " (Epoch #" + epoch + ")");
            restTemplate.postForEntity("http://" + targetNode + ":8080/api/election/ring/coordinator", 
                    Map.of("leader", newLeader, "epoch", epoch), Map.class);
            announceRingCoordinator(getNextRingNode(targetNode), newLeader, epoch, messages, unavailableNodes, originNode);
        } catch (Exception e) {
            if (!unavailableNodes.contains(targetNode)) unavailableNodes.add(targetNode);
            messages.add(targetNode + " failed to receive coordinator (Bypassing).");
            announceRingCoordinator(getNextRingNode(targetNode), newLeader, epoch, messages, unavailableNodes, originNode);
        }
    }

    private String getNextRingNode(String current) {
        switch (current) {
            case "node1": return "node2";
            case "node2": return "node3";
            case "node3": return "node1";
            default: return "node1";
        }
    }

    public List<ElectionExecutionDto> getElectionHistory() {
        return Collections.unmodifiableList(electionHistory);
    }
}
