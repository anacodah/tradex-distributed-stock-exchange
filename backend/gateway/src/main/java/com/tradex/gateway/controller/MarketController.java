package com.tradex.gateway.controller;

import com.tradex.gateway.entity.Stock;
import com.tradex.gateway.service.MarketService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/market")
public class MarketController {

    private final MarketService marketService;

    public MarketController(MarketService marketService) {
        this.marketService = marketService;
    }

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

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> marketStatus() {
        return ResponseEntity.ok(Map.of("status", "OPEN", "message", "Market is open"));
    }
}
