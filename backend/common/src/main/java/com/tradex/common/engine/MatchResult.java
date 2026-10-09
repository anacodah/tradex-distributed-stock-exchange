package com.tradex.common.engine;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class MatchResult {

    private final BookOrder takerOrder;
    private final List<TradeExecution> trades = new ArrayList<>();
    private OrderStatus finalStatus;
    private boolean restedInBook;

    public MatchResult(BookOrder takerOrder) {
        this.takerOrder = takerOrder;
    }

    public void addTrade(TradeExecution trade) {
        this.trades.add(trade);
    }

    public BookOrder getTakerOrder() {
        return takerOrder;
    }

    public List<TradeExecution> getTrades() {
        return trades;
    }

    public OrderStatus getFinalStatus() {
        return finalStatus;
    }

    public void setFinalStatus(OrderStatus finalStatus) {
        this.finalStatus = finalStatus;
    }

    public boolean isRestedInBook() {
        return restedInBook;
    }

    public void setRestedInBook(boolean restedInBook) {
        this.restedInBook = restedInBook;
    }

    public BigDecimal getTotalExecutedQuantity() {
        return trades.stream()
                .map(TradeExecution::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
