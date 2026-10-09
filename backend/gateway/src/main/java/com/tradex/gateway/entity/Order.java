package com.tradex.gateway.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "stock_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Stock stock;

    @Column(nullable = false, length = 10)
    private String side; // BUY, SELL

    @Column(name = "order_type", nullable = false, length = 15)
    private String orderType; // MARKET, LIMIT, STOP_LOSS

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal quantity;

    @Column(name = "filled_quantity", nullable = false, precision = 15, scale = 4)
    private BigDecimal filledQuantity = BigDecimal.ZERO;

    @Column(precision = 15, scale = 4)
    private BigDecimal price; // null for MARKET orders

    @Column(name = "stop_price", precision = 15, scale = 4)
    private BigDecimal stopPrice;

    @Column(name = "execution_price", precision = 15, scale = 4)
    private BigDecimal executionPrice;

    @Column(name = "reserved_amount", precision = 15, scale = 4)
    private BigDecimal reservedAmount = BigDecimal.ZERO;

    @Column(name = "reserved_shares", precision = 15, scale = 4)
    private BigDecimal reservedShares = BigDecimal.ZERO;

    @Column(name = "sequence_number")
    private Long sequenceNumber;

    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;

    @Column(name = "client_order_id", length = 64)
    private String clientOrderId;

    @Column(nullable = false, length = 20)
    private String status = "NEW"; // NEW, OPEN, PARTIALLY_FILLED, FILLED, CANCEL_PENDING, CANCELLED, REJECTED, EXPIRED

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() { this.updatedAt = LocalDateTime.now(); }

    public BigDecimal getRemainingQuantity() {
        if (quantity == null) return BigDecimal.ZERO;
        BigDecimal filled = filledQuantity != null ? filledQuantity : BigDecimal.ZERO;
        return quantity.subtract(filled).max(BigDecimal.ZERO);
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Stock getStock() { return stock; }
    public void setStock(Stock stock) { this.stock = stock; }
    public String getSide() { return side; }
    public void setSide(String side) { this.side = side; }
    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getFilledQuantity() { return filledQuantity; }
    public void setFilledQuantity(BigDecimal filledQuantity) { this.filledQuantity = filledQuantity; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getStopPrice() { return stopPrice; }
    public void setStopPrice(BigDecimal stopPrice) { this.stopPrice = stopPrice; }
    public BigDecimal getExecutionPrice() { return executionPrice; }
    public void setExecutionPrice(BigDecimal executionPrice) { this.executionPrice = executionPrice; }
    public BigDecimal getReservedAmount() { return reservedAmount; }
    public void setReservedAmount(BigDecimal reservedAmount) { this.reservedAmount = reservedAmount; }
    public BigDecimal getReservedShares() { return reservedShares; }
    public void setReservedShares(BigDecimal reservedShares) { this.reservedShares = reservedShares; }
    public Long getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(Long sequenceNumber) { this.sequenceNumber = sequenceNumber; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getClientOrderId() { return clientOrderId; }
    public void setClientOrderId(String clientOrderId) { this.clientOrderId = clientOrderId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
