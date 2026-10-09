package com.tradex.common.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Entity
@Table(name = "market_candles")
public class MarketCandle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_id", nullable = false)
    private Long stockId;

    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    @Column(name = "timeframe", nullable = false, length = 10)
    private String timeframe;

    @Column(name = "open", nullable = false, precision = 15, scale = 4)
    private BigDecimal open;

    @Column(name = "high", nullable = false, precision = 15, scale = 4)
    private BigDecimal high;

    @Column(name = "low", nullable = false, precision = 15, scale = 4)
    private BigDecimal low;

    @Column(name = "close", nullable = false, precision = 15, scale = 4)
    private BigDecimal close;

    @Column(name = "volume", nullable = false)
    private Long volume;

    @Column(name = "bucket_start", nullable = false)
    private ZonedDateTime bucketStart;

    /** "SIMULATED" or "LIVE". All data defaults to SIMULATED. */
    @Column(name = "data_source", length = 20)
    private String dataSource = "SIMULATED";

    public MarketCandle() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStockId() { return stockId; }
    public void setStockId(Long stockId) { this.stockId = stockId; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getTimeframe() { return timeframe; }
    public void setTimeframe(String timeframe) { this.timeframe = timeframe; }

    public BigDecimal getOpen() { return open; }
    public void setOpen(BigDecimal open) { this.open = open; }

    public BigDecimal getHigh() { return high; }
    public void setHigh(BigDecimal high) { this.high = high; }

    public BigDecimal getLow() { return low; }
    public void setLow(BigDecimal low) { this.low = low; }

    public BigDecimal getClose() { return close; }
    public void setClose(BigDecimal close) { this.close = close; }

    public Long getVolume() { return volume; }
    public void setVolume(Long volume) { this.volume = volume; }

    public ZonedDateTime getBucketStart() { return bucketStart; }
    public void setBucketStart(ZonedDateTime bucketStart) { this.bucketStart = bucketStart; }

    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }
}
