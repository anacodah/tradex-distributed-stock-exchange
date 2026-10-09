package com.tradex.gateway.dto;

import java.math.BigDecimal;

/**
 * DTO for company/instrument metadata.
 * Does NOT contain any price data — use {@link PriceSnapshotDto} for prices.
 * Satisfies Phase-3 requirement: company metadata stored separately from time-series data.
 */
public record CompanyDto(
        String symbol,
        String name,
        String sector,
        String industry,
        String description,
        String website,
        String country,
        String exchange,
        String currency,
        BigDecimal marketCap,
        Integer employees
) {}
