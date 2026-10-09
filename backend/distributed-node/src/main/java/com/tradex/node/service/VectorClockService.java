package com.tradex.node.service;

import com.tradex.common.model.VectorClock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class VectorClockService {

    @Value("${node.name:node1}")
    private String nodeName;

    private final VectorClock vectorClock = new VectorClock();
    private final List<Map<String, Object>> vectorEventHistory = new CopyOnWriteArrayList<>();

    public synchronized VectorClock getVectorClock() {
        return new VectorClock(vectorClock.getClockMap());
    }

    /**
     * Local event increments this node's component.
     */
    public synchronized VectorClock recordLocalEvent(String eventDescription) {
        vectorClock.increment(nodeName);
        recordHistory("LOCAL", eventDescription, null, new VectorClock(vectorClock.getClockMap()));
        return new VectorClock(vectorClock.getClockMap());
    }

    /**
     * Prepare message to send, increments component.
     */
    public synchronized VectorClock prepareSendEvent(String targetNode, String messageType) {
        vectorClock.increment(nodeName);
        recordHistory("SEND", "Send " + messageType + " to " + targetNode, targetNode, new VectorClock(vectorClock.getClockMap()));
        return new VectorClock(vectorClock.getClockMap());
    }

    /**
     * Receive message with incoming vector clock:
     * - VectorClock = component-wise max(local, received)
     * - increment local component
     */
    public synchronized VectorClock recordReceiveEvent(String sourceNode, VectorClock incomingClock, String messageType) {
        vectorClock.merge(incomingClock);
        vectorClock.increment(nodeName);
        recordHistory("RECEIVE", "Receive " + messageType + " from " + sourceNode, sourceNode, new VectorClock(vectorClock.getClockMap()));
        return new VectorClock(vectorClock.getClockMap());
    }

    public List<Map<String, Object>> getVectorEventHistory() {
        return Collections.unmodifiableList(vectorEventHistory);
    }

    private void recordHistory(String type, String desc, String peer, VectorClock snapshot) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("node", nodeName);
        entry.put("type", type);
        entry.put("description", desc);
        entry.put("peer", peer);
        entry.put("vector", snapshot.getClockMap());
        entry.put("vectorString", snapshot.serialize());
        entry.put("timestamp", System.currentTimeMillis());

        vectorEventHistory.add(0, entry);
        if (vectorEventHistory.size() > 100) {
            vectorEventHistory.remove(vectorEventHistory.size() - 1);
        }
    }
}
