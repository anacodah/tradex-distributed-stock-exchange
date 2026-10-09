package com.tradex.gateway.dto;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

/**
 * DTO representing the latest price snapshot for a single instrument.
 * Clearly separates:
 * - Session OHLC (open/high/low from the day's session start, previousClose from prior session)
 * - Latest intraday price (current, priceTimestamp)
 * These must NEVER be conflated.
 *
 * dataSource: "SIMULATED" or "LIVE". Clients MUST display this label.
 */
public record PriceSnapshotDto(
        String symbol,
        String companyName,
        String sector,
        String exchange,
        String currency,
        String dataSource,

        // Latest intraday price — NOT the session open
        BigDecimal price,
        ZonedDateTime priceTimestamp,

        // Session OHLC (exchange-session values)
        BigDecimal sessionOpen,
        BigDecimal sessionHigh,
        BigDecimal sessionLow,
        BigDecimal previousClose,

        // Derived fields (relative to previous close)
        BigDecimal changeAmount,
        BigDecimal changePercent,

        // Cumulative session volume
        Long volume,

        BigDecimal marketCap
) {}
