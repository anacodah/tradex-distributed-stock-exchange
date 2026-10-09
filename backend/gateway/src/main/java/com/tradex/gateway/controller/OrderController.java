package com.tradex.gateway.controller;

import com.tradex.common.engine.OrderBookSnapshot;
import com.tradex.gateway.dto.OrderDetailDto;
import com.tradex.gateway.dto.OrderModifyRequest;
import com.tradex.gateway.dto.OrderRequest;
import com.tradex.gateway.entity.Order;
import com.tradex.gateway.service.TradingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Authoritative Order API for paper-trading lifecycle.
 *
 * Endpoints:
 * - POST   /api/orders               Place order (supports Idempotency-Key)
 * - PUT    /api/orders/{id}          Modify resting order price/quantity
 * - POST   /api/orders/{id}/cancel   Cancel resting order
 * - GET    /api/orders               All user orders
 * - GET    /api/orders/open          Open / partially filled orders
 * - GET    /api/orders/completed     Filled orders
 * - GET    /api/orders/rejected      Rejected orders
 * - GET    /api/orders/{id}          Order details + fills
 * - GET    /api/orders/book/{symbol} Order book depth (L2)
 */
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

    @GetMapping("/open")
    public ResponseEntity<List<Order>> getOpenOrders(Authentication auth) {
        return ResponseEntity.ok(tradingService.getOpenOrders(auth.getName()));
    }

    @GetMapping("/completed")
    public ResponseEntity<List<Order>> getCompletedOrders(Authentication auth) {
        return ResponseEntity.ok(tradingService.getCompletedOrders(auth.getName()));
    }

    @GetMapping("/rejected")
    public ResponseEntity<List<Order>> getRejectedOrders(Authentication auth) {
        return ResponseEntity.ok(tradingService.getRejectedOrders(auth.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderDetail(Authentication auth, @PathVariable Long id) {
        try {
            OrderDetailDto detail = tradingService.getOrderDetail(auth.getName(), id);
            return ResponseEntity.ok(detail);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> placeOrder(Authentication auth,
                                        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                        @RequestBody Map<String, Object> body) {
        try {
            OrderRequest req = new OrderRequest();
            req.setSymbol((String) body.get("symbol"));

            String side = body.containsKey("side") ? (String) body.get("side") : (String) body.get("type");
            req.setSide(side);

            String orderType = body.containsKey("orderType") ? (String) body.get("orderType") : (String) body.get("orderKind");
            req.setOrderType(orderType != null ? orderType : "MARKET");

            if (body.get("quantity") != null) {
                req.setQuantity(new BigDecimal(body.get("quantity").toString()));
            }
            if (body.get("price") != null) {
                req.setPrice(new BigDecimal(body.get("price").toString()));
            }
            if (body.get("stopPrice") != null) {
                req.setStopPrice(new BigDecimal(body.get("stopPrice").toString()));
            }
            if (body.get("clientOrderId") != null) {
                req.setClientOrderId(body.get("clientOrderId").toString());
            }

            Order order = tradingService.placeOrder(auth.getName(), req, idempotencyKey);
            return ResponseEntity.ok(order);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage(), "error", "Validation error"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage(), "error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> modifyOrder(Authentication auth,
                                         @PathVariable Long id,
                                         @RequestBody OrderModifyRequest req) {
        try {
            Order modified = tradingService.modifyOrder(auth.getName(), id, req.getPrice(), req.getQuantity());
            return ResponseEntity.ok(modified);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelOrder(Authentication auth, @PathVariable Long id) {
        try {
            return ResponseEntity.ok(tradingService.cancelOrder(auth.getName(), id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage(), "message", e.getMessage()));
        }
    }

    @GetMapping("/book/{symbol}")
    public ResponseEntity<OrderBookSnapshot> getOrderBookDepth(@PathVariable String symbol,
                                                               @RequestParam(defaultValue = "10") int depth) {
        return ResponseEntity.ok(tradingService.getOrderBookDepth(symbol, depth));
    }
}
