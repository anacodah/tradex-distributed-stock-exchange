package com.tradex.gateway.service;

import com.tradex.common.entity.AuditLog;
import com.tradex.common.repository.AuditLogRepository;
import com.tradex.gateway.entity.User;
import com.tradex.gateway.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    /**
     * Records an audit log entry. Sanitizes details so that credentials, tokens, or passwords are never stored.
     */
    @Transactional
    public AuditLog recordAudit(Long userId, String actorUsername, String action, String resource,
                                String entityType, String entityId, String correlationId,
                                String outcome, String details, String ipAddress) {
        String sanitizedDetails = sanitize(details);
        AuditLog audit = new AuditLog(userId, actorUsername, action, resource, entityType, entityId,
                correlationId, outcome != null ? outcome : "SUCCESS", sanitizedDetails, ipAddress);
        return auditLogRepository.save(audit);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getUserAuditLogs(String username, int page, int size) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return auditLogRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getAllAuditLogsForAdmin(int page, int size) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getByCorrelationId(String correlationId) {
        return auditLogRepository.findByCorrelationIdOrderByCreatedAtDesc(correlationId);
    }

    private String sanitize(String text) {
        if (text == null) return null;
        return text.replaceAll("(?i)(password|token|secret|jwt)=[^&\\s,;]+", "$1=***REDACTED***");
    }
}
