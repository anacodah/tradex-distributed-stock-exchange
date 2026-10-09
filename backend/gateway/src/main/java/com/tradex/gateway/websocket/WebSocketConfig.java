package com.tradex.gateway.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket configuration.
 * Registers:
 * - /ws/market : market price push updates
 * - /ws/notifications : real-time in-app user notifications
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final MarketWebSocketHandler marketWebSocketHandler;
    private final NotificationWebSocketHandler notificationWebSocketHandler;
    private final ClusterEventsWebSocketHandler clusterEventsWebSocketHandler;

    public WebSocketConfig(MarketWebSocketHandler marketWebSocketHandler,
                           NotificationWebSocketHandler notificationWebSocketHandler,
                           ClusterEventsWebSocketHandler clusterEventsWebSocketHandler) {
        this.marketWebSocketHandler = marketWebSocketHandler;
        this.notificationWebSocketHandler = notificationWebSocketHandler;
        this.clusterEventsWebSocketHandler = clusterEventsWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(marketWebSocketHandler, "/ws/market")
                .setAllowedOrigins("*");
        registry.addHandler(notificationWebSocketHandler, "/ws/notifications")
                .setAllowedOrigins("*");
        registry.addHandler(clusterEventsWebSocketHandler, "/ws/cluster")
                .setAllowedOrigins("*");
    }
}
