package com.tradex.gateway.service;

import com.tradex.gateway.dto.HoldingDto;
import com.tradex.gateway.dto.PortfolioSummaryDto;
import com.tradex.gateway.dto.ReconciliationReportDto;
import com.tradex.gateway.entity.*;
import com.tradex.gateway.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Authoritative Portfolio Service.
 *
 * Implements:
 * - Weighted-Average Cost (WAC) basis calculation.
 * - Exact separation of total vs sellable vs reserved quantity.
 * - Daily P&L using previous close as the documented benchmark.
 * - Unrealized and Realized P&L tracking.
 * - Asset allocation percentages.
 * - Independent reconciliation against underlying trade history.
 */
@Service
public class PortfolioService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioService.class);

    private final HoldingRepository holdingRepository;
    private final WalletRepository walletRepository;
    private final OrderRepository orderRepository;
    private final TradeRepository tradeRepository;
    private final StockRepository stockRepository;
    private final UserRepository userRepository;

    public PortfolioService(HoldingRepository holdingRepository,
                            WalletRepository walletRepository,
                            OrderRepository orderRepository,
                            TradeRepository tradeRepository,
                            StockRepository stockRepository,
                            UserRepository userRepository) {
        this.holdingRepository = holdingRepository;
        this.walletRepository = walletRepository;
        this.orderRepository = orderRepository;
        this.tradeRepository = tradeRepository;
        this.stockRepository = stockRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PortfolioSummaryDto getPortfolioSummary(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        List<Holding> holdings = holdingRepository.findByUserId(user.getId());
        List<Order> openSellOrders = orderRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(
                user.getId(), List.of("OPEN", "PARTIALLY_FILLED", "NEW")
        ).stream().filter(o -> "SELL".equalsIgnoreCase(o.getSide())).collect(Collectors.toList());

        BigDecimal totalMarketValue = BigDecimal.ZERO;
        BigDecimal totalCostBasis = BigDecimal.ZERO;
        BigDecimal totalRealizedPnl = BigDecimal.ZERO;
        BigDecimal totalDailyPnl = BigDecimal.ZERO;

        List<HoldingDto> holdingDtos = new ArrayList<>();
        ZonedDateTime now = ZonedDateTime.now();

        for (Holding h : holdings) {
            if (h.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            Stock s = h.getStock();
            BigDecimal currentPrice = s.getCurrentPrice() != null ? s.getCurrentPrice() : h.getAveragePrice();
            BigDecimal prevClose = s.getPreviousClose() != null ? s.getPreviousClose() : currentPrice;

            // Reserved shares from pending sell orders
            BigDecimal reservedQty = openSellOrders.stream()
                    .filter(o -> o.getStock().getId().equals(s.getId()))
                    .map(Order::getRemainingQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal sellableQty = h.getQuantity().subtract(reservedQty).max(BigDecimal.ZERO);

            // Cost basis & market value
            BigDecimal marketValue = currentPrice.multiply(h.getQuantity()).setScale(4, RoundingMode.HALF_UP);
            BigDecimal costBasis = h.getAveragePrice().multiply(h.getQuantity()).setScale(4, RoundingMode.HALF_UP);

            // Unrealized P&L
            BigDecimal unrealizedPnl = marketValue.subtract(costBasis).setScale(4, RoundingMode.HALF_UP);
            BigDecimal unrealizedPct = costBasis.compareTo(BigDecimal.ZERO) > 0
                    ? unrealizedPnl.divide(costBasis, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;

            // Daily P&L benchmarked against official previous close
            BigDecimal priceDiffToday = currentPrice.subtract(prevClose);
            BigDecimal dailyPnl = h.getQuantity().multiply(priceDiffToday).setScale(4, RoundingMode.HALF_UP);
            BigDecimal dailyPnlPct = prevClose.compareTo(BigDecimal.ZERO) > 0
                    ? priceDiffToday.divide(prevClose, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;

            // Stale price check (e.g., if price timestamp is older than 2 minutes)
            boolean isStale = s.getPriceTimestamp() != null &&
                    Duration.between(s.getPriceTimestamp(), now).abs().toMinutes() > 2;

            totalMarketValue = totalMarketValue.add(marketValue);
            totalCostBasis = totalCostBasis.add(costBasis);
            totalRealizedPnl = totalRealizedPnl.add(h.getRealizedPnl() != null ? h.getRealizedPnl() : BigDecimal.ZERO);
            totalDailyPnl = totalDailyPnl.add(dailyPnl);

            HoldingDto dto = new HoldingDto();
            dto.setSymbol(s.getSymbol());
            dto.setCompanyName(s.getCompanyName());
            dto.setTotalQuantity(h.getQuantity());
            dto.setSellableQuantity(sellableQty);
            dto.setReservedQuantity(reservedQty);
            dto.setAveragePurchasePrice(h.getAveragePrice());
            dto.setCurrentPrice(currentPrice);
            dto.setPreviousClose(prevClose);
            dto.setMarketValue(marketValue);
            dto.setCostBasis(costBasis);
            dto.setRealizedPnl(h.getRealizedPnl());
            dto.setUnrealizedPnl(unrealizedPnl);
            dto.setUnrealizedPnlPct(unrealizedPct);
            dto.setDailyPnl(dailyPnl);
            dto.setDailyPnlPct(dailyPnlPct);
            dto.setPriceStale(isStale);

            holdingDtos.add(dto);
        }

        BigDecimal totalCash = wallet.getBalance();
        BigDecimal totalPortfolioValue = totalMarketValue.add(totalCash);

        // Compute asset allocation percentage for each holding & cash
        for (HoldingDto dto : holdingDtos) {
            BigDecimal alloc = totalPortfolioValue.compareTo(BigDecimal.ZERO) > 0
                    ? dto.getMarketValue().divide(totalPortfolioValue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;
            dto.setAllocationPct(alloc);
        }

        List<Map<String, Object>> allocations = new ArrayList<>();
        for (HoldingDto dto : holdingDtos) {
            allocations.add(Map.of(
                    "asset", dto.getSymbol(),
                    "value", dto.getMarketValue(),
                    "percentage", dto.getAllocationPct()
            ));
        }
        BigDecimal cashAlloc = totalPortfolioValue.compareTo(BigDecimal.ZERO) > 0
                ? totalCash.divide(totalPortfolioValue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;
        allocations.add(Map.of(
                "asset", "CASH",
                "value", totalCash,
                "percentage", cashAlloc
        ));

        BigDecimal totalUnrealizedPnl = totalMarketValue.subtract(totalCostBasis).setScale(4, RoundingMode.HALF_UP);
        BigDecimal totalUnrealizedPnlPct = totalCostBasis.compareTo(BigDecimal.ZERO) > 0
                ? totalUnrealizedPnl.divide(totalCostBasis, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        BigDecimal totalDailyPnlPct = totalMarketValue.compareTo(BigDecimal.ZERO) > 0
                ? totalDailyPnl.divide(totalMarketValue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        PortfolioSummaryDto summary = new PortfolioSummaryDto();
        summary.setTotalPortfolioValue(totalPortfolioValue);
        summary.setTotalMarketValue(totalMarketValue);
        summary.setTotalCostBasis(totalCostBasis);
        summary.setAvailableCash(wallet.getAvailableBalance());
        summary.setReservedCash(wallet.getReservedBalance());
        summary.setTotalCash(totalCash);
        summary.setTotalRealizedPnl(totalRealizedPnl);
        summary.setTotalUnrealizedPnl(totalUnrealizedPnl);
        summary.setTotalUnrealizedPnlPct(totalUnrealizedPnlPct);
        summary.setTotalDailyPnl(totalDailyPnl);
        summary.setTotalDailyPnlPct(totalDailyPnlPct);
        summary.setCostBasisMethod("WEIGHTED_AVERAGE_COST");
        summary.setAsOf(now);
        summary.setHoldings(holdingDtos);
        summary.setAllocations(allocations);

        return summary;
    }

    /**
     * Authoritative reconciliation of portfolio positions directly against executed trades.
     * Reconstructs quantities, weighted-average cost, and realized P&L.
     */
    @Transactional
    public ReconciliationReportDto reconcilePositions(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        List<Trade> allTrades = tradeRepository.findByUserIdOrderByExecutedAtDesc(user.getId());
        // Sort chronologically ascending for replay
        allTrades.sort(Comparator.comparing(Trade::getExecutedAt));

        Map<Long, PositionTracker> reconstructed = new HashMap<>();

        for (Trade t : allTrades) {
            Long stockId = t.getStock().getId();
            PositionTracker pos = reconstructed.computeIfAbsent(stockId, k -> new PositionTracker(t.getStock()));

            if ("BUY".equalsIgnoreCase(t.getSide())) {
                pos.applyBuy(t.getQuantity(), t.getExecutionPrice());
            } else if ("SELL".equalsIgnoreCase(t.getSide())) {
                pos.applySell(t.getQuantity(), t.getExecutionPrice());
            }
        }

        List<Holding> existingHoldings = holdingRepository.findByUserId(user.getId());
        int repairedCount = 0;
        List<Map<String, Object>> adjustments = new ArrayList<>();

        for (Map.Entry<Long, PositionTracker> entry : reconstructed.entrySet()) {
            Long stockId = entry.getKey();
            PositionTracker tracker = entry.getValue();

            Holding holding = holdingRepository.findByUserIdAndStockIdForUpdate(user.getId(), stockId)
                    .orElseGet(() -> {
                        Holding h = new Holding();
                        h.setUser(user);
                        h.setStock(tracker.stock);
                        h.setQuantity(BigDecimal.ZERO);
                        h.setAveragePrice(BigDecimal.ZERO);
                        h.setRealizedPnl(BigDecimal.ZERO);
                        return h;
                    });

            boolean mismatch = holding.getQuantity().compareTo(tracker.quantity) != 0 ||
                    holding.getAveragePrice().compareTo(tracker.averagePrice) != 0 ||
                    holding.getRealizedPnl().compareTo(tracker.realizedPnl) != 0;

            if (mismatch) {
                Map<String, Object> adj = new HashMap<>();
                adj.put("symbol", tracker.stock.getSymbol());
                adj.put("previousQty", holding.getQuantity());
                adj.put("reconciledQty", tracker.quantity);
                adj.put("previousAvgPrice", holding.getAveragePrice());
                adj.put("reconciledAvgPrice", tracker.averagePrice);
                adj.put("previousRealizedPnl", holding.getRealizedPnl());
                adj.put("reconciledRealizedPnl", tracker.realizedPnl);
                adjustments.add(adj);

                holding.setQuantity(tracker.quantity);
                holding.setAveragePrice(tracker.averagePrice);
                holding.setRealizedPnl(tracker.realizedPnl);
                holdingRepository.save(holding);
                repairedCount++;
            }
        }

        ReconciliationReportDto report = new ReconciliationReportDto();
        report.setReconciled(repairedCount == 0);
        report.setPositionsChecked(reconstructed.size());
        report.setPositionsRepaired(repairedCount);
        report.setAdjustments(adjustments);
        return report;
    }

    /**
     * Helper to track and reconstruct position via Weighted-Average Cost.
     */
    private static class PositionTracker {
        final Stock stock;
        BigDecimal quantity = BigDecimal.ZERO;
        BigDecimal averagePrice = BigDecimal.ZERO;
        BigDecimal realizedPnl = BigDecimal.ZERO;

        PositionTracker(Stock stock) {
            this.stock = stock;
        }

        void applyBuy(BigDecimal buyQty, BigDecimal buyPrice) {
            BigDecimal totalQty = quantity.add(buyQty);
            BigDecimal totalCost = quantity.multiply(averagePrice).add(buyQty.multiply(buyPrice));
            if (totalQty.compareTo(BigDecimal.ZERO) > 0) {
                averagePrice = totalCost.divide(totalQty, 4, RoundingMode.HALF_UP);
            }
            quantity = totalQty;
        }

        void applySell(BigDecimal sellQty, BigDecimal sellPrice) {
            BigDecimal costBasisSold = sellQty.multiply(averagePrice);
            BigDecimal proceeds = sellQty.multiply(sellPrice);
            realizedPnl = realizedPnl.add(proceeds.subtract(costBasisSold));
            quantity = quantity.subtract(sellQty).max(BigDecimal.ZERO);
            if (quantity.compareTo(BigDecimal.ZERO) == 0) {
                averagePrice = BigDecimal.ZERO;
            }
        }
    }
}
