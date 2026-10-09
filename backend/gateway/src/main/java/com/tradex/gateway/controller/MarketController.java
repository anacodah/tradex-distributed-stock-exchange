package com.tradex.gateway.controller;

import com.tradex.gateway.dto.CompanyDto;
import com.tradex.gateway.dto.MarketStatsDto;
import com.tradex.gateway.dto.OhlcvCandleDto;
import com.tradex.gateway.dto.PriceSnapshotDto;
import com.tradex.gateway.entity.Stock;
import com.tradex.gateway.service.MarketService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

/**
 * Market Data REST API.
 *
 * All endpoints are read-only.  Price data is read from the database —
 * the engine is the only writer.  Endpoints do NOT generate or invent prices.
 *
 * Public (no auth required — configured in SecurityConfig):
 *   GET /api/market/stocks              — list all stocks (raw entity, for order routing)
 *   GET /api/market/stocks/{symbol}     — single stock entity
 *   GET /api/market/prices              — all latest price snapshots (PriceSnapshotDto)
 *   GET /api/market/prices/{symbol}     — single price snapshot
 *   GET /api/market/candles/{symbol}    — OHLCV candles  ?timeframe=1H&from=...&to=...
 *   GET /api/market/companies           — search companies  ?q=...
 *   GET /api/market/companies/{symbol}  — company profile
 *   GET /api/market/sectors             — list of all sectors
 *   GET /api/market/sectors/{sector}/companies — companies in a sector
 *   GET /api/market/stats               — market-wide and sector statistics
 *   GET /api/market/status              — market open/closed status
 */
@RestController
@RequestMapping("/api/market")
public class MarketController {

    private final MarketService marketService;

    public MarketController(MarketService marketService) {
        this.marketService = marketService;
    }

    // ------------------------------------------------------------------
    // Stocks (raw entity — includes session OHLC for order routing)
    // ------------------------------------------------------------------

    @GetMapping("/stocks")
    public ResponseEntity<List<Stock>> getAllStocks(@RequestParam(required = false) String search) {
        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(marketService.searchStocks(search));
        }
        return ResponseEntity.ok(marketService.getAllStocks());
    }

    @GetMapping("/stocks/{symbol}")
    public ResponseEntity<Stock> getStock(@PathVariable String symbol) {
        return ResponseEntity.ok(marketService.getStockBySymbol(symbol));
    }

    // ------------------------------------------------------------------
    // Price snapshots — clearly labelled with dataSource
    // These are exchange-session + intraday values, kept strictly separate.
    // ------------------------------------------------------------------

    /**
     * All latest price snapshots.
     * {@code dataSource} field in each entry identifies whether data is SIMULATED or LIVE.
     */
    @GetMapping("/prices")
    public ResponseEntity<List<PriceSnapshotDto>> getAllPrices() {
        return ResponseEntity.ok(marketService.getAllSnapshots());
    }

    /**
     * Latest price snapshot for one instrument.
     */
    @GetMapping("/prices/{symbol}")
    public ResponseEntity<PriceSnapshotDto> getPrice(@PathVariable String symbol) {
        return ResponseEntity.ok(marketService.getSnapshot(symbol));
    }

    // ------------------------------------------------------------------
    // OHLCV candles — read from market_candles table
    // DO NOT mix 1D session candles with intraday prices.
    // ------------------------------------------------------------------

    /**
     * Historical OHLCV candles for a symbol.
     *
     * @param symbol    Ticker symbol (case-insensitive)
     * @param timeframe "1M", "1H", "1D" (default: "1H")
     * @param from      Optional ISO-8601 window start (inclusive)
     * @param to        Optional ISO-8601 window end (inclusive)
     */
    @GetMapping("/candles/{symbol}")
    public ResponseEntity<List<OhlcvCandleDto>> getCandles(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "1H") String timeframe,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime to) {
        return ResponseEntity.ok(marketService.getCandles(symbol, timeframe, from, to));
    }

    // ------------------------------------------------------------------
    // Company / instrument profiles — metadata only, no prices
    // ------------------------------------------------------------------

    /**
     * Search companies by symbol or name fragment.
     */
    @GetMapping("/companies")
    public ResponseEntity<List<CompanyDto>> searchCompanies(@RequestParam(required = false) String q) {
        if (q == null || q.isBlank()) {
            return ResponseEntity.ok(List.of()); // require a query for search
        }
        return ResponseEntity.ok(marketService.searchCompanies(q));
    }

    /**
     * Full company profile (description, sector, industry, website, exchange, …).
     */
    @GetMapping("/companies/{symbol}")
    public ResponseEntity<CompanyDto> getCompany(@PathVariable String symbol) {
        return ResponseEntity.ok(marketService.getCompanyProfile(symbol));
    }

    // ------------------------------------------------------------------
    // Sector queries
    // ------------------------------------------------------------------

    @GetMapping("/sectors")
    public ResponseEntity<List<String>> getAllSectors() {
        return ResponseEntity.ok(marketService.getAllSectors());
    }

    @GetMapping("/sectors/{sector}/companies")
    public ResponseEntity<List<CompanyDto>> getCompaniesBySector(@PathVariable String sector) {
        return ResponseEntity.ok(marketService.getCompaniesBySector(sector));
    }

    // ------------------------------------------------------------------
    // Market-wide statistics
    // ------------------------------------------------------------------

    /**
     * Market-wide advancers, decliners, unchanged counts + per-sector stats.
     * All derived from persisted data — never invented.
     */
    @GetMapping("/stats")
    public ResponseEntity<MarketStatsDto> getMarketStats() {
        return ResponseEntity.ok(marketService.getMarketStats());
    }

    // ------------------------------------------------------------------
    // Market status
    // ------------------------------------------------------------------

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> marketStatus() {
        return ResponseEntity.ok(Map.of(
                "status", "OPEN",
                "message", "TradeX simulated market is running",
                "dataSource", "SIMULATED"
        ));
    }
}
