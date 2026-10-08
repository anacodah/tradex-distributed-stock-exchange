package com.tradex.gateway.controller;

import com.tradex.gateway.entity.Wallet;
import com.tradex.gateway.entity.WalletTransaction;
import com.tradex.gateway.service.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    public ResponseEntity<Wallet> getWallet(Authentication auth) {
        return ResponseEntity.ok(walletService.getOrCreateWallet(auth.getName()));
    }

    @PostMapping("/deposit")
    public ResponseEntity<?> deposit(Authentication auth, @RequestBody Map<String, Object> body) {
        try {
            BigDecimal amount = new BigDecimal(body.get("amount").toString());
            return ResponseEntity.ok(walletService.deposit(auth.getName(), amount));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(Authentication auth, @RequestBody Map<String, Object> body) {
        try {
            BigDecimal amount = new BigDecimal(body.get("amount").toString());
            return ResponseEntity.ok(walletService.withdraw(auth.getName(), amount));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<WalletTransaction>> getTransactions(Authentication auth) {
        return ResponseEntity.ok(walletService.getTransactions(auth.getName()));
    }
}
