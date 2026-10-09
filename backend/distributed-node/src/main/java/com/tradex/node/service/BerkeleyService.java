package com.tradex.node.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class BerkeleyService {

    @Value("${node.name:node1}")
    private String nodeName;

    // Simulated local physical clock offset in milliseconds (randomized per node at start or configurable)
    private double clockOffsetMs;
    // Simulated clock drift in milliseconds per second (e.g. +1.5 ms/s)
    private double driftRateMsPerSec = 0.0;
    private long lastDriftUpdateTime = System.currentTimeMillis();

    private final RestTemplate restTemplate;
    private final LamportClockService lamportClockService;

    private final List<Map<String, Object>> syncHistory = new ArrayList<>();

    public BerkeleyService(RestTemplate restTemplate, LamportClockService lamportClockService) {
        this.restTemplate = restTemplate;
        this.lamportClockService = lamportClockService;
        // Seed default offset per node
        this.clockOffsetMs = (Math.random() * 200) - 100; // e.g. -100ms to +100ms
        this.driftRateMsPerSec = (Math.random() * 2.0) - 1.0; // small drift
    }

    public synchronized double getClockOffsetMs() {
        applyElapsedDrift();
        return clockOffsetMs;
    }

    public synchronized void setClockOffsetMs(double offset) {
        this.clockOffsetMs = offset;
        this.lastDriftUpdateTime = System.currentTimeMillis();
    }

    public synchronized double getDriftRateMsPerSec() {
        return driftRateMsPerSec;
    }

    public synchronized void setDriftRateMsPerSec(double driftRate) {
        applyElapsedDrift();
        this.driftRateMsPerSec = driftRate;
    }

    /**
     * Simulated physical clock reading: System.currentTimeMillis() + offset + accumulated drift.
     * Host OS time is strictly untouched.
     */
    public synchronized long getSimulatedPhysicalTimeMs() {
        applyElapsedDrift();
        return System.currentTimeMillis() + (long) clockOffsetMs;
    }

    private void applyElapsedDrift() {
        long now = System.currentTimeMillis();
        long elapsedSec = (now - lastDriftUpdateTime) / 1000;
        if (elapsedSec > 0 && driftRateMsPerSec != 0.0) {
            this.clockOffsetMs += (elapsedSec * driftRateMsPerSec);
            this.lastDriftUpdateTime = now;
        }
    }

    public Map<String, Object> synchronizeClocks() {
        String[] peers = {"node1", "node2", "node3"};
        Map<String, Double> nodeOffsets = new HashMap<>();
        
        // 1. Collect offsets from all active peers
        for (String peer : peers) {
            if (peer.equals(nodeName)) {
                nodeOffsets.put(peer, this.clockOffsetMs);
            } else {
                try {
                    ResponseEntity<Map> response = restTemplate.getForEntity("http://" + peer + ":8080/api/clock/berkeley/status", Map.class);
                    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                        Number offset = (Number) response.getBody().get("clockOffsetMs");
                        if (offset != null) {
                            nodeOffsets.put(peer, offset.doubleValue());
                        }
                    }
                } catch (Exception e) {
                    // Node is offline, skip
                }
            }
        }

        // 2. Calculate average offset
        double sum = 0;
        for (double off : nodeOffsets.values()) {
            sum += off;
        }
        double avgOffset = nodeOffsets.isEmpty() ? 0 : sum / nodeOffsets.size();

        // 3. Calculate adjustments and apply them
        Map<String, Double> adjustments = new HashMap<>();
        Map<String, Double> finalOffsets = new HashMap<>();

        for (Map.Entry<String, Double> entry : nodeOffsets.entrySet()) {
            String pNode = entry.getKey();
            double origOffset = entry.getValue();
            double adjustment = avgOffset - origOffset;
            double finalOffset = origOffset + adjustment;

            adjustments.put(pNode, adjustment);
            finalOffsets.put(pNode, finalOffset);

            // Send adjustment to target node
            if (pNode.equals(nodeName)) {
                this.clockOffsetMs = finalOffset;
            } else {
                try {
                    Map<String, Object> req = new HashMap<>();
                    req.put("adjustmentMs", adjustment);
                    restTemplate.postForEntity("http://" + pNode + ":8080/api/clock/berkeley/adjust", req, Map.class);
                } catch (Exception ignored) {}
            }
        }

        lamportClockService.logEvent("CLOCK_SYNC", nodeName, "ALL", null, 
                "Berkeley clock sync executed by coordinator " + nodeName + ". Avg offset: " + String.format("%.2f", avgOffset) + "ms");

        Map<String, Object> result = new HashMap<>();
        result.put("coordinator", nodeName);
        result.put("participatingNodes", new ArrayList<>(nodeOffsets.keySet()));
        result.put("originalOffsets", nodeOffsets);
        result.put("averageOffsetMs", avgOffset);
        result.put("adjustmentsMs", adjustments);
        result.put("finalOffsets", finalOffsets);
        result.put("timestamp", System.currentTimeMillis());

        syncHistory.add(0, result);
        return result;
    }

    public synchronized void applyAdjustment(double adjustmentMs) {
        this.clockOffsetMs += adjustmentMs;
    }

    public List<Map<String, Object>> getSyncHistory() {
        return syncHistory;
    }
}
