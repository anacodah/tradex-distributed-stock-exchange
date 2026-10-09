package com.tradex.gateway.service;

import com.tradex.common.rmi.dto.TradingAnalyticsReportDto;
import com.tradex.gateway.entity.Order;
import com.tradex.gateway.entity.Stock;
import com.tradex.gateway.entity.Trade;
import com.tradex.gateway.repository.OrderRepository;
import com.tradex.gateway.repository.StockRepository;
import com.tradex.gateway.repository.TradeRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TradingAnalyticsService {

    private final TradeRepository tradeRepository;
    private final OrderRepository orderRepository;
    private final StockRepository stockRepository;

    public TradingAnalyticsService(TradeRepository tradeRepository,
                                   OrderRepository orderRepository,
                                   StockRepository stockRepository) {
        this.tradeRepository = tradeRepository;
        this.orderRepository = orderRepository;
        this.stockRepository = stockRepository;
    }

    /**
     * Compute comprehensive trading analytics strictly from persisted records.
     */
    public TradingAnalyticsReportDto getTradingAnalytics() {
        TradingAnalyticsReportDto dto = new TradingAnalyticsReportDto();
        dto.setTimestamp(System.currentTimeMillis());

        List<Trade> allTrades = tradeRepository.findAll();
        List<Order> allOrders = orderRepository.findAll();
        List<Stock> allStocks = stockRepository.findAll();

        // 1. Trades & Volume Metrics
        BigDecimal totalVolume = BigDecimal.ZERO;
        Map<String, BigDecimal> symbolVolumes = new HashMap<>();
        Map<String, Long> symbolTradeCounts = new HashMap<>();

        for (Trade trade : allTrades) {
            BigDecimal tradeVal = (trade.getExecutionPrice() != null && trade.getQuantity() != null)
                    ? trade.getExecutionPrice().multiply(trade.getQuantity())
                    : (trade.getTotalValue() != null ? trade.getTotalValue() : BigDecimal.ZERO);
            totalVolume = totalVolume.add(tradeVal);

            String sym = trade.getStock() != null ? trade.getStock().getSymbol() : null;
            if (sym != null) {
                symbolVolumes.put(sym, symbolVolumes.getOrDefault(sym, BigDecimal.ZERO).add(tradeVal));
                symbolTradeCounts.put(sym, symbolTradeCounts.getOrDefault(sym, 0L) + 1L);
            }
        }
        dto.setTotalTradingVolume(totalVolume.setScale(2, RoundingMode.HALF_UP));
        dto.setTotalTradeCount(allTrades.size());

        // 2. Orders & Fill/Cancellation/Rejection Rates
        long totalOrders = allOrders.size();
        long filled = 0;
        long cancelled = 0;
        long rejected = 0;
        BigDecimal buyVol = BigDecimal.ZERO;
        BigDecimal sellVol = BigDecimal.ZERO;

        for (Order o : allOrders) {
            if ("FILLED".equalsIgnoreCase(o.getStatus())) filled++;
            else if ("CANCELLED".equalsIgnoreCase(o.getStatus())) cancelled++;
            else if ("REJECTED".equalsIgnoreCase(o.getStatus())) rejected++;

            BigDecimal orderVal = (o.getPrice() != null && o.getQuantity() != null)
                    ? o.getPrice().multiply(o.getQuantity())
                    : BigDecimal.ZERO;

            if (o.getSide() != null && "BUY".equalsIgnoreCase(o.getSide())) {
                buyVol = buyVol.add(orderVal);
            } else if (o.getSide() != null && "SELL".equalsIgnoreCase(o.getSide())) {
                sellVol = sellVol.add(orderVal);
            }
        }

        dto.setTotalOrderCount(totalOrders);
        dto.setFilledOrderCount(filled);
        dto.setCancelledOrderCount(cancelled);
        dto.setRejectedOrderCount(rejected);
        dto.setFillRatePercent(totalOrders > 0 ? ((double) filled / totalOrders) * 100.0 : 0.0);
        dto.setCancellationRatePercent(totalOrders > 0 ? ((double) cancelled / totalOrders) * 100.0 : 0.0);
        dto.setRejectionRatePercent(totalOrders > 0 ? ((double) rejected / totalOrders) * 100.0 : 0.0);

        dto.setTotalBuyVolume(buyVol.setScale(2, RoundingMode.HALF_UP));
        dto.setTotalSellVolume(sellVol.setScale(2, RoundingMode.HALF_UP));
        BigDecimal sumSides = buyVol.add(sellVol);
        if (sumSides.compareTo(BigDecimal.ZERO) > 0) {
            dto.setBuyRatioPercent(buyVol.divide(sumSides, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue());
            dto.setSellRatioPercent(sellVol.divide(sumSides, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue());
        } else {
            dto.setBuyRatioPercent(50.0);
            dto.setSellRatioPercent(50.0);
        }

        // 3. Most Active Instruments
        List<Map<String, Object>> activeList = symbolVolumes.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("symbol", e.getKey());
                    m.put("volume", e.getValue().setScale(2, RoundingMode.HALF_UP));
                    m.put("tradeCount", symbolTradeCounts.getOrDefault(e.getKey(), 0L));
                    return m;
                })
                .collect(Collectors.toList());
        dto.setMostActiveInstruments(activeList);

        // 4. Market Movers (Gainers and Losers) based on current database price vs previous close
        List<Stock> validStocks = allStocks.stream()
                .filter(s -> s.getCurrentPrice() != null && s.getPreviousClose() != null && s.getPreviousClose().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());

        List<Map<String, Object>> gainers = validStocks.stream()
                .sorted((a, b) -> {
                    BigDecimal changeA = a.getCurrentPrice().subtract(a.getPreviousClose()).divide(a.getPreviousClose(), 4, RoundingMode.HALF_UP);
                    BigDecimal changeB = b.getCurrentPrice().subtract(b.getPreviousClose()).divide(b.getPreviousClose(), 4, RoundingMode.HALF_UP);
                    return changeB.compareTo(changeA);
                })
                .limit(5)
                .map(s -> {
                    BigDecimal pct = s.getCurrentPrice().subtract(s.getPreviousClose()).divide(s.getPreviousClose(), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
                    return Map.of("symbol", (Object) s.getSymbol(), "price", s.getCurrentPrice(), "changePercent", pct.setScale(2, RoundingMode.HALF_UP));
                })
                .collect(Collectors.toList());
        dto.setTopMarketGainers(gainers);

        List<Map<String, Object>> losers = validStocks.stream()
                .sorted((a, b) -> {
                    BigDecimal changeA = a.getCurrentPrice().subtract(a.getPreviousClose()).divide(a.getPreviousClose(), 4, RoundingMode.HALF_UP);
                    BigDecimal changeB = b.getCurrentPrice().subtract(b.getPreviousClose()).divide(b.getPreviousClose(), 4, RoundingMode.HALF_UP);
                    return changeA.compareTo(changeB);
                })
                .limit(5)
                .map(s -> {
                    BigDecimal pct = s.getCurrentPrice().subtract(s.getPreviousClose()).divide(s.getPreviousClose(), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
                    return Map.of("symbol", (Object) s.getSymbol(), "price", s.getCurrentPrice(), "changePercent", pct.setScale(2, RoundingMode.HALF_UP));
                })
                .collect(Collectors.toList());
        dto.setTopMarketLosers(losers);

        return dto;
    }
}
