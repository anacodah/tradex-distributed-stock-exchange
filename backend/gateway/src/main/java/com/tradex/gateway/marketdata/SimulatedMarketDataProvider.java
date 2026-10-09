package com.tradex.gateway.marketdata;

import com.tradex.gateway.entity.Stock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Random;

/**
 * Deterministic simulated market data provider.
 *
 * Price evolution model:
 * - Prices evolve through the background {@link MarketDataEngine} on a fixed schedule.
 * - Each tick applies a bounded Gaussian random walk with per-symbol volatility.
 * - The random walk uses a seeded Random so behaviour is reproducible per-symbol.
 * - Intraday session high/low are tracked and only updated when the new price exceeds them.
 * - This provider NEVER generates random prices on GET requests.
 * - All data is clearly labelled with source "SIMULATED".
 */
@Component
public class SimulatedMarketDataProvider implements MarketDataProvider {

    private static final Logger log = LoggerFactory.getLogger(SimulatedMarketDataProvider.class);

    /**
     * Volatility per symbol (daily sigma approximation, used to scale per-tick noise).
     * If a symbol is not listed, the default volatility of 0.0015 is used.
     */
    private static final java.util.Map<String, Double> SYMBOL_VOLATILITY = java.util.Map.of(
        "TSLA",  0.0040,
        "NVDA",  0.0030,
        "AAPL",  0.0015,
        "MSFT",  0.0012,
        "GOOGL", 0.0014,
        "AMZN",  0.0018,
        "META",  0.0020,
        "NFLX",  0.0025,
        "JPM",   0.0010,
        "V",     0.0008
    );

    private static final double DEFAULT_VOLATILITY = 0.0015;
    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);

    private final Random random = new Random(42L); // seed for reproducibility

    @Override
    public String getSourceLabel() {
        return "SIMULATED";
    }

    @Override
    public boolean isAvailable() {
        return true; // always available — it is the fallback
    }

    /**
     * Compute the next simulated price for a single stock.
     * Called by the engine only — NOT by GET request handlers.
     *
     * The new price is bounded to [prevClose * 0.70, prevClose * 1.30] to prevent
     * simulation drift producing unrealistic values over time.
     */
    @Override
    public Stock fetchLatestPrice(String symbol, Stock current) {
        if (current == null) {
            log.warn("fetchLatestPrice called with null stock for symbol {}", symbol);
            return null;
        }

        double volatility = SYMBOL_VOLATILITY.getOrDefault(symbol.toUpperCase(), DEFAULT_VOLATILITY);
        double gaussianNoise = random.nextGaussian() * volatility;

        BigDecimal prevPrice = current.getCurrentPrice();
        BigDecimal newPrice = prevPrice.multiply(
            BigDecimal.ONE.add(BigDecimal.valueOf(gaussianNoise)), MC
        ).setScale(4, RoundingMode.HALF_UP);

        // Bound price to ±30% of previous close to prevent runaway simulation
        BigDecimal prevClose = current.getPreviousClose();
        BigDecimal lowerBound = prevClose.multiply(BigDecimal.valueOf(0.70)).setScale(4, RoundingMode.HALF_UP);
        BigDecimal upperBound = prevClose.multiply(BigDecimal.valueOf(1.30)).setScale(4, RoundingMode.HALF_UP);
        if (newPrice.compareTo(lowerBound) < 0) newPrice = lowerBound;
        if (newPrice.compareTo(upperBound) > 0) newPrice = upperBound;

        current.setCurrentPrice(newPrice);
        current.setPriceTimestamp(ZonedDateTime.now());
        current.setUpdatedAt(ZonedDateTime.now());
        current.setDataSource("SIMULATED");

        // Update intraday high/low (session values — NOT mixed with current intraday price)
        if (newPrice.compareTo(current.getHighPrice()) > 0) {
            current.setHighPrice(newPrice);
        }
        if (newPrice.compareTo(current.getLowPrice()) < 0) {
            current.setLowPrice(newPrice);
        }

        // Update volume (simulate incremental trades, scaled by volatility)
        long volumeIncrement = (long) (current.getVolume() * 0.002 + random.nextInt(5000));
        current.setVolume(current.getVolume() + volumeIncrement);

        return current;
    }

    @Override
    public List<Stock> fetchAllPrices(List<Stock> stocks) {
        return stocks.stream()
                .map(s -> fetchLatestPrice(s.getSymbol(), s))
                .toList();
    }
}
