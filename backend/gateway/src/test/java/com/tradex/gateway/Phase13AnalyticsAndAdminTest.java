package com.tradex.gateway;

import com.tradex.common.rmi.dto.TradingAnalyticsReportDto;
import com.tradex.gateway.client.NodeRmiClientService;
import com.tradex.gateway.entity.*;
import com.tradex.gateway.repository.OrderRepository;
import com.tradex.gateway.repository.StockRepository;
import com.tradex.gateway.repository.TradeRepository;
import com.tradex.gateway.service.TradingAnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class Phase13AnalyticsAndAdminTest {

    private TradeRepository tradeRepository;
    private OrderRepository orderRepository;
    private StockRepository stockRepository;
    private TradingAnalyticsService analyticsService;
    private NodeRmiClientService rmiClientService;

    @BeforeEach
    void setUp() {
        tradeRepository = mock(TradeRepository.class);
        orderRepository = mock(OrderRepository.class);
        stockRepository = mock(StockRepository.class);
        analyticsService = new TradingAnalyticsService(tradeRepository, orderRepository, stockRepository);

        rmiClientService = new NodeRmiClientService(new RestTemplate());
        rmiClientService.setCurrentActiveLeader("node3");
        rmiClientService.setCurrentLeaderEpoch(3);
        rmiClientService.setTradingState("AVAILABLE");
    }

    // ==========================================
    // 1. Persisted Trading Volume & Trades Metrics
    // ==========================================

    @Test
    void testTradingVolumeAndCountCalculationFromPersistedRecords() {
        Stock s1 = new Stock();
        s1.setSymbol("AAPL");
        Trade t1 = new Trade();
        t1.setTradeId("TRD-1");
        t1.setStock(s1);
        t1.setExecutionPrice(new BigDecimal("150.00"));
        t1.setQuantity(new BigDecimal("10"));

        Stock s2 = new Stock();
        s2.setSymbol("TSLA");
        Trade t2 = new Trade();
        t2.setTradeId("TRD-2");
        t2.setStock(s2);
        t2.setExecutionPrice(new BigDecimal("200.00"));
        t2.setQuantity(new BigDecimal("5"));

        when(tradeRepository.findAll()).thenReturn(List.of(t1, t2));
        when(orderRepository.findAll()).thenReturn(List.of());
        when(stockRepository.findAll()).thenReturn(List.of());

        TradingAnalyticsReportDto report = analyticsService.getTradingAnalytics();

        assertNotNull(report);
        assertEquals(2L, report.getTotalTradeCount());
        // $150*10 + $200*5 = 1500 + 1000 = 2500.00
        assertEquals(new BigDecimal("2500.00"), report.getTotalTradingVolume());
    }

    // ==========================================
    // 2. Order Fill, Cancellation and Rejection Rates
    // ==========================================

    @Test
    void testOrderFillCancellationAndRejectionRates() {
        Order o1 = new Order();
        o1.setId(1L);
        o1.setStatus("FILLED");
        o1.setSide("BUY");
        o1.setPrice(new BigDecimal("100"));
        o1.setQuantity(new BigDecimal("2"));

        Order o2 = new Order();
        o2.setId(2L);
        o2.setStatus("CANCELLED");
        o2.setSide("SELL");
        o2.setPrice(new BigDecimal("100"));
        o2.setQuantity(new BigDecimal("1"));

        Order o3 = new Order();
        o3.setId(3L);
        o3.setStatus("REJECTED");
        o3.setSide("BUY");
        o3.setPrice(new BigDecimal("100"));
        o3.setQuantity(new BigDecimal("1"));

        Order o4 = new Order();
        o4.setId(4L);
        o4.setStatus("FILLED");
        o4.setSide("BUY");
        o4.setPrice(new BigDecimal("100"));
        o4.setQuantity(new BigDecimal("1"));

        when(tradeRepository.findAll()).thenReturn(List.of());
        when(orderRepository.findAll()).thenReturn(List.of(o1, o2, o3, o4));
        when(stockRepository.findAll()).thenReturn(List.of());

        TradingAnalyticsReportDto report = analyticsService.getTradingAnalytics();

        assertEquals(4L, report.getTotalOrderCount());
        assertEquals(2L, report.getFilledOrderCount());
        assertEquals(1L, report.getCancelledOrderCount());
        assertEquals(1L, report.getRejectedOrderCount());
        assertEquals(50.0, report.getFillRatePercent(), 0.01);
        assertEquals(25.0, report.getCancellationRatePercent(), 0.01);
        assertEquals(25.0, report.getRejectionRatePercent(), 0.01);
    }

    // ==========================================
    // 3. Most Active Instruments by Volume
    // ==========================================

    @Test
    void testMostActiveInstrumentsOrdering() {
        Stock s1 = new Stock();
        s1.setSymbol("MSFT");
        Trade t1 = new Trade();
        t1.setStock(s1);
        t1.setExecutionPrice(new BigDecimal("300"));
        t1.setQuantity(new BigDecimal("20")); // $6000

        Stock s2 = new Stock();
        s2.setSymbol("NVDA");
        Trade t2 = new Trade();
        t2.setStock(s2);
        t2.setExecutionPrice(new BigDecimal("500"));
        t2.setQuantity(new BigDecimal("20")); // $10000

        when(tradeRepository.findAll()).thenReturn(List.of(t1, t2));
        when(orderRepository.findAll()).thenReturn(List.of());
        when(stockRepository.findAll()).thenReturn(List.of());

        TradingAnalyticsReportDto report = analyticsService.getTradingAnalytics();

        assertNotNull(report.getMostActiveInstruments());
        assertEquals(2, report.getMostActiveInstruments().size());
        assertEquals("NVDA", report.getMostActiveInstruments().get(0).get("symbol"));
        assertEquals("MSFT", report.getMostActiveInstruments().get(1).get("symbol"));
    }

    // ==========================================
    // 4. Market Movers (Gainers and Losers)
    // ==========================================

    @Test
    void testMarketMoversGainersAndLosers() {
        Stock gainer = new Stock();
        gainer.setSymbol("ABC");
        gainer.setCurrentPrice(new BigDecimal("120.00"));
        gainer.setPreviousClose(new BigDecimal("100.00")); // +20%

        Stock loser = new Stock();
        loser.setSymbol("XYZ");
        loser.setCurrentPrice(new BigDecimal("80.00"));
        loser.setPreviousClose(new BigDecimal("100.00")); // -20%

        when(tradeRepository.findAll()).thenReturn(List.of());
        when(orderRepository.findAll()).thenReturn(List.of());
        when(stockRepository.findAll()).thenReturn(List.of(gainer, loser));

        TradingAnalyticsReportDto report = analyticsService.getTradingAnalytics();

        assertFalse(report.getTopMarketGainers().isEmpty());
        assertEquals("ABC", report.getTopMarketGainers().get(0).get("symbol"));
        assertFalse(report.getTopMarketLosers().isEmpty());
        assertEquals("XYZ", report.getTopMarketLosers().get(0).get("symbol"));
    }

    // ==========================================
    // 5. Emergency Administrative Circuit Breaker
    // ==========================================

    @Test
    void testAdminEmergencyCircuitBreakerSafeguard() {
        assertEquals("AVAILABLE", rmiClientService.getTradingState());

        // Admin trips circuit breaker
        rmiClientService.setTradingState("SUSPENDED");
        assertEquals("SUSPENDED", rmiClientService.getTradingState());

        // Admin clears lock
        rmiClientService.setTradingState("AVAILABLE");
        assertEquals("AVAILABLE", rmiClientService.getTradingState());
    }
}
