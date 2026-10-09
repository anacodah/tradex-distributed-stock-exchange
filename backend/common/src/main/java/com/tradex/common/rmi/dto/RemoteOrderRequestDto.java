package com.tradex.common.rmi.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class RemoteOrderRequestDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String requestId;
    private String correlationId;
    private Long orderId;
    private Long userId;
    private String username;
    private String symbol;
    private String side;        // BUY, SELL
    private String orderType;   // MARKET, LIMIT, STOP_LOSS
    private BigDecimal price;
    private BigDecimal stopPrice;
    private BigDecimal quantity;
    private String idempotencyKey;
    private long sourceTimestamp;

    public RemoteOrderRequestDto() {}

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getSide() { return side; }
    public void setSide(String side) { this.side = side; }

    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getStopPrice() { return stopPrice; }
    public void setStopPrice(BigDecimal stopPrice) { this.stopPrice = stopPrice; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public long getSourceTimestamp() { return sourceTimestamp; }
    public void setSourceTimestamp(long sourceTimestamp) { this.sourceTimestamp = sourceTimestamp; }
}
