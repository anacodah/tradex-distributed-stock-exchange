package com.tradex.common.entity;

import jakarta.persistence.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "actor_username", length = 100)
    private String actorUsername;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "resource", length = 100)
    private String resource;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id", length = 64)
    private String entityId;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "outcome", length = 30)
    private String outcome = "SUCCESS";

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt = ZonedDateTime.now();

    public AuditLog() {}

    public AuditLog(Long userId, String actorUsername, String action, String resource, String entityType, String entityId, String correlationId, String outcome, String details, String ipAddress) {
        this.userId = userId;
        this.actorUsername = actorUsername;
        this.action = action;
        this.resource = resource;
        this.entityType = entityType;
        this.entityId = entityId;
        this.correlationId = correlationId;
        this.outcome = outcome;
        this.details = details;
        this.ipAddress = ipAddress;
        this.createdAt = ZonedDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getActorUsername() { return actorUsername; }
    public void setActorUsername(String actorUsername) { this.actorUsername = actorUsername; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getResource() { return resource; }
    public void setResource(String resource) { this.resource = resource; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }
}
