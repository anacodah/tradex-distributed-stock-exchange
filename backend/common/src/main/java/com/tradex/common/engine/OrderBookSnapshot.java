package com.tradex.common.engine;

import java.math.BigDecimal;
import java.util.List;

public class OrderBookSnapshot {

    public static class Level {
        private final BigDecimal price;
        private final BigDecimal quantity;
        private final int orderCount;

        public Level(BigDecimal price, BigDecimal quantity, int orderCount) {
            this.price = price;
            this.quantity = quantity;
            this.orderCount = orderCount;
        }

        public BigDecimal getPrice() { return price; }
        public BigDecimal getQuantity() { return quantity; }
        public int getOrderCount() { return orderCount; }
    }

    private final String symbol;
    private final List<Level> bids;
    private final List<Level> asks;

    public OrderBookSnapshot(String symbol, List<Level> bids, List<Level> asks) {
        this.symbol = symbol;
        this.bids = bids;
        this.asks = asks;
    }

    public String getSymbol() { return symbol; }
    public List<Level> getBids() { return bids; }
    public List<Level> getAsks() { return asks; }
}
