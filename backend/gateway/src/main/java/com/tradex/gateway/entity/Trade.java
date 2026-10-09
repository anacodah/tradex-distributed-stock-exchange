package com.tradex.gateway.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trades")
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trade_id", unique = true, length = 64)
    private String tradeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User seller;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "stock_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Stock stock;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maker_order_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Order makerOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taker_order_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Order takerOrder;

    @Column(nullable = false, length = 10)
    private String side; // BUY, SELL

    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal quantity;

    @Column(name = "execution_price", nullable = false, precision = 15, scale = 4)
    private BigDecimal executionPrice;

    @Column(name = "total_value", nullable = false, precision = 15, scale = 4)
    private BigDecimal totalValue;

    @Column(name = "executed_at", nullable = false, updatable = false)
    private LocalDateTime executedAt = LocalDateTime.now();

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTradeId() { return tradeId; }
    public void setTradeId(String tradeId) { this.tradeId = tradeId; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public User getBuyer() { return buyer; }
    public void setBuyer(User buyer) { this.buyer = buyer; }
    public User getSeller() { return seller; }
    public void setSeller(User seller) { this.seller = seller; }
    public Stock getStock() { return stock; }
    public void setStock(Stock stock) { this.stock = stock; }
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
    public Order getMakerOrder() { return makerOrder; }
    public void setMakerOrder(Order makerOrder) { this.makerOrder = makerOrder; }
    public Order getTakerOrder() { return takerOrder; }
    public void setTakerOrder(Order takerOrder) { this.takerOrder = takerOrder; }
    public String getSide() { return side; }
    public void setSide(String side) { this.side = side; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getExecutionPrice() { return executionPrice; }
    public void setExecutionPrice(BigDecimal executionPrice) { this.executionPrice = executionPrice; }
    public BigDecimal getTotalValue() { return totalValue; }
    public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }
    public LocalDateTime getExecutedAt() { return executedAt; }
    public void setExecutedAt(LocalDateTime executedAt) { this.executedAt = executedAt; }
}
