package com.tradex.gateway.service;

import com.tradex.common.entity.Notification;
import com.tradex.common.repository.NotificationRepository;
import com.tradex.gateway.entity.User;
import com.tradex.gateway.repository.UserRepository;
import com.tradex.gateway.websocket.NotificationWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationWebSocketHandler webSocketHandler;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository,
                               NotificationWebSocketHandler webSocketHandler) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.webSocketHandler = webSocketHandler;
    }

    /**
     * Persist notification with deduplication check before delivering.
     */
    @Transactional
    public Notification sendNotification(Long userId, String title, String message, String type,
                                         String dedupKey, Long orderId, String tradeId, String linkUrl) {
        // 1. Deduplication check
        if (dedupKey != null && !dedupKey.isBlank()) {
            Optional<Notification> existing = notificationRepository.findByDedupKey(dedupKey);
            if (existing.isPresent()) {
                log.debug("Duplicate notification skipped for dedupKey: {}", dedupKey);
                return existing.get();
            }
        }

        // 2. Persist notification
        Notification notification = new Notification(userId, title, message, type, dedupKey, orderId, tradeId, linkUrl);
        notification = notificationRepository.save(notification);

        // 3. Deliver via WebSocket push
        final Notification savedNotification = notification;
        userRepository.findById(userId).ifPresent(user -> {
            try {
                webSocketHandler.pushNotification(user.getUsername(), savedNotification);
            } catch (Exception e) {
                log.warn("Failed to push notification via WebSocket: {}", e.getMessage());
            }
        });

        return notification;
    }

    @Transactional(readOnly = true)
    public List<Notification> getNotifications(String username, int page, int size) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return notificationRepository.countByUserIdAndIsReadFalse(user.getId());
    }

    @Transactional
    public Notification markAsRead(String username, Long notificationId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        if (!notification.getUserId().equals(user.getId())) {
            throw new SecurityException("Cannot mark notification belonging to another user");
        }
        notification.setIsRead(true);
        return notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        notificationRepository.markAllAsRead(user.getId());
    }
}
