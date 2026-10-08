package com.tradex.gateway.controller;

import com.tradex.gateway.entity.Trade;
import com.tradex.gateway.service.TradingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trades")
public class TradeController {

    private final TradingService tradingService;

    public TradeController(TradingService tradingService) {
        this.tradingService = tradingService;
    }

    @GetMapping
    public ResponseEntity<List<Trade>> getTrades(Authentication auth) {
        return ResponseEntity.ok(tradingService.getTrades(auth.getName()));
    }
}
