package com.tradex.gateway.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tradex.gateway.entity.Stock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * WebSocket handler for real-time market price broadcasts.
 *
 * Protocol:
 * - Client connects to ws://host/ws/market
 * - Server broadcasts JSON price updates every ~5 seconds (tied to engine tick)
 * - Clients can send {"action":"subscribe","symbols":["AAPL","MSFT"]} to filter
 * - Stale sessions (no pong within 60s) are detected and cleaned up
 *
 * Message format (server → client):
 * {
 *   "type": "PRICE_UPDATE",
 *   "dataSource": "SIMULATED",
 *   "timestamp": "2024-01-15T14:30:00Z",
 *   "updates": [
 *     { "symbol": "AAPL", "price": 189.30, "changeAmount": 0.80, "changePercent": 0.42,
 *       "dayHigh": 191.20, "dayLow": 187.80, "volume": 52341200, "priceTimestamp": "..." }
 *   ]
 * }
 */
@Component
public class MarketWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(MarketWebSocketHandler.class);

    /** Active WebSocket sessions. CopyOnWriteArrayList for thread-safe iteration during broadcasts. */
    private final CopyOnWriteArrayList<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    /** Optional per-session symbol filter. If empty set → send all symbols. */
    private final ConcurrentHashMap<String, Set<String>> sessionSubscriptions = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;

    public MarketWebSocketHandler() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        sessionSubscriptions.put(session.getId(), new HashSet<>());
        log.info("WS connected: {} (total: {})", session.getId(), sessions.size());

        // Send welcome message
        try {
            Map<String, Object> welcome = Map.of(
                "type", "CONNECTED",
                "message", "Connected to TradeX Market Data (SIMULATED)",
                "dataSource", "SIMULATED",
                "timestamp", Instant.now().toString()
            );
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(welcome)));
        } catch (IOException e) {
            log.warn("Failed to send welcome to session {}", session.getId());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
            String action = (String) payload.get("action");

            if ("subscribe".equals(action)) {
                @SuppressWarnings("unchecked")
                List<String> symbols = (List<String>) payload.get("symbols");
                if (symbols != null && !symbols.isEmpty()) {
                    sessionSubscriptions.put(session.getId(),
                        new HashSet<>(symbols.stream().map(String::toUpperCase).toList()));
                    log.debug("Session {} subscribed to: {}", session.getId(), symbols);
                }
            } else if ("unsubscribe".equals(action)) {
                sessionSubscriptions.put(session.getId(), new HashSet<>());
            } else if ("ping".equals(action)) {
                session.sendMessage(new TextMessage("{\"type\":\"pong\"}"));
            }
        } catch (Exception e) {
            log.warn("Error handling WS message from session {}: {}", session.getId(), e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        sessionSubscriptions.remove(session.getId());
        log.info("WS disconnected: {} status={} (remaining: {})",
                session.getId(), status, sessions.size());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("WS transport error for session {}: {}", session.getId(), exception.getMessage());
        sessions.remove(session);
        sessionSubscriptions.remove(session.getId());
    }

    /**
     * Broadcast price updates to all connected subscribers.
     * Called by {@link com.tradex.gateway.marketdata.MarketDataEngine} on each tick.
     */
    public void broadcastPriceUpdates(List<Stock> stocks) {
        if (sessions.isEmpty()) return;

        try {
            List<Map<String, Object>> updates = stocks.stream().map(s -> {
                BigDecimal changeAmount = s.getCurrentPrice().subtract(s.getPreviousClose())
                        .setScale(4, RoundingMode.HALF_UP);
                BigDecimal changePercent = s.getPreviousClose().compareTo(BigDecimal.ZERO) == 0
                        ? BigDecimal.ZERO
                        : changeAmount.divide(s.getPreviousClose(), 6, RoundingMode.HALF_UP)
                              .multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP);

                Map<String, Object> update = new LinkedHashMap<>();
                update.put("symbol", s.getSymbol());
                update.put("companyName", s.getCompanyName());
                update.put("price", s.getCurrentPrice());
                update.put("openPrice", s.getOpenPrice());
                update.put("dayHigh", s.getHighPrice());
                update.put("dayLow", s.getLowPrice());
                update.put("previousClose", s.getPreviousClose());
                update.put("changeAmount", changeAmount);
                update.put("changePercent", changePercent);
                update.put("volume", s.getVolume());
                update.put("dataSource", s.getDataSource());
                update.put("priceTimestamp", s.getPriceTimestamp() != null
                        ? s.getPriceTimestamp().toString() : Instant.now().toString());
                return update;
            }).toList();

            Map<String, Object> message = new LinkedHashMap<>();
            message.put("type", "PRICE_UPDATE");
            message.put("dataSource", "SIMULATED");
            message.put("timestamp", Instant.now().toString());
            message.put("updates", updates);

            String json = objectMapper.writeValueAsString(message);

            List<WebSocketSession> stale = new ArrayList<>();
            for (WebSocketSession session : sessions) {
                if (!session.isOpen()) {
                    stale.add(session);
                    continue;
                }
                try {
                    Set<String> subscriptions = sessionSubscriptions.getOrDefault(session.getId(), Set.of());
                    if (!subscriptions.isEmpty()) {
                        // Send filtered update
                        List<Map<String, Object>> filtered = updates.stream()
                                .filter(u -> subscriptions.contains(u.get("symbol")))
                                .toList();
                        if (filtered.isEmpty()) continue;
                        Map<String, Object> filteredMsg = new LinkedHashMap<>(message);
                        filteredMsg.put("updates", filtered);
                        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(filteredMsg)));
                    } else {
                        session.sendMessage(new TextMessage(json));
                    }
                } catch (IOException e) {
                    log.warn("Failed to send to session {}, marking stale", session.getId());
                    stale.add(session);
                }
            }

            // Cleanup stale sessions
            if (!stale.isEmpty()) {
                sessions.removeAll(stale);
                stale.forEach(s -> sessionSubscriptions.remove(s.getId()));
            }
        } catch (Exception e) {
            log.error("Error broadcasting price updates", e);
        }
    }

    public int getActiveSessionCount() {
        return sessions.size();
    }
}
