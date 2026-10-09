package com.tradex.gateway.marketdata;

import com.tradex.common.entity.MarketCandle;
import com.tradex.common.entity.MarketPrice;
import com.tradex.common.repository.MarketCandleRepository;
import com.tradex.common.repository.MarketPriceRepository;
import com.tradex.gateway.entity.Stock;
import com.tradex.gateway.repository.StockRepository;
import com.tradex.gateway.websocket.MarketWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Background market data engine — the ONLY place where prices are mutated.
 *
 * Responsibilities:
 * - Evolves simulated prices every 5 seconds via {@link SimulatedMarketDataProvider}.
 * - Persists updated stock rows (stocks table) and price snapshots (market_prices table).
 * - Builds and upserts 1-minute and 1-hour OHLCV candles in market_candles table.
 * - Publishes price updates to connected WebSocket subscribers.
 * - Resets the daily session (open price, high, low, previous_close) at market open.
 *
 * GET request handlers MUST read from the database — they must not call this engine or
 * generate prices inline.
 */
@Component
public class MarketDataEngine {

    private static final Logger log = LoggerFactory.getLogger(MarketDataEngine.class);

    private final StockRepository stockRepository;
    private final MarketPriceRepository marketPriceRepository;
    private final MarketCandleRepository marketCandleRepository;
    private final SimulatedMarketDataProvider simulatedProvider;
    private final MarketWebSocketHandler webSocketHandler;
    private final org.springframework.beans.factory.ObjectProvider<com.tradex.gateway.service.TradingService> tradingServiceProvider;

    // Track whether a new session was opened today
    private ZonedDateTime lastSessionReset = null;

    public MarketDataEngine(StockRepository stockRepository,
                            MarketPriceRepository marketPriceRepository,
                            MarketCandleRepository marketCandleRepository,
                            SimulatedMarketDataProvider simulatedProvider,
                            MarketWebSocketHandler webSocketHandler,
                            org.springframework.beans.factory.ObjectProvider<com.tradex.gateway.service.TradingService> tradingServiceProvider) {
        this.stockRepository = stockRepository;
        this.marketPriceRepository = marketPriceRepository;
        this.marketCandleRepository = marketCandleRepository;
        this.simulatedProvider = simulatedProvider;
        this.webSocketHandler = webSocketHandler;
        this.tradingServiceProvider = tradingServiceProvider;
    }

    /**
     * Main price tick — runs every 5 seconds.
     * Evolves all stock prices, persists snapshots, upserts 1M candles.
     */
    @Scheduled(fixedDelayString = "${tradex.market.tick-interval-ms:5000}")
    @Transactional
    public void tickPrices() {
        try {
            maybeResetDailySession();

            List<Stock> stocks = stockRepository.findAll();
            if (stocks.isEmpty()) {
                return;
            }

            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/New_York"));

            for (Stock stock : stocks) {
                Stock updated = simulatedProvider.fetchLatestPrice(stock.getSymbol(), stock);
                if (updated == null) continue;

                stockRepository.save(updated);

                // Persist price snapshot
                BigDecimal changeAmount = updated.getCurrentPrice()
                        .subtract(updated.getPreviousClose())
                        .setScale(4, RoundingMode.HALF_UP);
                BigDecimal changePercent = updated.getPreviousClose().compareTo(BigDecimal.ZERO) == 0
                        ? BigDecimal.ZERO
                        : changeAmount.divide(updated.getPreviousClose(), 6, RoundingMode.HALF_UP)
                              .multiply(BigDecimal.valueOf(100))
                              .setScale(4, RoundingMode.HALF_UP);

                MarketPrice snapshot = new MarketPrice();
                snapshot.setStockId(updated.getId());
                snapshot.setSymbol(updated.getSymbol());
                snapshot.setPrice(updated.getCurrentPrice());
                snapshot.setChangeAmount(changeAmount);
                snapshot.setChangePercent(changePercent);
                snapshot.setDayHigh(updated.getHighPrice());
                snapshot.setDayLow(updated.getLowPrice());
                snapshot.setVolume(updated.getVolume());
                snapshot.setTimestamp(now);
                snapshot.setDataSource(updated.getDataSource() != null ? updated.getDataSource() : "SIMULATED");
                marketPriceRepository.save(snapshot);

                // Upsert 1-minute candle
                upsertCandle(updated, "1M", now.truncatedTo(ChronoUnit.MINUTES), now);

                // Upsert 1-hour candle
                ZonedDateTime hourBucket = now.truncatedTo(ChronoUnit.HOURS);
                upsertCandle(updated, "1H", hourBucket, now);

                // Trigger eligible stop-loss orders
                final Stock finalUpdated = updated;
                tradingServiceProvider.ifAvailable(ts -> ts.processStopLossTriggers(finalUpdated.getSymbol(), finalUpdated.getCurrentPrice()));
            }

            // Broadcast to WebSocket subscribers (use refreshed list from DB to ensure consistent state)
            List<Stock> refreshed = stockRepository.findAll();
            webSocketHandler.broadcastPriceUpdates(refreshed);
            log.debug("Market tick: updated {} stocks at {}", stocks.size(), ZonedDateTime.now());

        } catch (Exception e) {
            log.error("Error during market price tick", e);
        }
    }

