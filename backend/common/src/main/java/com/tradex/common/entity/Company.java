package com.tradex.common.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

/**
 * Company metadata — stored separately from time-series price data.
 * Do NOT store prices here; use {@link MarketPrice} and {@link MarketCandle} for that.
 */
@Entity
@Table(name = "companies")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "symbol", nullable = false, unique = true, length = 20)
    private String symbol;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "sector", length = 50)
    private String sector;

    @Column(name = "industry", length = 100)
    private String industry;

    @Column(name = "website", length = 255)
    private String website;

    @Column(name = "country", length = 50)
    private String country = "USA";

    /** Exchange where this instrument is listed, e.g. "NASDAQ", "NYSE", "TRADEX". */
    @Column(name = "exchange", length = 20)
    private String exchange = "TRADEX";

    @Column(name = "currency", length = 5)
    private String currency = "USD";

    /** Market capitalisation in USD. Refreshed periodically — not real-time. */
    @Column(name = "market_cap", precision = 22, scale = 2)
    private BigDecimal marketCap;

    @Column(name = "employees")
    private Integer employees;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt = ZonedDateTime.now();

    public Company() {}

    public Company(String symbol, String name, String sector) {
        this.symbol = symbol;
        this.name = name;
        this.sector = sector;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }

    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public BigDecimal getMarketCap() { return marketCap; }
    public void setMarketCap(BigDecimal marketCap) { this.marketCap = marketCap; }

    public Integer getEmployees() { return employees; }
    public void setEmployees(Integer employees) { this.employees = employees; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }
}
