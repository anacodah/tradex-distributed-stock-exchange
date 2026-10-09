package com.tradex.gateway;

import com.tradex.gateway.dto.HoldingDto;
import com.tradex.gateway.dto.PortfolioSummaryDto;
import com.tradex.gateway.dto.ReconciliationReportDto;
import com.tradex.gateway.entity.*;
import com.tradex.gateway.repository.*;
import com.tradex.gateway.service.PortfolioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class Phase5PortfolioServiceTest {

    @Mock
    private HoldingRepository holdingRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private TradeRepository tradeRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PortfolioService portfolioService;

    private User testUser;
    private Wallet testWallet;
    private Stock testStock;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testUser = new User();
        testUser.setId(10L);
        testUser.setUsername("trader1");

        testWallet = new Wallet();
        testWallet.setUser(testUser);
        testWallet.setBalance(new BigDecimal("10000.00"));
        testWallet.setAvailableBalance(new BigDecimal("9000.00"));
        testWallet.setReservedBalance(new BigDecimal("1000.00"));

        testStock = new Stock();
        testStock.setId(1L);
        testStock.setSymbol("AAPL");
        testStock.setCompanyName("Apple Inc.");
        testStock.setCurrentPrice(new BigDecimal("180.00"));
        testStock.setPreviousClose(new BigDecimal("175.00"));
        testStock.setPriceTimestamp(ZonedDateTime.now());

        when(userRepository.findByUsername("trader1")).thenReturn(Optional.of(testUser));
        when(walletRepository.findByUserId(10L)).thenReturn(Optional.of(testWallet));
    }

    @Test
    void testPortfolioCalculationsWeightedAverageCostAndDailyPnl() {
        // Holding of 10 shares @ $150 average cost basis
        Holding holding = new Holding();
        holding.setUser(testUser);
        holding.setStock(testStock);
        holding.setQuantity(new BigDecimal("10"));
        holding.setAveragePrice(new BigDecimal("150.00"));
        holding.setRealizedPnl(new BigDecimal("250.00"));

        when(holdingRepository.findByUserId(10L)).thenReturn(List.of(holding));

        // Pending sell order of 3 shares
        Order pendingSell = new Order();
        pendingSell.setSide("SELL");
        pendingSell.setStock(testStock);
        pendingSell.setQuantity(new BigDecimal("3"));
        pendingSell.setFilledQuantity(BigDecimal.ZERO);
        pendingSell.setStatus("OPEN");

        when(orderRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(eq(10L), any()))
                .thenReturn(List.of(pendingSell));

        PortfolioSummaryDto summary = portfolioService.getPortfolioSummary("trader1");

        assertNotNull(summary);
        assertEquals(1, summary.getHoldings().size());
        HoldingDto hDto = summary.getHoldings().get(0);

        // 1. Quantity checks (total = 10, reserved = 3, sellable = 7)
        assertEquals(new BigDecimal("10"), hDto.getTotalQuantity());
        assertEquals(new BigDecimal("3"), hDto.getReservedQuantity());
        assertEquals(new BigDecimal("7"), hDto.getSellableQuantity());

        // 2. Cost basis & Market value: 10 * 150 = 1500; 10 * 180 = 1800
        assertEquals(new BigDecimal("1500.0000"), hDto.getCostBasis());
        assertEquals(new BigDecimal("1800.0000"), hDto.getMarketValue());

        // 3. Unrealized P&L: 1800 - 1500 = +300 (+20%)
        assertEquals(new BigDecimal("300.0000"), hDto.getUnrealizedPnl());
        assertEquals(new BigDecimal("20.0000"), hDto.getUnrealizedPnlPct());

        // 4. Daily P&L vs previous close ($175): 10 * (180 - 175) = +50
        assertEquals(new BigDecimal("50.0000"), hDto.getDailyPnl());

        // 5. Asset allocation: Cash = 10000, Equities = 1800, Total = 11800
        // Stock allocation: 1800 / 11800 ~= 15.25%
        assertEquals(new BigDecimal("11800.0000"), summary.getTotalPortfolioValue());
        assertEquals("WEIGHTED_AVERAGE_COST", summary.getCostBasisMethod());
    }

    @Test
    void testLosingPositionUnrealizedPnl() {
        // Stock current price is $180, but user bought at $200
        Holding losingHolding = new Holding();
        losingHolding.setUser(testUser);
        losingHolding.setStock(testStock);
        losingHolding.setQuantity(new BigDecimal("5"));
        losingHolding.setAveragePrice(new BigDecimal("200.00")); // Cost: $1000
        losingHolding.setRealizedPnl(BigDecimal.ZERO);

        when(holdingRepository.findByUserId(10L)).thenReturn(List.of(losingHolding));
        when(orderRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(eq(10L), any()))
                .thenReturn(Collections.emptyList());

        PortfolioSummaryDto summary = portfolioService.getPortfolioSummary("trader1");
        HoldingDto hDto = summary.getHoldings().get(0);

        // Market value = 5 * 180 = 900; Cost basis = 1000; Unrealized P&L = -100 (-10%)
        assertEquals(new BigDecimal("900.0000"), hDto.getMarketValue());
        assertEquals(new BigDecimal("1000.0000"), hDto.getCostBasis());
        assertEquals(new BigDecimal("-100.0000"), hDto.getUnrealizedPnl());
        assertEquals(new BigDecimal("-10.0000"), hDto.getUnrealizedPnlPct());
    }

    @Test
    void testStalePriceDetection() {
        // Price timestamp is 10 minutes ago
        testStock.setPriceTimestamp(ZonedDateTime.now().minusMinutes(10));

        Holding holding = new Holding();
        holding.setUser(testUser);
        holding.setStock(testStock);
        holding.setQuantity(new BigDecimal("2"));
        holding.setAveragePrice(new BigDecimal("180.00"));

        when(holdingRepository.findByUserId(10L)).thenReturn(List.of(holding));
        when(orderRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(eq(10L), any()))
                .thenReturn(Collections.emptyList());

        PortfolioSummaryDto summary = portfolioService.getPortfolioSummary("trader1");
        assertTrue(summary.getHoldings().get(0).isPriceStale(), "Price older than 2 minutes should be marked stale");
    }

    @Test
    void testAuthoritativePositionReconciliationFromTrades() {
        // Simulate trade history:
        // Trade 1: BUY 10 AAPL @ $100
        // Trade 2: BUY 10 AAPL @ $120 -> WAC = (1000 + 1200) / 20 = $110
        // Trade 3: SELL 5 AAPL @ $150 -> Realized P&L = 5 * (150 - 110) = +$200; Remaining Qty = 15 @ $110
        List<Trade> trades = new ArrayList<>();

        Trade t1 = new Trade();
        t1.setUser(testUser);
        t1.setStock(testStock);
        t1.setSide("BUY");
        t1.setQuantity(new BigDecimal("10"));
        t1.setExecutionPrice(new BigDecimal("100.00"));
        t1.setExecutedAt(LocalDateTime.now().minusHours(3));
        trades.add(t1);

        Trade t2 = new Trade();
        t2.setUser(testUser);
        t2.setStock(testStock);
        t2.setSide("BUY");
        t2.setQuantity(new BigDecimal("10"));
        t2.setExecutionPrice(new BigDecimal("120.00"));
        t2.setExecutedAt(LocalDateTime.now().minusHours(2));
        trades.add(t2);

        Trade t3 = new Trade();
        t3.setUser(testUser);
        t3.setStock(testStock);
        t3.setSide("SELL");
        t3.setQuantity(new BigDecimal("5"));
        t3.setExecutionPrice(new BigDecimal("150.00"));
        t3.setExecutedAt(LocalDateTime.now().minusHours(1));
        trades.add(t3);

        when(tradeRepository.findByUserIdOrderByExecutedAtDesc(10L)).thenReturn(trades);

        // Suppose holding was out of sync (e.g. quantity was recorded as 12 instead of 15)
        Holding desyncedHolding = new Holding();
        desyncedHolding.setUser(testUser);
        desyncedHolding.setStock(testStock);
        desyncedHolding.setQuantity(new BigDecimal("12"));
        desyncedHolding.setAveragePrice(new BigDecimal("100.00"));
        desyncedHolding.setRealizedPnl(BigDecimal.ZERO);

        when(holdingRepository.findByUserIdAndStockIdForUpdate(10L, 1L))
                .thenReturn(Optional.of(desyncedHolding));

        ReconciliationReportDto report = portfolioService.reconcilePositions("trader1");

        assertNotNull(report);
        assertEquals(1, report.getPositionsRepaired());
        // Verify repaired holding attributes
        assertEquals(new BigDecimal("15"), desyncedHolding.getQuantity());
        assertEquals(new BigDecimal("110.0000"), desyncedHolding.getAveragePrice());
        assertEquals(new BigDecimal("200.0000"), desyncedHolding.getRealizedPnl());
        verify(holdingRepository, times(1)).save(desyncedHolding);
    }
}