    /**
     * Daily candle close — runs at NYSE close (21:00 UTC = 16:00 ET) every weekday.
     * Finalises the 1D candle for all stocks.
     */
    @Scheduled(cron = "${tradex.market.daily-close-cron:0 0 21 * * MON-FRI}", zone = "UTC")
    @Transactional
    public void closeDailyCandles() {
        log.info("Closing daily candles (market close)");
        try {
            List<Stock> stocks = stockRepository.findAll();
            ZonedDateTime today = ZonedDateTime.now(ZoneId.of("America/New_York"))
                    .truncatedTo(ChronoUnit.DAYS);

            for (Stock stock : stocks) {
                upsertOrFinaliseCandle(stock, "1D", today, stock.getCurrentPrice());
                // After close, set previousClose = current close for next session
                stock.setPreviousClose(stock.getCurrentPrice());
                stockRepository.save(stock);
            }
        } catch (Exception e) {
            log.error("Error closing daily candles", e);
        }
    }

    /**
     * Daily session reset — runs at NYSE open (14:30 UTC = 09:30 ET) every weekday.
     * Resets open/high/low for the new session.
     */
    @Scheduled(cron = "${tradex.market.daily-open-cron:30 30 14 * * MON-FRI}", zone = "UTC")
    @Transactional
    public void openDailySession() {
        log.info("Opening new daily session (market open)");
        try {
            List<Stock> stocks = stockRepository.findAll();
            ZonedDateTime now = ZonedDateTime.now();
            for (Stock stock : stocks) {
                BigDecimal openPrice = stock.getCurrentPrice();
                stock.setOpenPrice(openPrice);
                stock.setHighPrice(openPrice);
                stock.setLowPrice(openPrice);
                stock.setVolume(0L);
                stock.setUpdatedAt(now);
                stockRepository.save(stock);
            }
            lastSessionReset = now;
        } catch (Exception e) {
            log.error("Error opening daily session", e);
        }
    }

    /**
     * Upsert a candle for the current bucket.
     * If the candle exists, update high/low/close/volume. Otherwise create a new one.
     */
    private void upsertCandle(Stock stock, String timeframe, ZonedDateTime bucketStart, ZonedDateTime now) {
        marketCandleRepository.findByStockIdAndTimeframeAndBucketStart(
                stock.getId(), timeframe, bucketStart)
        .ifPresentOrElse(
            candle -> {
                // Update existing candle: high, low, close, volume
                BigDecimal price = stock.getCurrentPrice();
                if (price.compareTo(candle.getHigh()) > 0) candle.setHigh(price);
                if (price.compareTo(candle.getLow()) < 0) candle.setLow(price);
                candle.setClose(price);
                candle.setVolume(stock.getVolume());
                marketCandleRepository.save(candle);
            },
            () -> {
                // Create new candle
                MarketCandle candle = new MarketCandle();
                candle.setStockId(stock.getId());
                candle.setSymbol(stock.getSymbol());
                candle.setTimeframe(timeframe);
                candle.setBucketStart(bucketStart);
                candle.setDataSource(stock.getDataSource() != null ? stock.getDataSource() : "SIMULATED");
                BigDecimal price = stock.getCurrentPrice();
                candle.setOpen(price);
                candle.setHigh(price);
                candle.setLow(price);
                candle.setClose(price);
                candle.setVolume(stock.getVolume());
                marketCandleRepository.save(candle);
            }
        );
    }

    private void upsertOrFinaliseCandle(Stock stock, String timeframe, ZonedDateTime bucketStart, BigDecimal closePrice) {
        marketCandleRepository.findByStockIdAndTimeframeAndBucketStart(
                stock.getId(), timeframe, bucketStart)
        .ifPresentOrElse(
            candle -> {
                candle.setClose(closePrice);
                candle.setVolume(stock.getVolume());
                marketCandleRepository.save(candle);
            },
            () -> upsertCandle(stock, timeframe, bucketStart, ZonedDateTime.now())
        );
    }

    /**
     * Resets daily session if it hasn't been reset today and we are in simulated mode
     * (for offline development where the cron won't fire).
     */
    private void maybeResetDailySession() {
        ZonedDateTime today = ZonedDateTime.now(ZoneId.of("America/New_York")).truncatedTo(ChronoUnit.DAYS);
        if (lastSessionReset == null || lastSessionReset.truncatedTo(ChronoUnit.DAYS).isBefore(today)) {
            // Only trigger the first time (subsequent resets are handled by cron)
            if (lastSessionReset == null) {
                lastSessionReset = ZonedDateTime.now();
                log.info("Simulated mode: initialised session reset timestamp");
            }
        }
    }
}
