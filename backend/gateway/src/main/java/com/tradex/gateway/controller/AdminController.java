package com.tradex.gateway.controller;

import com.tradex.common.rmi.dto.TradingAnalyticsReportDto;
import com.tradex.gateway.client.NodeRmiClientService;
import com.tradex.gateway.entity.Order;
import com.tradex.gateway.entity.Trade;
import com.tradex.gateway.entity.User;
import com.tradex.gateway.repository.OrderRepository;
import com.tradex.gateway.repository.TradeRepository;
import com.tradex.gateway.repository.UserRepository;
import com.tradex.gateway.service.AuditService;
import com.tradex.gateway.service.DistributedLoadBalancerService;
import com.tradex.gateway.service.TradingAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final TradeRepository tradeRepository;
    private final NodeRmiClientService rmiClientService;
    private final DistributedLoadBalancerService loadBalancerService;
    private final TradingAnalyticsService analyticsService;
    private final AuditService auditService;

    public AdminController(UserRepository userRepository,
                           OrderRepository orderRepository,
                           TradeRepository tradeRepository,
                           NodeRmiClientService rmiClientService,
                           DistributedLoadBalancerService loadBalancerService,
                           TradingAnalyticsService analyticsService,
                           AuditService auditService) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.tradeRepository = tradeRepository;
        this.rmiClientService = rmiClientService;
        this.loadBalancerService = loadBalancerService;
        this.analyticsService = analyticsService;
        this.auditService = auditService;
    }

    /**
     * Overview metrics for the administrative dashboard.
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getAdminOverview() {
        long userCount = userRepository.count();
        long orderCount = orderRepository.count();
        long tradeCount = tradeRepository.count();
        String leader = rmiClientService.discoverLeaderNode();
        long epoch = rmiClientService.getCurrentLeaderEpoch();
        String tradingState = rmiClientService.getTradingState();

        return ResponseEntity.ok(Map.of(
                "userCount", userCount,
                "orderCount", orderCount,
                "tradeCount", tradeCount,
                "activeLeader", leader,
                "leaderEpoch", epoch,
                "tradingState", tradingState,
                "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * Role-protected list of all registered users and their account states.
     */
    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (User u : users) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("username", u.getUsername());
            map.put("email", u.getEmail());
            map.put("enabled", u.isEnabled());
            map.put("roles", u.getRoles().stream().map(r -> r.getName()).toList());
            map.put("createdAt", u.getCreatedAt());
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Role-protected list of all system orders.
     */
    @GetMapping("/orders")
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderRepository.findAll());
    }

    /**
     * Role-protected list of all executed trades.
     */
    @GetMapping("/trades")
    public ResponseEntity<List<Trade>> getAllTrades() {
        return ResponseEntity.ok(tradeRepository.findAll());
    }

    /**
     * Comprehensive persisted trading analytics.
     */
    @GetMapping("/analytics")
    public ResponseEntity<TradingAnalyticsReportDto> getAnalytics() {
        return ResponseEntity.ok(analyticsService.getTradingAnalytics());
    }

    /**
     * Audited administrative emergency circuit-breaker to suspend or resume trading.
     */
    @PostMapping("/trading-circuit-breaker")
    public ResponseEntity<Map<String, Object>> toggleCircuitBreaker(@RequestBody Map<String, String> body) {
        String state = body.getOrDefault("state", "SUSPENDED");
        rmiClientService.setTradingState(state);
        auditService.recordAudit(null, "ADMIN", "ADMIN_CIRCUIT_BREAKER", "/api/admin/trading-circuit-breaker", "CIRCUIT_BREAKER", state, null, "SUCCESS", "Trading state set to " + state, null);
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "tradingState", state));
    }
}
