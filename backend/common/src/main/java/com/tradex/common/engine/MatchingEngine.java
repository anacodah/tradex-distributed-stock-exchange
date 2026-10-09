package com.tradex.common.engine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Authoritative, deterministic Matching Engine.
 *
 * Rules:
 * 1. Price-Time Priority (FIFO):
 *    - BUY orders prioritized by highest limit price, then earliest sequence number.
 *    - SELL orders prioritized by lowest limit price, then earliest sequence number.
 * 2. Execution Price:
 *    - Trades execute at the resting maker's price.
 * 3. Market Orders:
 *    - Execute against available resting liquidity in the opposite book.
 *    - Never promise fills when liquidity is insufficient — unfilled remainder is discarded/unfilled.
 * 4. Partial Fills:
 *    - Supported; fills up to available maker size, reduces quantities, and continues matching.
 * 5. Modifications:
 *    - Atomically updates price and/or quantity.
 *    - A price-changing modification or quantity increase loses time priority (requires new sequence number).
 * 6. Stop-Loss Triggering:
 *    - Evaluated deterministically when market prices update.
 */
public class MatchingEngine {

    private final Map<String, OrderBook> books = new ConcurrentHashMap<>();
    private final Map<String, List<BookOrder>> pendingStopLossOrders = new ConcurrentHashMap<>();

    public OrderBook getOrCreateBook(String symbol) {
        return books.computeIfAbsent(symbol.toUpperCase(), OrderBook::new);
    }

