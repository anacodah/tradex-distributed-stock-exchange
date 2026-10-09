package com.tradex.gateway.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

/**
 * Stock entity representing the canonical latest-state record for each listed instrument.
 * This stores session-level OHLC (open/high/low from the day's session start) plus
 * the most recent intraday price. These must NEVER be mixed with historical candle data.
 *
 * data_source values: "SIMULATED" | "LIVE"
 */
@Entity
@Table(name = "stocks")
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 10)
    private String symbol;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    /** Latest intraday price. Updated by the market data engine; NOT the session open. */
    @Column(name = "current_price", nullable = false, precision = 15, scale = 4)
    private BigDecimal currentPrice;

    /** Session open price (first trade of the day's session). */
    @Column(name = "open_price", nullable = false, precision = 15, scale = 4)
    private BigDecimal openPrice;

    /** Session high (intraday). */
    @Column(name = "high_price", nullable = false, precision = 15, scale = 4)
    private BigDecimal highPrice;

    /** Session low (intraday). */
    @Column(name = "low_price", nullable = false, precision = 15, scale = 4)
    private BigDecimal lowPrice;

    /** Previous day closing price. */
    @Column(name = "previous_close", nullable = false, precision = 15, scale = 4)
    private BigDecimal previousClose;

    /** Cumulative session volume. */
    @Column(nullable = false)
    private Long volume;

    @Column(name = "market_cap", precision = 20, scale = 2)
    private BigDecimal marketCap;

    private String sector;

    /** "SIMULATED" or "LIVE". All prices in this system default to SIMULATED. */
    @Column(name = "data_source", length = 20)
    private String dataSource = "SIMULATED";

    @Column(name = "exchange", length = 20)
    private String exchange = "TRADEX";

    @Column(name = "currency", length = 5)
    private String currency = "USD";

    /** Timestamp of the most recent price update. Clients must display this alongside the price. */
    @Column(name = "price_timestamp")
    private ZonedDateTime priceTimestamp = ZonedDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt = ZonedDateTime.now();

    // ---- Getters / Setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }

    public BigDecimal getOpenPrice() { return openPrice; }
    public void setOpenPrice(BigDecimal openPrice) { this.openPrice = openPrice; }

    public BigDecimal getHighPrice() { return highPrice; }
    public void setHighPrice(BigDecimal highPrice) { this.highPrice = highPrice; }

    public BigDecimal getLowPrice() { return lowPrice; }
    public void setLowPrice(BigDecimal lowPrice) { this.lowPrice = lowPrice; }

    public BigDecimal getPreviousClose() { return previousClose; }
    public void setPreviousClose(BigDecimal previousClose) { this.previousClose = previousClose; }

    public Long getVolume() { return volume; }
    public void setVolume(Long volume) { this.volume = volume; }

    public BigDecimal getMarketCap() { return marketCap; }
    public void setMarketCap(BigDecimal marketCap) { this.marketCap = marketCap; }

    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }

    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }

    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public ZonedDateTime getPriceTimestamp() { return priceTimestamp; }
    public void setPriceTimestamp(ZonedDateTime priceTimestamp) { this.priceTimestamp = priceTimestamp; }

    public ZonedDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(ZonedDateTime updatedAt) { this.updatedAt = updatedAt; }
}
