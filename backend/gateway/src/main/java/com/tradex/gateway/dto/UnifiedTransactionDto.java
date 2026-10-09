package com.tradex.gateway.dto;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public class UnifiedTransactionDto {

    private String id;
    private String type; // ORDER_SUBMISSION, TRADE_EXECUTION, WALLET_DEBIT, WALLET_CREDIT, FUND_RESERVATION, FUND_RELEASE, COMPENSATING_ENTRY, DISTRIBUTED_EVENT
    private ZonedDateTime timestamp;
    private Long userId;
    private String username;
    private String symbol;
    private Long orderId;
    private String tradeId;
    private String correlationId;
    private BigDecimal amount;
    private BigDecimal quantity;
    private BigDecimal price;
    private String description;
    private String status;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public ZonedDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(ZonedDateTime timestamp) { this.timestamp = timestamp; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getTradeId() { return tradeId; }
    public void setTradeId(String tradeId) { this.tradeId = tradeId; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
