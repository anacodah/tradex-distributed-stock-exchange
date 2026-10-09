package com.tradex.gateway.marketdata;

import com.tradex.gateway.entity.Stock;
import java.util.List;

/**
 * Provider abstraction for market data.
 * Implementations must be clearly identified by their {@link #getSourceLabel()} value
 * so that consumers can display data provenance to users.
 */
public interface MarketDataProvider {

    /**
     * Human-readable label identifying the data source.
     * Examples: "SIMULATED", "ALPHA_VANTAGE", "TWELVE_DATA"
     */
    String getSourceLabel();

    /**
     * Returns true if this provider is currently available and configured.
     */
    boolean isAvailable();

    /**
     * Fetch the latest price tick for a single symbol.
     * Must never return fabricated random prices inline; all randomness belongs in the
     * background simulation engine, not in GET request handlers.
     *
     * @param symbol  Ticker symbol (uppercase)
     * @param current The current persisted stock state (used for fallback & delta calculation)
     * @return Updated stock with refreshed prices, or {@code current} if provider is unavailable
     */
    Stock fetchLatestPrice(String symbol, Stock current);

    /**
     * Bulk-fetch latest prices for all given symbols.
     * Implementations may batch-request from external APIs.
     */
    List<Stock> fetchAllPrices(List<Stock> stocks);
}
