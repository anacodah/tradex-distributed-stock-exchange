package com.tradex.gateway.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Real-time WebSocket handler for cluster events, leader changes, failovers, and order state transitions.
 * Endpoint: /ws/cluster
 */
@Component
public class ClusterEventsWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ClusterEventsWebSocketHandler.class);

    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("Client connected to cluster events WebSocket: session ID {}", session.getId());
        // Send initial connected ACK
        try {
            Map<String, Object> welcome = Map.of(
                    "type", "CONNECTED",
                    "sessionId", session.getId(),
                    "timestamp", System.currentTimeMillis()
            );
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(welcome)));
        } catch (IOException ignored) {}
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("Client disconnected from cluster events WebSocket: session ID {}", session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        sessions.remove(session);
        log.warn("Cluster events WebSocket transport error: {}", exception.getMessage());
    }

    /**
     * Broadcast an authoritative event to all connected frontend clients.
     */
    public void broadcastEvent(String eventType, Map<String, Object> payload) {
        if (sessions.isEmpty()) return;

        Map<String, Object> message = new HashMap<>();
        message.put("type", eventType);
        message.put("payload", payload);
        message.put("timestamp", System.currentTimeMillis());

        try {
            String json = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(json);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                    } catch (IOException e) {
                        log.warn("Failed sending cluster event to session {}: {}", session.getId(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed serializing cluster broadcast event: {}", e.getMessage());
        }
    }
}
