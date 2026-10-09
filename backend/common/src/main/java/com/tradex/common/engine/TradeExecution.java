package com.tradex.common.engine;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Immutable record of a matched trade execution between a maker and taker order.
 */
public class TradeExecution {

    private final String tradeId;
    private final String symbol;
    private final Long makerOrderId;
    private final Long takerOrderId;
    private final Long buyerUserId;
    private final Long sellerUserId;
    private final OrderSide takerSide;
    private final BigDecimal price;
    private final BigDecimal quantity;
    private final BigDecimal totalValue;
    private final ZonedDateTime executedAt;

    public TradeExecution(String symbol,
                          Long makerOrderId,
                          Long takerOrderId,
                          Long buyerUserId,
                          Long sellerUserId,
                          OrderSide takerSide,
                          BigDecimal price,
                          BigDecimal quantity,
                          BigDecimal totalValue) {
        this.tradeId = "TRD-" + UUID.randomUUID().toString();
        this.symbol = symbol;
        this.makerOrderId = makerOrderId;
        this.takerOrderId = takerOrderId;
        this.buyerUserId = buyerUserId;
        this.sellerUserId = sellerUserId;
        this.takerSide = takerSide;
        this.price = price;
        this.quantity = quantity;
        this.totalValue = totalValue;
        this.executedAt = ZonedDateTime.now();
    }

    public String getTradeId() { return tradeId; }
    public String getSymbol() { return symbol; }
    public Long getMakerOrderId() { return makerOrderId; }
    public Long getTakerOrderId() { return takerOrderId; }
    public Long getBuyerUserId() { return buyerUserId; }
    public Long getSellerUserId() { return sellerUserId; }
    public OrderSide getTakerSide() { return takerSide; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getTotalValue() { return totalValue; }
    public ZonedDateTime getExecutedAt() { return executedAt; }
}
