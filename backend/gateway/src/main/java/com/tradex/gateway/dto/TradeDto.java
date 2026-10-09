package com.tradex.gateway.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TradeDto {

    private Long id;
    private String tradeId;
    private Long orderId;
    private String symbol;
    private String side;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal totalValue;
    private LocalDateTime executedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTradeId() { return tradeId; }
    public void setTradeId(String tradeId) { this.tradeId = tradeId; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getSide() { return side; }
    public void setSide(String side) { this.side = side; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getTotalValue() { return totalValue; }
    public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }
    public LocalDateTime getExecutedAt() { return executedAt; }
    public void setExecutedAt(LocalDateTime executedAt) { this.executedAt = executedAt; }
}
