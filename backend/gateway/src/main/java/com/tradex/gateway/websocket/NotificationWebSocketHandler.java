package com.tradex.gateway.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradex.common.entity.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationWebSocketHandler.class);

    // Map username -> set of WebSocket sessions
    private final Map<String, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String username = extractUsername(session);
        if (username != null) {
            userSessions.computeIfAbsent(username, k -> new CopyOnWriteArraySet<>()).add(session);
            log.info("WebSocket notifications connected for user: {}", username);
        } else {
            // Anonymous connection fallback (or global listener)
            userSessions.computeIfAbsent("ANONYMOUS", k -> new CopyOnWriteArraySet<>()).add(session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String username = extractUsername(session);
        if (username != null) {
            Set<WebSocketSession> sessions = userSessions.get(username);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    userSessions.remove(username);
                }
            }
        } else {
            Set<WebSocketSession> anon = userSessions.get("ANONYMOUS");
            if (anon != null) anon.remove(session);
        }
    }

    public void pushNotification(String username, Notification notification) {
        if (username == null || notification == null) return;
        Set<WebSocketSession> sessions = userSessions.get(username);
        if (sessions == null || sessions.isEmpty()) return;

        try {
            String json = objectMapper.writeValueAsString(notification);
            TextMessage message = new TextMessage(json);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(message);
                    } catch (IOException e) {
                        log.warn("Failed to send WebSocket notification to {}: {}", username, e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error serializing notification: {}", e.getMessage());
        }
    }

    private String extractUsername(WebSocketSession session) {
        if (session.getPrincipal() != null) {
            return session.getPrincipal().getName();
        }
        URI uri = session.getUri();
        if (uri != null && uri.getQuery() != null) {
            for (String param : uri.getQuery().split("&")) {
                String[] pair = param.split("=");
                if (pair.length == 2 && "user".equalsIgnoreCase(pair[0])) {
                    return pair[1];
                }
            }
        }
        return null;
    }
}
