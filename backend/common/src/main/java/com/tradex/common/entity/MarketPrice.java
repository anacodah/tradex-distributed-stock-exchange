package com.tradex.common.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Entity
@Table(name = "market_prices")
public class MarketPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_id", nullable = false)
    private Long stockId;

    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    @Column(name = "price", nullable = false, precision = 15, scale = 4)
    private BigDecimal price;

    @Column(name = "change_amount", precision = 15, scale = 4)
    private BigDecimal changeAmount;

    @Column(name = "change_percent", precision = 10, scale = 4)
    private BigDecimal changePercent;

    @Column(name = "day_high", precision = 15, scale = 4)
    private BigDecimal dayHigh;

    @Column(name = "day_low", precision = 15, scale = 4)
    private BigDecimal dayLow;

    @Column(name = "volume")
    private Long volume;

    @Column(name = "timestamp", nullable = false)
    private ZonedDateTime timestamp = ZonedDateTime.now();

    /** "SIMULATED" or "LIVE". All data defaults to SIMULATED. */
    @Column(name = "data_source", length = 20)
    private String dataSource = "SIMULATED";

    public MarketPrice() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStockId() { return stockId; }
    public void setStockId(Long stockId) { this.stockId = stockId; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getChangeAmount() { return changeAmount; }
    public void setChangeAmount(BigDecimal changeAmount) { this.changeAmount = changeAmount; }

    public BigDecimal getChangePercent() { return changePercent; }
    public void setChangePercent(BigDecimal changePercent) { this.changePercent = changePercent; }

    public BigDecimal getDayHigh() { return dayHigh; }
    public void setDayHigh(BigDecimal dayHigh) { this.dayHigh = dayHigh; }

    public BigDecimal getDayLow() { return dayLow; }
    public void setDayLow(BigDecimal dayLow) { this.dayLow = dayLow; }

    public Long getVolume() { return volume; }
    public void setVolume(Long volume) { this.volume = volume; }

    public ZonedDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(ZonedDateTime timestamp) { this.timestamp = timestamp; }

    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }
}
