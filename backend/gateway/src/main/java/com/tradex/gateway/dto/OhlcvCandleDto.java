package com.tradex.gateway.dto;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

/**
 * DTO for a single OHLCV candle.
 * {@code timeframe} values: "1M" (1-minute), "5M", "1H", "4H", "1D".
 * {@code bucketStart} is the inclusive start of the candle window (UTC).
 *
 * IMPORTANT: Daily candles (1D) contain session OHLCV values.
 *             Sub-daily candles (1M, 1H) contain intraday OHLCV values.
 *             These must NEVER be mixed.
 *
 * dataSource: "SIMULATED" or "LIVE".
 */
public record OhlcvCandleDto(
        String symbol,
        String timeframe,
        ZonedDateTime bucketStart,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        Long volume,
        String dataSource
) {}
