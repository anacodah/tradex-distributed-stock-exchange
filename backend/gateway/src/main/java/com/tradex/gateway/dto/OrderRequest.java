package com.tradex.gateway.dto;

import java.math.BigDecimal;

public class OrderRequest {

    private String symbol;
    private String side; // BUY, SELL
    private String orderType; // MARKET, LIMIT, STOP_LOSS
    private BigDecimal quantity;
    private BigDecimal price; // required for LIMIT
    private BigDecimal stopPrice; // required for STOP_LOSS
    private String clientOrderId;

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getSide() { return side; }
    public void setSide(String side) { this.side = side; }

    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getStopPrice() { return stopPrice; }
    public void setStopPrice(BigDecimal stopPrice) { this.stopPrice = stopPrice; }

    public String getClientOrderId() { return clientOrderId; }
    public void setClientOrderId(String clientOrderId) { this.clientOrderId = clientOrderId; }
}
