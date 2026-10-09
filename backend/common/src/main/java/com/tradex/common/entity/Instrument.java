package com.tradex.common.entity;

import jakarta.persistence.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "instruments")
public class Instrument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "symbol", nullable = false, unique = true, length = 20)
    private String symbol;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "type", length = 20)
    private String type = "EQUITY";

    @Column(name = "exchange", length = 50)
    private String exchange = "TRADEX";

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt = ZonedDateTime.now();

    public Instrument() {}

    public Instrument(String symbol, String name) {
        this.symbol = symbol;
        this.name = name;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }
}
