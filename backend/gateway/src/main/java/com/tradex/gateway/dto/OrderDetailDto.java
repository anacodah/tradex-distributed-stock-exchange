package com.tradex.gateway.dto;

import com.tradex.gateway.entity.Order;
import java.util.List;

public class OrderDetailDto {

    private Order order;
    private List<TradeDto> trades;

    public OrderDetailDto(Order order, List<TradeDto> trades) {
        this.order = order;
        this.trades = trades;
    }

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    public List<TradeDto> getTrades() { return trades; }
    public void setTrades(List<TradeDto> trades) { this.trades = trades; }
}
