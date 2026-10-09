package com.tradex.gateway.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_transactions")
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Column(nullable = false, length = 30)
    private String type; // DEPOSIT, WITHDRAWAL, TRADE_DEBIT, TRADE_CREDIT, FUND_RESERVATION, FUND_RELEASE, COMPENSATING_CREDIT, COMPENSATING_DEBIT

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal amount;

    @Column(name = "balance_before", nullable = false, precision = 15, scale = 4)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", nullable = false, precision = 15, scale = 4)
    private BigDecimal balanceAfter;

    private String description;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;

    @Column(name = "status", length = 20)
    private String status = "COMPLETED";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Wallet getWallet() { return wallet; }
    public void setWallet(Wallet wallet) { this.wallet = wallet; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getBalanceBefore() { return balanceBefore; }
    public void setBalanceBefore(BigDecimal balanceBefore) { this.balanceBefore = balanceBefore; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getReferenceId() { return referenceId; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
