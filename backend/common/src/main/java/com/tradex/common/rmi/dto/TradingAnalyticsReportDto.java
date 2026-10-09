package com.tradex.common.rmi.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TradingAnalyticsReportDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private BigDecimal totalTradingVolume = BigDecimal.ZERO;
    private long totalTradeCount;
    private long totalOrderCount;
    private long filledOrderCount;
    private long cancelledOrderCount;
    private long rejectedOrderCount;
    private double fillRatePercent;
    private double cancellationRatePercent;
    private double rejectionRatePercent;
    private BigDecimal totalBuyVolume = BigDecimal.ZERO;
    private BigDecimal totalSellVolume = BigDecimal.ZERO;
    private double buyRatioPercent;
    private double sellRatioPercent;
    private List<Map<String, Object>> mostActiveInstruments = new ArrayList<>();
    private List<Map<String, Object>> topMarketGainers = new ArrayList<>();
    private List<Map<String, Object>> topMarketLosers = new ArrayList<>();
    private List<Map<String, Object>> volumeHistory = new ArrayList<>();
    private long timestamp;

    public TradingAnalyticsReportDto() {}

    public BigDecimal getTotalTradingVolume() { return totalTradingVolume; }
    public void setTotalTradingVolume(BigDecimal totalTradingVolume) { this.totalTradingVolume = totalTradingVolume; }

    public long getTotalTradeCount() { return totalTradeCount; }
    public void setTotalTradeCount(long totalTradeCount) { this.totalTradeCount = totalTradeCount; }

    public long getTotalOrderCount() { return totalOrderCount; }
    public void setTotalOrderCount(long totalOrderCount) { this.totalOrderCount = totalOrderCount; }

    public long getFilledOrderCount() { return filledOrderCount; }
    public void setFilledOrderCount(long filledOrderCount) { this.filledOrderCount = filledOrderCount; }

    public long getCancelledOrderCount() { return cancelledOrderCount; }
    public void setCancelledOrderCount(long cancelledOrderCount) { this.cancelledOrderCount = cancelledOrderCount; }

    public long getRejectedOrderCount() { return rejectedOrderCount; }
    public void setRejectedOrderCount(long rejectedOrderCount) { this.rejectedOrderCount = rejectedOrderCount; }

    public double getFillRatePercent() { return fillRatePercent; }
    public void setFillRatePercent(double fillRatePercent) { this.fillRatePercent = fillRatePercent; }

    public double getCancellationRatePercent() { return cancellationRatePercent; }
    public void setCancellationRatePercent(double cancellationRatePercent) { this.cancellationRatePercent = cancellationRatePercent; }

    public double getRejectionRatePercent() { return rejectionRatePercent; }
    public void setRejectionRatePercent(double rejectionRatePercent) { this.rejectionRatePercent = rejectionRatePercent; }

    public BigDecimal getTotalBuyVolume() { return totalBuyVolume; }
    public void setTotalBuyVolume(BigDecimal totalBuyVolume) { this.totalBuyVolume = totalBuyVolume; }

    public BigDecimal getTotalSellVolume() { return totalSellVolume; }
    public void setTotalSellVolume(BigDecimal totalSellVolume) { this.totalSellVolume = totalSellVolume; }

    public double getBuyRatioPercent() { return buyRatioPercent; }
    public void setBuyRatioPercent(double buyRatioPercent) { this.buyRatioPercent = buyRatioPercent; }

    public double getSellRatioPercent() { return sellRatioPercent; }
    public void setSellRatioPercent(double sellRatioPercent) { this.sellRatioPercent = sellRatioPercent; }

    public List<Map<String, Object>> getMostActiveInstruments() { return mostActiveInstruments; }
    public void setMostActiveInstruments(List<Map<String, Object>> mostActiveInstruments) { this.mostActiveInstruments = mostActiveInstruments; }

    public List<Map<String, Object>> getTopMarketGainers() { return topMarketGainers; }
    public void setTopMarketGainers(List<Map<String, Object>> topMarketGainers) { this.topMarketGainers = topMarketGainers; }

    public List<Map<String, Object>> getTopMarketLosers() { return topMarketLosers; }
    public void setTopMarketLosers(List<Map<String, Object>> topMarketLosers) { this.topMarketLosers = topMarketLosers; }

    public List<Map<String, Object>> getVolumeHistory() { return volumeHistory; }
    public void setVolumeHistory(List<Map<String, Object>> volumeHistory) { this.volumeHistory = volumeHistory; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
