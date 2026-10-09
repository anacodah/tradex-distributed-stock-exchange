package com.tradex.gateway.controller;

import com.tradex.common.entity.AuditLog;
import com.tradex.gateway.service.AuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Users can view their own permitted audit history.
     */
    @GetMapping
    public ResponseEntity<List<AuditLog>> getUserAuditLogs(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(auditService.getUserAuditLogs(auth.getName(), page, size));
    }

    /**
     * Administrators can access complete authorized audit views.
     */
    @GetMapping("/admin")
    public ResponseEntity<List<AuditLog>> getAllAuditLogs(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        // Enforce admin access: verify user has ADMIN authority
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN") || a.getAuthority().equalsIgnoreCase("ADMIN"));
        if (!isAdmin) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(auditService.getAllAuditLogsForAdmin(page, size));
    }

    /**
     * Trace events by correlation ID.
     */
    @GetMapping("/correlation/{correlationId}")
    public ResponseEntity<List<AuditLog>> getByCorrelationId(@PathVariable String correlationId) {
        return ResponseEntity.ok(auditService.getByCorrelationId(correlationId));
    }
}
