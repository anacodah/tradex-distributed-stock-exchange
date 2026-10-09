package com.tradex.gateway.dto;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

public class PortfolioSummaryDto {

    private BigDecimal totalPortfolioValue;
    private BigDecimal totalMarketValue;
    private BigDecimal totalCostBasis;
    private BigDecimal availableCash;
    private BigDecimal reservedCash;
    private BigDecimal totalCash;
    private BigDecimal totalRealizedPnl;
    private BigDecimal totalUnrealizedPnl;
    private BigDecimal totalUnrealizedPnlPct;
    private BigDecimal totalDailyPnl;
    private BigDecimal totalDailyPnlPct;
    private String costBasisMethod;
    private ZonedDateTime asOf;
    private List<HoldingDto> holdings;
    private List<Map<String, Object>> allocations;

    public BigDecimal getTotalPortfolioValue() { return totalPortfolioValue; }
    public void setTotalPortfolioValue(BigDecimal totalPortfolioValue) { this.totalPortfolioValue = totalPortfolioValue; }

    public BigDecimal getTotalMarketValue() { return totalMarketValue; }
    public void setTotalMarketValue(BigDecimal totalMarketValue) { this.totalMarketValue = totalMarketValue; }

    public BigDecimal getTotalCostBasis() { return totalCostBasis; }
    public void setTotalCostBasis(BigDecimal totalCostBasis) { this.totalCostBasis = totalCostBasis; }

    public BigDecimal getAvailableCash() { return availableCash; }
    public void setAvailableCash(BigDecimal availableCash) { this.availableCash = availableCash; }

    public BigDecimal getReservedCash() { return reservedCash; }
    public void setReservedCash(BigDecimal reservedCash) { this.reservedCash = reservedCash; }

    public BigDecimal getTotalCash() { return totalCash; }
    public void setTotalCash(BigDecimal totalCash) { this.totalCash = totalCash; }

    public BigDecimal getTotalRealizedPnl() { return totalRealizedPnl; }
    public void setTotalRealizedPnl(BigDecimal totalRealizedPnl) { this.totalRealizedPnl = totalRealizedPnl; }

    public BigDecimal getTotalUnrealizedPnl() { return totalUnrealizedPnl; }
    public void setTotalUnrealizedPnl(BigDecimal totalUnrealizedPnl) { this.totalUnrealizedPnl = totalUnrealizedPnl; }

    public BigDecimal getTotalUnrealizedPnlPct() { return totalUnrealizedPnlPct; }
    public void setTotalUnrealizedPnlPct(BigDecimal totalUnrealizedPnlPct) { this.totalUnrealizedPnlPct = totalUnrealizedPnlPct; }

    public BigDecimal getTotalDailyPnl() { return totalDailyPnl; }
    public void setTotalDailyPnl(BigDecimal totalDailyPnl) { this.totalDailyPnl = totalDailyPnl; }

    public BigDecimal getTotalDailyPnlPct() { return totalDailyPnlPct; }
    public void setTotalDailyPnlPct(BigDecimal totalDailyPnlPct) { this.totalDailyPnlPct = totalDailyPnlPct; }

    public String getCostBasisMethod() { return costBasisMethod; }
    public void setCostBasisMethod(String costBasisMethod) { this.costBasisMethod = costBasisMethod; }

    public ZonedDateTime getAsOf() { return asOf; }
    public void setAsOf(ZonedDateTime asOf) { this.asOf = asOf; }

    public List<HoldingDto> getHoldings() { return holdings; }
    public void setHoldings(List<HoldingDto> holdings) { this.holdings = holdings; }

    public List<Map<String, Object>> getAllocations() { return allocations; }
    public void setAllocations(List<Map<String, Object>> allocations) { this.allocations = allocations; }
}
