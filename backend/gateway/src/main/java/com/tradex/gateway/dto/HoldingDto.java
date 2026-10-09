package com.tradex.gateway.dto;

import java.math.BigDecimal;

public class HoldingDto {

    private String symbol;
    private String companyName;
    private BigDecimal totalQuantity;
    private BigDecimal sellableQuantity;
    private BigDecimal reservedQuantity;
    private BigDecimal averagePurchasePrice;
    private BigDecimal currentPrice;
    private BigDecimal previousClose;
    private BigDecimal marketValue;
    private BigDecimal costBasis;
    private BigDecimal realizedPnl;
    private BigDecimal unrealizedPnl;
    private BigDecimal unrealizedPnlPct;
    private BigDecimal dailyPnl;
    private BigDecimal dailyPnlPct;
    private BigDecimal allocationPct;
    private boolean isPriceStale;

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public BigDecimal getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(BigDecimal totalQuantity) { this.totalQuantity = totalQuantity; }

    public BigDecimal getSellableQuantity() { return sellableQuantity; }
    public void setSellableQuantity(BigDecimal sellableQuantity) { this.sellableQuantity = sellableQuantity; }

    public BigDecimal getReservedQuantity() { return reservedQuantity; }
    public void setReservedQuantity(BigDecimal reservedQuantity) { this.reservedQuantity = reservedQuantity; }

    public BigDecimal getAveragePurchasePrice() { return averagePurchasePrice; }
    public void setAveragePurchasePrice(BigDecimal averagePurchasePrice) { this.averagePurchasePrice = averagePurchasePrice; }

    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }

    public BigDecimal getPreviousClose() { return previousClose; }
    public void setPreviousClose(BigDecimal previousClose) { this.previousClose = previousClose; }

    public BigDecimal getMarketValue() { return marketValue; }
    public void setMarketValue(BigDecimal marketValue) { this.marketValue = marketValue; }

    public BigDecimal getCostBasis() { return costBasis; }
    public void setCostBasis(BigDecimal costBasis) { this.costBasis = costBasis; }

    public BigDecimal getRealizedPnl() { return realizedPnl; }
    public void setRealizedPnl(BigDecimal realizedPnl) { this.realizedPnl = realizedPnl; }

    public BigDecimal getUnrealizedPnl() { return unrealizedPnl; }
    public void setUnrealizedPnl(BigDecimal unrealizedPnl) { this.unrealizedPnl = unrealizedPnl; }

    public BigDecimal getUnrealizedPnlPct() { return unrealizedPnlPct; }
    public void setUnrealizedPnlPct(BigDecimal unrealizedPnlPct) { this.unrealizedPnlPct = unrealizedPnlPct; }

    public BigDecimal getDailyPnl() { return dailyPnl; }
    public void setDailyPnl(BigDecimal dailyPnl) { this.dailyPnl = dailyPnl; }

    public BigDecimal getDailyPnlPct() { return dailyPnlPct; }
    public void setDailyPnlPct(BigDecimal dailyPnlPct) { this.dailyPnlPct = dailyPnlPct; }

    public BigDecimal getAllocationPct() { return allocationPct; }
    public void setAllocationPct(BigDecimal allocationPct) { this.allocationPct = allocationPct; }

    public boolean isPriceStale() { return isPriceStale; }
    public void setPriceStale(boolean priceStale) { isPriceStale = priceStale; }
}
