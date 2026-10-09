package com.tradex.gateway;

import com.tradex.common.entity.Company;
import com.tradex.common.entity.MarketCandle;
import com.tradex.common.repository.CompanyRepository;
import com.tradex.common.repository.MarketCandleRepository;
import com.tradex.common.repository.MarketPriceRepository;
import com.tradex.gateway.dto.CompanyDto;
import com.tradex.gateway.dto.MarketStatsDto;
import com.tradex.gateway.dto.OhlcvCandleDto;
import com.tradex.gateway.dto.PriceSnapshotDto;
import com.tradex.gateway.entity.Stock;
import com.tradex.gateway.marketdata.SimulatedMarketDataProvider;
import com.tradex.gateway.repository.StockRepository;
import com.tradex.gateway.service.MarketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class Phase3MarketDataTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private MarketCandleRepository marketCandleRepository;

    @Mock
    private MarketPriceRepository marketPriceRepository;

    @InjectMocks
    private MarketService marketService;

    private SimulatedMarketDataProvider simulatedProvider;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        simulatedProvider = new SimulatedMarketDataProvider();
    }

    @Test
    void testPriceSnapshotCalculation() {
        Stock stock = new Stock();
        stock.setId(10L);
        stock.setSymbol("AAPL");
        stock.setCompanyName("Apple Inc.");
        stock.setCurrentPrice(new BigDecimal("150.00"));
        stock.setPreviousClose(new BigDecimal("145.00"));
        stock.setOpenPrice(new BigDecimal("146.00"));
        stock.setHighPrice(new BigDecimal("152.00"));
        stock.setLowPrice(new BigDecimal("145.50"));
        stock.setVolume(1000000L);
        stock.setDataSource("SIMULATED");
        stock.setUpdatedAt(ZonedDateTime.now());

        when(stockRepository.findBySymbol("AAPL")).thenReturn(Optional.of(stock));

        PriceSnapshotDto snapshot = marketService.getSnapshot("AAPL");

        assertNotNull(snapshot);
        assertEquals("AAPL", snapshot.getSymbol());
        assertEquals(new BigDecimal("150.00"), snapshot.getCurrentPrice());
        assertEquals(new BigDecimal("5.0000"), snapshot.getChangeAmount());
        assertEquals("SIMULATED", snapshot.getDataSource());
    }

    @Test
    void testMarketStatsAggregations() {
        Stock gainer = new Stock();
        gainer.setId(1L);
        gainer.setSymbol("NVDA");
        gainer.setCurrentPrice(new BigDecimal("200.00"));
        gainer.setPreviousClose(new BigDecimal("190.00"));
        gainer.setVolume(500000L);

        Stock loser = new Stock();
        loser.setId(2L);
        loser.setSymbol("INTC");
        loser.setCurrentPrice(new BigDecimal("30.00"));
        loser.setPreviousClose(new BigDecimal("35.00"));
        loser.setVolume(300000L);

        when(stockRepository.findAll()).thenReturn(List.of(gainer, loser));
        when(companyRepository.findAll()).thenReturn(List.of());

        MarketStatsDto stats = marketService.getMarketStats();

        assertNotNull(stats);
        assertEquals(2, stats.getTotalTradedSecurities());
        assertEquals(1, stats.getAdvancers());
        assertEquals(1, stats.getDecliners());
        assertEquals(0, stats.getUnchanged());
        assertEquals(800000L, stats.getTotalVolume());
    }

    @Test
    void testCompanySearchAndDtoMapping() {
        Company apple = new Company();
        apple.setId(1L);
        apple.setSymbol("AAPL");
        apple.setName("Apple Inc.");
        apple.setSector("Technology");
        apple.setIndustry("Consumer Electronics");
        apple.setDescription("Consumer electronics company");
        apple.setExchange("NASDAQ");
        apple.setCurrency("USD");

        when(companyRepository.searchCompanies("Apple")).thenReturn(List.of(apple));

        List<CompanyDto> results = marketService.searchCompanies("Apple");
        assertEquals(1, results.size());
        assertEquals("AAPL", results.get(0).getSymbol());
        assertEquals("Technology", results.get(0).getSector());
    }

    @Test
    void testSimulatedMarketDataProviderDeterministicPerturbation() {
        Stock stock = new Stock();
        stock.setSymbol("MSFT");
        stock.setCurrentPrice(new BigDecimal("300.00"));
        stock.setHighPrice(new BigDecimal("300.00"));
        stock.setLowPrice(new BigDecimal("300.00"));
        stock.setVolume(1000L);
        stock.setPreviousClose(new BigDecimal("298.00"));

        Stock updated = simulatedProvider.fetchLatestPrice("MSFT", stock);

        assertNotNull(updated);
        assertNotNull(updated.getCurrentPrice());
        assertTrue(updated.getCurrentPrice().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(updated.getHighPrice().compareTo(updated.getLowPrice()) >= 0);
        assertEquals("SIMULATED", updated.getDataSource());
    }

    @Test
    void testCandleRetrieval() {
        Stock stock = new Stock();
        stock.setId(1L);
        stock.setSymbol("TSLA");

        MarketCandle candle = new MarketCandle();
        candle.setStockId(1L);
        candle.setSymbol("TSLA");
        candle.setTimeframe("1H");
        candle.setOpen(new BigDecimal("200.00"));
        candle.setHigh(new BigDecimal("210.00"));
        candle.setLow(new BigDecimal("195.00"));
        candle.setClose(new BigDecimal("205.00"));
        candle.setVolume(50000L);
        candle.setBucketStart(ZonedDateTime.now());
        candle.setDataSource("SIMULATED");

        when(stockRepository.findBySymbol("TSLA")).thenReturn(Optional.of(stock));
        when(marketCandleRepository.findByStockIdAndTimeframeOrderByBucketStartAsc(1L, "1H"))
                .thenReturn(List.of(candle));

        List<OhlcvCandleDto> candles = marketService.getCandles("TSLA", "1H", null, null);
        assertEquals(1, candles.size());
        assertEquals(new BigDecimal("205.00"), candles.get(0).getClose());
        assertEquals("1H", candles.get(0).getTimeframe());
    }
}
