package com.tradex.common.engine;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Thread-safe OrderBook maintaining Price-Time (FIFO) priority:
 * - Bids: Highest price first (descending), then earliest sequence number.
 * - Asks: Lowest price first (ascending), then earliest sequence number.
 */
public class OrderBook {

    private final String symbol;

    // Bids priority: Price DESC, Sequence ASC
    private final NavigableSet<BookOrder> bids = new ConcurrentSkipListSet<>();

    // Asks priority: Price ASC, Sequence ASC
    private final NavigableSet<BookOrder> asks = new ConcurrentSkipListSet<>();

    // Map by orderId for quick lookup, modification, and cancellation
    private final Map<Long, BookOrder> activeOrders = new ConcurrentHashMap<>();

    public OrderBook(String symbol) {
        this.symbol = symbol;
    }

    public synchronized void addOrder(BookOrder order) {
        if (order.isFullyFilled()) return;
        activeOrders.put(order.getOrderId(), order);
        if (order.getSide() == OrderSide.BUY) {
            bids.add(order);
        } else {
            asks.add(order);
        }
    }

    public synchronized BookOrder removeOrder(Long orderId) {
        BookOrder order = activeOrders.remove(orderId);
        if (order != null) {
            if (order.getSide() == OrderSide.BUY) {
                bids.remove(order);
            } else {
                asks.remove(order);
            }
        }
        return order;
    }

    public synchronized BookOrder getOrder(Long orderId) {
        return activeOrders.get(orderId);
    }

    public synchronized NavigableSet<BookOrder> getBids() {
        return bids;
    }

    public synchronized NavigableSet<BookOrder> getAsks() {
        return asks;
    }

    public String getSymbol() {
        return symbol;
    }

    /**
     * Snapshot aggregated price levels (L2 order book view).
     */
    public synchronized OrderBookSnapshot getSnapshot(int depth) {
        Map<BigDecimal, BigDecimal> bidAgg = new LinkedHashMap<>();
        Map<BigDecimal, Integer> bidCounts = new LinkedHashMap<>();
        for (BookOrder b : bids) {
            bidAgg.merge(b.getPrice(), b.getRemainingQuantity(), BigDecimal::add);
            bidCounts.merge(b.getPrice(), 1, Integer::sum);
        }

        Map<BigDecimal, BigDecimal> askAgg = new LinkedHashMap<>();
        Map<BigDecimal, Integer> askCounts = new LinkedHashMap<>();
        for (BookOrder a : asks) {
            askAgg.merge(a.getPrice(), a.getRemainingQuantity(), BigDecimal::add);
            askCounts.merge(a.getPrice(), 1, Integer::sum);
        }

        List<OrderBookSnapshot.Level> bidLevels = new ArrayList<>();
        int count = 0;
        for (Map.Entry<BigDecimal, BigDecimal> entry : bidAgg.entrySet()) {
            if (count++ >= depth) break;
            bidLevels.add(new OrderBookSnapshot.Level(entry.getKey(), entry.getValue(), bidCounts.get(entry.getKey())));
        }

        List<OrderBookSnapshot.Level> askLevels = new ArrayList<>();
        count = 0;
        for (Map.Entry<BigDecimal, BigDecimal> entry : askAgg.entrySet()) {
            if (count++ >= depth) break;
            askLevels.add(new OrderBookSnapshot.Level(entry.getKey(), entry.getValue(), askCounts.get(entry.getKey())));
        }

        return new OrderBookSnapshot(symbol, bidLevels, askLevels);
    }
}