    public synchronized MatchResult match(BookOrder taker, BigDecimal currentMarketPrice) {
        OrderBook book = getOrCreateBook(taker.getSymbol());
        MatchResult result = new MatchResult(taker);

        if (taker.getOrderType() == OrderType.STOP_LOSS) {
            // Register as stop-loss order until triggered
            pendingStopLossOrders.computeIfAbsent(taker.getSymbol().toUpperCase(), k -> new ArrayList<>()).add(taker);
            result.setFinalStatus(OrderStatus.OPEN);
            result.setRestedInBook(false);
            return result;
        }

        NavigableSet<BookOrder> oppositeQueue = (taker.getSide() == OrderSide.BUY) ? book.getAsks() : book.getBids();

        Iterator<BookOrder> iterator = oppositeQueue.iterator();
        while (iterator.hasNext() && !taker.isFullyFilled()) {
            BookOrder maker = iterator.next();

            // Self-trade prevention (same user cannot fill against themselves)
            if (maker.getUserId().equals(taker.getUserId())) {
                continue;
            }

            // Check if price crosses
            boolean priceCrossed;
            if (taker.getOrderType() == OrderType.MARKET) {
                priceCrossed = true; // Market orders accept best available maker price
            } else {
                // Limit order price check
                if (taker.getSide() == OrderSide.BUY) {
                    priceCrossed = maker.getPrice().compareTo(taker.getPrice()) <= 0;
                } else {
                    priceCrossed = maker.getPrice().compareTo(taker.getPrice()) >= 0;
                }
            }

            if (!priceCrossed) {
                // Since queue is sorted by price, no subsequent orders will cross
                break;
            }

            // Calculate fillable quantity
            BigDecimal makerRemaining = maker.getRemainingQuantity();
            BigDecimal takerRemaining = taker.getRemainingQuantity();
            BigDecimal tradeQuantity = makerRemaining.min(takerRemaining);

            if (tradeQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal executionPrice = maker.getPrice(); // Maker sets execution price
            BigDecimal totalValue = executionPrice.multiply(tradeQuantity).setScale(4, RoundingMode.HALF_UP);

            // Update filled quantities
            maker.setFilledQuantity(maker.getFilledQuantity().add(tradeQuantity));
            taker.setFilledQuantity(taker.getFilledQuantity().add(tradeQuantity));

            Long buyerUserId = (taker.getSide() == OrderSide.BUY) ? taker.getUserId() : maker.getUserId();
            Long sellerUserId = (taker.getSide() == OrderSide.SELL) ? taker.getUserId() : maker.getUserId();

            TradeExecution trade = new TradeExecution(
                    taker.getSymbol(),
                    maker.getOrderId(),
                    taker.getOrderId(),
                    buyerUserId,
                    sellerUserId,
                    taker.getSide(),
                    executionPrice,
                    tradeQuantity,
                    totalValue
            );
            result.addTrade(trade);

            if (maker.isFullyFilled()) {
                iterator.remove();
                book.removeOrder(maker.getOrderId());
            }
        }

        // Determine final taker status
        if (taker.isFullyFilled()) {
            result.setFinalStatus(OrderStatus.FILLED);
            result.setRestedInBook(false);
        } else if (taker.getFilledQuantity().compareTo(BigDecimal.ZERO) > 0) {
            // Partially filled
            if (taker.getOrderType() == OrderType.LIMIT) {
                book.addOrder(taker);
                result.setFinalStatus(OrderStatus.PARTIALLY_FILLED);
                result.setRestedInBook(true);
            } else {
                // Market order: no resting in book for remaining unfilled shares if liquidity exhausted
                result.setFinalStatus(OrderStatus.PARTIALLY_FILLED);
                result.setRestedInBook(false);
            }
        } else {
            // No fills
            if (taker.getOrderType() == OrderType.LIMIT) {
                book.addOrder(taker);
                result.setFinalStatus(OrderStatus.OPEN);
                result.setRestedInBook(true);
            } else {
                // Market order with zero liquidity
                result.setFinalStatus(OrderStatus.REJECTED);
                result.setRestedInBook(false);
            }
        }

        return result;
    }

    /**
     * Evaluate pending stop-loss orders for a symbol given latest price.
     * Returns list of orders that were triggered.
     */
    public synchronized List<BookOrder> evaluateStopLossTriggers(String symbol, BigDecimal latestPrice) {
        List<BookOrder> pending = pendingStopLossOrders.get(symbol.toUpperCase());
        if (pending == null || pending.isEmpty()) {
            return Collections.emptyList();
        }

        List<BookOrder> triggered = new ArrayList<>();
        Iterator<BookOrder> it = pending.iterator();
        while (it.hasNext()) {
            BookOrder stopOrder = it.next();
            boolean isTriggered = false;

            if (stopOrder.getSide() == OrderSide.SELL) {
                // Sell stop loss triggers if market price drops to or below stop price
                if (latestPrice.compareTo(stopOrder.getStopPrice()) <= 0) {
                    isTriggered = true;
                }
            } else if (stopOrder.getSide() == OrderSide.BUY) {
                // Buy stop loss triggers if market price rises to or above stop price
                if (latestPrice.compareTo(stopOrder.getStopPrice()) >= 0) {
                    isTriggered = true;
                }
            }

            if (isTriggered) {
                it.remove();
                triggered.add(stopOrder);
            }
        }
        return triggered;
    }

    /**
     * Cancel an order in the book.
     */
    public synchronized BookOrder cancelOrder(String symbol, Long orderId) {
        OrderBook book = books.get(symbol.toUpperCase());
        if (book != null) {
            BookOrder removed = book.removeOrder(orderId);
            if (removed != null) return removed;
        }

        List<BookOrder> stopList = pendingStopLossOrders.get(symbol.toUpperCase());
        if (stopList != null) {
            for (Iterator<BookOrder> it = stopList.iterator(); it.hasNext(); ) {
                BookOrder o = it.next();
                if (o.getOrderId().equals(orderId)) {
                    it.remove();
                    return o;
                }
            }
        }
        return null;
    }

    /**
     * Modify an order in the book.
     * Price-changing modification or quantity increase loses time priority.
     */
    public synchronized BookOrder modifyOrder(String symbol, Long orderId, BigDecimal newPrice, BigDecimal newQuantity, Long newSequence) {
        OrderBook book = books.get(symbol.toUpperCase());
        if (book == null) return null;

        BookOrder resting = book.getOrder(orderId);
        if (resting == null) return null;

        boolean priceChanged = newPrice != null && (resting.getPrice() == null || resting.getPrice().compareTo(newPrice) != 0);
        boolean quantityIncreased = newQuantity != null && newQuantity.compareTo(resting.getQuantity()) > 0;

        // Remove from sorted queue
        book.removeOrder(orderId);

        if (newPrice != null) resting.setPrice(newPrice);
        if (newQuantity != null) resting.setQuantity(newQuantity);

        // Loss of time priority
        if (priceChanged || quantityIncreased) {
            resting.setSequenceNumber(newSequence);
        }

        // Re-insert with new priority
        book.addOrder(resting);
        return resting;
    }

    public OrderBookSnapshot getDepth(String symbol, int depth) {
        return getOrCreateBook(symbol).getSnapshot(depth);
    }
}
