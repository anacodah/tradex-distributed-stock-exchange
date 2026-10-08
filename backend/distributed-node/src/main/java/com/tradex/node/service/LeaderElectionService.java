package com.tradex.node.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class LeaderElectionService {

    @Value("${node.id:1}")
    private int nodeId;

    @Value("${node.name:node1}")
    private String nodeName;

    private int priority;
    private String currentLeader = "node3"; // Default initial highest node
    private boolean isLeader = false;

    private final RestTemplate restTemplate;
    private final LamportClockService lamportClockService;

    private final List<Map<String, Object>> electionHistory = new ArrayList<>();

    public LeaderElectionService(RestTemplate restTemplate, LamportClockService lamportClockService) {
        this.restTemplate = restTemplate;
        this.lamportClockService = lamportClockService;
    }

    public synchronized void init() {
        this.priority = nodeId; // Priority 1, 2, 3
        this.isLeader = nodeName.equals(currentLeader);
    }

    public synchronized int getPriority() {
        if (priority == 0) priority = nodeId;
        return priority;
    }

    public synchronized String getCurrentLeader() {
        return currentLeader;
    }

    public synchronized boolean isLeader() {
        return isLeader;
    }

    public synchronized void setLeader(String leader) {
        this.currentLeader = leader;
        this.isLeader = nodeName.equals(leader);
    }

    // ==========================================
    // BULLY ALGORITHM
    // ==========================================
    public Map<String, Object> startBullyElection() {
        init();
        List<String> messages = new ArrayList<>();
        String oldLeader = currentLeader;
        messages.add(nodeName + " initiated Bully election for new Primary Matching Engine (Priority " + priority + ")");

        String[] allNodes = {"node1", "node2", "node3"};
        boolean higherNodeResponded = false;

        // Send ELECTION message to nodes with higher priority
        for (String peer : allNodes) {
            int peerPriority = Integer.parseInt(peer.replace("node", ""));
            if (peerPriority > this.priority) {
                messages.add(nodeName + " -> " + peer + " : ELECTION");
                try {
                    ResponseEntity<Map> response = restTemplate.postForEntity("http://" + peer + ":8080/api/election/bully/receive-election", 
                            Map.of("initiator", nodeName, "priority", priority), Map.class);
                    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                        Boolean ok = (Boolean) response.getBody().get("ok");
                        if (Boolean.TRUE.equals(ok)) {
                            higherNodeResponded = true;
                            messages.add(peer + " -> " + nodeName + " : OK");
                        }
                    }
                } catch (Exception e) {
                    messages.add(peer + " is UNREACHABLE/OFFLINE");
                }
            }
        }

        String newLeader;
        if (!higherNodeResponded) {
            // No higher priority node responded, this node becomes leader!
            newLeader = nodeName;
            setLeader(newLeader);
            messages.add(nodeName + " : BECOME PRIMARY ENGINE (Highest active priority)");
            announceCoordinatorBully(newLeader, messages);
        } else {
            // Wait for higher node to announce or query current leader
            newLeader = currentLeader;
        }

        lamportClockService.logEvent("ELECTION", nodeName, "ALL", null, 
                "Bully Election completed. Old Engine: " + oldLeader + ", New Primary Engine: " + newLeader);

        Map<String, Object> result = new HashMap<>();
        result.put("electionId", UUID.randomUUID().toString());
        result.put("algorithm", "BULLY");
        result.put("initiator", nodeName);
        result.put("messages", messages);
        result.put("oldLeader", oldLeader);
        result.put("newLeader", newLeader);
        result.put("timestamp", System.currentTimeMillis());
        result.put("status", "COMPLETED");

        electionHistory.add(0, result);
        return result;
    }

    private void announceCoordinatorBully(String leader, List<String> messages) {
        String[] allNodes = {"node1", "node2", "node3"};
        for (String peer : allNodes) {
            if (!peer.equals(nodeName)) {
                try {
                    messages.add(nodeName + " -> " + peer + " : COORDINATOR (" + leader + ")");
                    restTemplate.postForEntity("http://" + peer + ":8080/api/election/bully/coordinator", 
                            Map.of("leader", leader), Map.class);
                } catch (Exception ignored) {}
            }
        }
    }

    // ==========================================
    // RING ALGORITHM
    // ==========================================
    public Map<String, Object> startRingElection() {
        init();
        List<String> messages = new ArrayList<>();
        List<String> ringPath = new ArrayList<>();
        List<Integer> candidates = new ArrayList<>();
        
        candidates.add(priority);
        ringPath.add(nodeName);
        messages.add(nodeName + " initiated Ring election for new Primary Matching Engine with priority " + priority);

        String oldLeader = currentLeader;
        String nextNode = getNextRingNode(nodeName);

        // Pass election token around the ring
        boolean completed = passRingMessage(nextNode, candidates, ringPath, messages, nodeName);

        int maxPriority = Collections.max(candidates);
        String newLeader = "node" + maxPriority;
        setLeader(newLeader);

        // Announce leader around the ring
        announceRingCoordinator(getNextRingNode(nodeName), newLeader, messages, nodeName);

        lamportClockService.logEvent("ELECTION", nodeName, "ALL", null, 
                "Ring Election completed. Path: " + ringPath + ", New Primary Engine: " + newLeader);

        Map<String, Object> result = new HashMap<>();
        result.put("electionId", UUID.randomUUID().toString());
        result.put("algorithm", "RING");
        result.put("initiator", nodeName);
        result.put("ringPath", ringPath);
        result.put("candidates", candidates);
        result.put("messages", messages);
        result.put("oldLeader", oldLeader);
        result.put("newLeader", newLeader);
        result.put("timestamp", System.currentTimeMillis());
        result.put("status", "COMPLETED");

        electionHistory.add(0, result);
        return result;
    }

    private boolean passRingMessage(String targetNode, List<Integer> candidates, List<String> ringPath, List<String> messages, String originNode) {
        if (targetNode.equals(originNode)) {
            messages.add("Ring election token completed full circuit back to " + originNode);
            return true;
        }

        try {
            messages.add(ringPath.get(ringPath.size() - 1) + " -> " + targetNode + " : ELECTION_TOKEN " + candidates);
            Map<String, Object> req = new HashMap<>();
            req.put("originNode", originNode);
            req.put("candidates", candidates);
            req.put("ringPath", ringPath);

            ResponseEntity<Map> response = restTemplate.postForEntity("http://" + targetNode + ":8080/api/election/ring/pass", req, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Integer> updatedCandidates = (List<Integer>) response.getBody().get("candidates");
                List<String> updatedPath = (List<String>) response.getBody().get("ringPath");
                if (updatedCandidates != null) candidates.clear(); candidates.addAll(updatedCandidates);
                if (updatedPath != null) ringPath.clear(); ringPath.addAll(updatedPath);
                return true;
            }
        } catch (Exception e) {
            messages.add(targetNode + " is UNREACHABLE. Skipping to next ring node.");
            String bypassNode = getNextRingNode(targetNode);
            return passRingMessage(bypassNode, candidates, ringPath, messages, originNode);
        }
        return false;
    }

    private void announceRingCoordinator(String targetNode, String newLeader, List<String> messages, String originNode) {
        if (targetNode.equals(originNode)) return;

        try {
            messages.add(originNode + " -> " + targetNode + " : COORDINATOR " + newLeader);
            restTemplate.postForEntity("http://" + targetNode + ":8080/api/election/ring/coordinator", Map.of("leader", newLeader), Map.class);
            announceRingCoordinator(getNextRingNode(targetNode), newLeader, messages, originNode);
        } catch (Exception e) {
            // Bypass unreachable
            announceRingCoordinator(getNextRingNode(targetNode), newLeader, messages, originNode);
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

    public List<Map<String, Object>> getElectionHistory() {
        return electionHistory;
    }
}
