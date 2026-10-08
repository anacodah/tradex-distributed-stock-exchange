package com.tradex.gateway.controller;

import com.tradex.gateway.entity.Order;
import com.tradex.gateway.entity.Trade;
import com.tradex.gateway.service.TradingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final TradingService tradingService;

    public OrderController(TradingService tradingService) {
        this.tradingService = tradingService;
    }

    @GetMapping
    public ResponseEntity<List<Order>> getOrders(Authentication auth) {
        return ResponseEntity.ok(tradingService.getOrders(auth.getName()));
    }

    @PostMapping
    public ResponseEntity<?> placeOrder(Authentication auth, @RequestBody Map<String, Object> body) {
        try {
            String symbol = (String) body.get("symbol");
            String side = body.containsKey("side") ? (String) body.get("side") : (String) body.get("type");
            String orderType = body.containsKey("orderType") ? (String) body.get("orderType") : (String) body.get("orderKind");
            if (orderType == null) orderType = "MARKET";
            
            BigDecimal quantity = new BigDecimal(body.get("quantity").toString());
            BigDecimal price = body.get("price") != null ? new BigDecimal(body.get("price").toString()) : null;
            BigDecimal stopPrice = body.get("stopPrice") != null ? new BigDecimal(body.get("stopPrice").toString()) : null;

            Order order = tradingService.placeOrder(auth.getName(), symbol, side, orderType, quantity, price, stopPrice);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelOrder(Authentication auth, @PathVariable Long id) {
        try {
            return ResponseEntity.ok(tradingService.cancelOrder(auth.getName(), id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
