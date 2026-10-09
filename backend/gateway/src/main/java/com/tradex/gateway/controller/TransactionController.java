package com.tradex.gateway.controller;

import com.tradex.gateway.dto.UnifiedTransactionDto;
import com.tradex.gateway.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * Unified transaction history API with clearly distinguishable record types.
     */
    @GetMapping
    public ResponseEntity<List<UnifiedTransactionDto>> getTransactions(
            Authentication auth,
            @RequestParam(required = false) String type) {
        return ResponseEntity.ok(transactionService.getUnifiedHistory(auth.getName(), type));
    }

    /**
     * Post an auditable compensating entry for financial corrections.
     */
    @PostMapping("/compensating")
    public ResponseEntity<?> postCompensatingEntry(
            Authentication auth,
            @RequestBody Map<String, Object> body) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN") || a.getAuthority().equalsIgnoreCase("ADMIN"));
        if (!isAdmin) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin privileges required for compensating financial entries"));
        }

        try {
            String targetUser = (String) body.get("username");
            BigDecimal amount = new BigDecimal(body.get("amount").toString());
            String reason = (String) body.get("reason");
            String correlationId = (String) body.get("correlationId");

            return ResponseEntity.ok(transactionService.postCompensatingEntry(targetUser, amount, reason, correlationId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
