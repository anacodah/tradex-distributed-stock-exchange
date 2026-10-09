package com.tradex.gateway.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Market-wide and sector-level summary statistics.
 * All statistics are derived from the latest persisted stock data — no live generation.
 */
public record MarketStatsDto(
        int totalInstruments,
        long advancers,
        long decliners,
        long unchanged,
        BigDecimal totalMarketCap,
        String dataSource,
        List<SectorStat> sectorStats
) {
    /**
     * Per-sector roll-up: number of instruments and average change percent.
     */
    public record SectorStat(
            String sector,
            int count,
            BigDecimal avgChangePercent
    ) {}
}
