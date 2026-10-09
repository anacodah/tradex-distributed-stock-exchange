package com.tradex.common.engine;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Representation of an active or resting order in the matching engine.
 */
public class BookOrder implements Comparable<BookOrder> {

    private final Long orderId;
    private final Long userId;
    private final String symbol;
    private final OrderSide side;
    private final OrderType orderType;
    private BigDecimal price; // null for pure market order
    private BigDecimal stopPrice;
    private BigDecimal quantity;
    private BigDecimal filledQuantity;
    private Long sequenceNumber;
    private ZonedDateTime timestamp;

    public BookOrder(Long orderId,
                     Long userId,
                     String symbol,
                     OrderSide side,
                     OrderType orderType,
                     BigDecimal price,
                     BigDecimal stopPrice,
                     BigDecimal quantity,
                     BigDecimal filledQuantity,
                     Long sequenceNumber,
                     ZonedDateTime timestamp) {
        this.orderId = orderId;
        this.userId = userId;
        this.symbol = symbol;
        this.side = side;
        this.orderType = orderType;
        this.price = price;
        this.stopPrice = stopPrice;
        this.quantity = quantity;
        this.filledQuantity = filledQuantity != null ? filledQuantity : BigDecimal.ZERO;
        this.sequenceNumber = sequenceNumber;
        this.timestamp = timestamp != null ? timestamp : ZonedDateTime.now();
    }

    public BigDecimal getRemainingQuantity() {
        return quantity.subtract(filledQuantity);
    }

    public boolean isFullyFilled() {
        return getRemainingQuantity().compareTo(BigDecimal.ZERO) <= 0;
    }

    /**
     * Priority rules:
     * - BUY: Highest price first (DESC), then lowest sequenceNumber (ASC).
     * - SELL: Lowest price first (ASC), then lowest sequenceNumber (ASC).
     */
    @Override
    public int compareTo(BookOrder other) {
        if (this == other) return 0;
        if (other == null) return -1;

        if (this.side != other.side) {
            return this.side.compareTo(other.side);
        }

        if (this.side == OrderSide.BUY) {
            // Price DESC
            int priceCmp = other.price.compareTo(this.price);
            if (priceCmp != 0) return priceCmp;
        } else {
            // Price ASC
            int priceCmp = this.price.compareTo(other.price);
            if (priceCmp != 0) return priceCmp;
        }

        // Time priority: earlier sequence number first (ASC)
        if (this.sequenceNumber != null && other.sequenceNumber != null) {
            int seqCmp = this.sequenceNumber.compareTo(other.sequenceNumber);
            if (seqCmp != 0) return seqCmp;
        }

        return this.orderId.compareTo(other.orderId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BookOrder bookOrder = (BookOrder) o;
        return Objects.equals(orderId, bookOrder.orderId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId);
    }

    // Getters and Setters
    public Long getOrderId() { return orderId; }
    public Long getUserId() { return userId; }
    public String getSymbol() { return symbol; }
    public OrderSide getSide() { return side; }
    public OrderType getOrderType() { return orderType; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getStopPrice() { return stopPrice; }
    public void setStopPrice(BigDecimal stopPrice) { this.stopPrice = stopPrice; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getFilledQuantity() { return filledQuantity; }
    public void setFilledQuantity(BigDecimal filledQuantity) { this.filledQuantity = filledQuantity; }
    public Long getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(Long sequenceNumber) { this.sequenceNumber = sequenceNumber; }
    public ZonedDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(ZonedDateTime timestamp) { this.timestamp = timestamp; }
}
