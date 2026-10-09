package com.tradex.common.entity;

import jakarta.persistence.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "type", length = 30)
    private String type = "INFO"; // INFO, ORDER_ACCEPTED, ORDER_REJECTED, TRADE_EXECUTED, ORDER_CANCELLED, STOP_TRIGGERED

    @Column(name = "is_read")
    private Boolean isRead = false;

    @Column(name = "dedup_key", unique = true, length = 128)
    private String dedupKey;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "trade_id", length = 64)
    private String tradeId;

    @Column(name = "link_url", length = 255)
    private String linkUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt = ZonedDateTime.now();

    public Notification() {}

    public Notification(Long userId, String title, String message, String type, String dedupKey, Long orderId, String tradeId, String linkUrl) {
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.dedupKey = dedupKey;
        this.orderId = orderId;
        this.tradeId = tradeId;
        this.linkUrl = linkUrl;
        this.isRead = false;
        this.createdAt = ZonedDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }

    public String getDedupKey() { return dedupKey; }
    public void setDedupKey(String dedupKey) { this.dedupKey = dedupKey; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getTradeId() { return tradeId; }
    public void setTradeId(String tradeId) { this.tradeId = tradeId; }

    public String getLinkUrl() { return linkUrl; }
    public void setLinkUrl(String linkUrl) { this.linkUrl = linkUrl; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }
}
