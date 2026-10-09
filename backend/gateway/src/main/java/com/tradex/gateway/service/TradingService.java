package com.tradex.gateway.service;

import com.tradex.common.engine.*;
import com.tradex.common.entity.IdempotencyRecord;
import com.tradex.common.entity.OrderEvent;
import com.tradex.common.repository.IdempotencyRecordRepository;
import com.tradex.common.repository.OrderEventRepository;
import com.tradex.gateway.dto.*;
import com.tradex.gateway.entity.*;
import com.tradex.gateway.repository.*;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class TradingService {

    private static final Logger log = LoggerFactory.getLogger(TradingService.class);

    private final OrderRepository orderRepository;
    private final TradeRepository tradeRepository;
    private final HoldingRepository holdingRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTxRepository;
    private final StockRepository stockRepository;
    private final UserRepository userRepository;
    private final OrderEventRepository orderEventRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final org.springframework.beans.factory.ObjectProvider<NotificationService> notificationProvider;
    private final org.springframework.beans.factory.ObjectProvider<AuditService> auditProvider;
    private final org.springframework.beans.factory.ObjectProvider<com.tradex.gateway.client.NodeRmiClientService> rmiClientProvider;

    private final MatchingEngine matchingEngine = new MatchingEngine();
    private final AtomicLong globalSequenceGenerator = new AtomicLong(1000);

    public TradingService(OrderRepository orderRepository,
                          TradeRepository tradeRepository,
                          HoldingRepository holdingRepository,
                          WalletRepository walletRepository,
                          WalletTransactionRepository walletTxRepository,
                          StockRepository stockRepository,
                          UserRepository userRepository,
                          OrderEventRepository orderEventRepository,
                          IdempotencyRecordRepository idempotencyRecordRepository,
                          org.springframework.beans.factory.ObjectProvider<NotificationService> notificationProvider,
                          org.springframework.beans.factory.ObjectProvider<AuditService> auditProvider,
                          org.springframework.beans.factory.ObjectProvider<com.tradex.gateway.client.NodeRmiClientService> rmiClientProvider) {
        this.orderRepository = orderRepository;
        this.tradeRepository = tradeRepository;
        this.holdingRepository = holdingRepository;
        this.walletRepository = walletRepository;
        this.walletTxRepository = walletTxRepository;
        this.stockRepository = stockRepository;
        this.userRepository = userRepository;
        this.orderEventRepository = orderEventRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.notificationProvider = notificationProvider;
        this.auditProvider = auditProvider;
        this.rmiClientProvider = rmiClientProvider;
    }

    @PostConstruct
    public void initOrderBooks() {
        try {
            List<Order> restingOrders = orderRepository.findActiveRestingOrders();
            for (Order o : restingOrders) {
                if (o.getStatus().equals("OPEN") || o.getStatus().equals("PARTIALLY_FILLED")) {
                    BookOrder bo = new BookOrder(
                            o.getId(),
                            o.getUser().getId(),
                            o.getStock().getSymbol(),
                            OrderSide.fromString(o.getSide()),
                            OrderType.fromString(o.getOrderType()),
                            o.getPrice(),
                            o.getStopPrice(),
                            o.getQuantity(),
                            o.getFilledQuantity(),
                            o.getSequenceNumber() != null ? o.getSequenceNumber() : o.getId(),
                            ZonedDateTime.now()
                    );
                    matchingEngine.getOrCreateBook(o.getStock().getSymbol()).addOrder(bo);
                }
            }
            log.info("Initialized MatchingEngine with {} active resting orders", restingOrders.size());
        } catch (Exception e) {
            log.warn("Could not load initial resting orders into memory: {}", e.getMessage());
        }
    }

    public MatchingEngine getMatchingEngine() {
        return matchingEngine;
    }

    /**
     * Submit and execute a paper-trading order with full lifecycle tracking.
     */
    @Transactional
    public Order placeOrder(String username, OrderRequest req, String idempotencyKey) {
        // 1. Idempotency Check
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<Order> existingOrder = orderRepository.findByIdempotencyKey(idempotencyKey.trim());
            if (existingOrder.isPresent()) {
                log.info("Duplicate order submission detected for idempotency key: {}", idempotencyKey);
                return existingOrder.get();
            }
        }

        // Trading Safety Check: reject new writes if cluster is recovering or suspended
        var rmiClientCheck = rmiClientProvider.getIfAvailable();
        if (rmiClientCheck != null && "SUSPENDED".equalsIgnoreCase(rmiClientCheck.getTradingState())) {
            throw new IllegalStateException("Trading is temporarily SUSPENDED: Cluster leader election / state recovery in progress.");
        }

        // 2. Validate Inputs
        if (req.getSymbol() == null || req.getSymbol().isBlank()) {
            throw new IllegalArgumentException("Symbol is required");
        }
        if (req.getQuantity() == null || req.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
        OrderSide side = OrderSide.fromString(req.getSide());
        if (side == null) {
            throw new IllegalArgumentException("Side must be BUY or SELL");
        }
        OrderType orderType = OrderType.fromString(req.getOrderType());
        if (orderType == OrderType.LIMIT && (req.getPrice() == null || req.getPrice().compareTo(BigDecimal.ZERO) <= 0)) {
            throw new IllegalArgumentException("Limit price must be greater than 0 for LIMIT orders");
        }
        if (orderType == OrderType.STOP_LOSS && (req.getStopPrice() == null || req.getStopPrice().compareTo(BigDecimal.ZERO) <= 0)) {
            throw new IllegalArgumentException("Stop price must be greater than 0 for STOP_LOSS orders");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        Stock stock = stockRepository.findBySymbol(req.getSymbol().trim().toUpperCase())
                .orElseThrow(() -> new RuntimeException("Instrument not found: " + req.getSymbol()));

        // 3. Financial & Holding Validation & Reservation
        BigDecimal reservedAmount = BigDecimal.ZERO;
        BigDecimal reservedShares = BigDecimal.ZERO;

        Wallet wallet = walletRepository.findByUserIdForUpdate(user.getId())
                .orElseThrow(() -> new RuntimeException("Wallet not found. Please initialize wallet."));

        if (side == OrderSide.BUY) {
            BigDecimal estimatedPrice = (orderType == OrderType.LIMIT) ? req.getPrice() : stock.getCurrentPrice();
            if (estimatedPrice == null || estimatedPrice.compareTo(BigDecimal.ZERO) <= 0) {
                estimatedPrice = stock.getCurrentPrice();
            }
            reservedAmount = estimatedPrice.multiply(req.getQuantity()).setScale(4, RoundingMode.HALF_UP);

            if (wallet.getAvailableBalance().compareTo(reservedAmount) < 0) {
                throw new RuntimeException("Insufficient funds. Required: $" + reservedAmount + ", Available: $" + wallet.getAvailableBalance());
            }

            // Reserve funds
            wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(reservedAmount));
            wallet.setReservedBalance(wallet.getReservedBalance().add(reservedAmount));
            walletRepository.save(wallet);

        } else { // SELL
            reservedShares = req.getQuantity();
            Holding holding = holdingRepository.findByUserIdAndStockIdForUpdate(user.getId(), stock.getId())
                    .orElseThrow(() -> new RuntimeException("No holdings found for " + stock.getSymbol()));

            if (holding.getQuantity().compareTo(reservedShares) < 0) {
                throw new RuntimeException("Insufficient shares. Owned: " + holding.getQuantity() + ", Required: " + reservedShares);
            }
        }

        // 4. Create and persist Order in NEW state
        Order order = new Order();
        order.setUser(user);
        order.setStock(stock);
        order.setSide(side.name());
        order.setOrderType(orderType.name());
        order.setQuantity(req.getQuantity());
        order.setFilledQuantity(BigDecimal.ZERO);
        order.setPrice(req.getPrice());
        order.setStopPrice(req.getStopPrice());
        order.setReservedAmount(reservedAmount);
        order.setReservedShares(reservedShares);
        order.setStatus(OrderStatus.NEW.name());
        order.setSequenceNumber(globalSequenceGenerator.incrementAndGet());
        order.setIdempotencyKey(idempotencyKey);
        order.setClientOrderId(req.getClientOrderId());
        order = orderRepository.save(order);

        recordOrderEvent(order.getId(), "CREATED", "Order submitted by user " + username);

        // 5. Submit to Authoritative Matching Engine on Cluster Leader via Java RMI
        com.tradex.common.rmi.dto.RemoteOrderResponseDto rmiResp = null;
        var rmiClient = rmiClientProvider.getIfAvailable();
        if (rmiClient != null) {
            try {
                com.tradex.common.rmi.dto.RemoteOrderRequestDto rmiReq = new com.tradex.common.rmi.dto.RemoteOrderRequestDto();
                rmiReq.setRequestId(UUID.randomUUID().toString());
                rmiReq.setCorrelationId("ORD-" + order.getId());
                rmiReq.setOrderId(order.getId());
                rmiReq.setUserId(user.getId());
                rmiReq.setUsername(username);
                rmiReq.setSymbol(stock.getSymbol());
                rmiReq.setSide(side.name());
                rmiReq.setOrderType(orderType.name());
                rmiReq.setPrice(req.getPrice());
                rmiReq.setStopPrice(req.getStopPrice());
                rmiReq.setQuantity(req.getQuantity());
                rmiReq.setIdempotencyKey(idempotencyKey);
                rmiReq.setSourceTimestamp(System.currentTimeMillis());

                rmiResp = rmiClient.routeOrderToLeader(rmiReq);
                if (rmiResp != null && rmiResp.isSuccessful()) {
                    log.info("Authoritative order #{} matched remotely by leader {}", order.getId(), rmiResp.getMatchedByNode());
                }
            } catch (Exception e) {
                log.warn("Cluster leader RMI invocation unavailable, falling back to local engine: {}", e.getMessage());
            }
        }

        // 6. Process Trades & Settlement
        if (rmiResp != null && rmiResp.isSuccessful()) {
            for (var tradeDto : rmiResp.getTrades()) {
                TradeExecution te = new TradeExecution(
                        stock.getSymbol(),
                        tradeDto.getMakerOrderId(),
                        tradeDto.getTakerOrderId(),
                        tradeDto.getBuyerUserId(),
                        tradeDto.getSellerUserId(),
                        side,
                        tradeDto.getPrice(),
                        tradeDto.getQuantity(),
                        tradeDto.getTotalValue() != null ? tradeDto.getTotalValue() : tradeDto.getPrice().multiply(tradeDto.getQuantity())
                );
                settleTrade(order, te, stock);
            }
            order.setStatus(rmiResp.getStatus());
            order.setFilledQuantity(rmiResp.getFilledQuantity() != null ? rmiResp.getFilledQuantity() : BigDecimal.ZERO);
        } else {
            // Local fallback matching engine
            BookOrder bookOrder = new BookOrder(
                    order.getId(),
                    user.getId(),
                    stock.getSymbol(),
                    side,
                    orderType,
                    req.getPrice(),
                    req.getStopPrice(),
                    req.getQuantity(),
                    BigDecimal.ZERO,
                    order.getSequenceNumber(),
                    ZonedDateTime.now()
            );

            MatchResult matchResult = matchingEngine.match(bookOrder, stock.getCurrentPrice());
            for (TradeExecution tradeExec : matchResult.getTrades()) {
                settleTrade(order, tradeExec, stock);
            }

            BigDecimal remainingQty = order.getQuantity().subtract(order.getFilledQuantity());
            if (matchResult.getFinalStatus() == OrderStatus.FILLED) {
                order.setStatus(OrderStatus.FILLED.name());
            } else if (order.getFilledQuantity().compareTo(BigDecimal.ZERO) > 0) {
                order.setStatus(OrderStatus.PARTIALLY_FILLED.name());
                if (orderType == OrderType.MARKET) {
                    refundUnfilledReservations(order, remainingQty, stock);
                }
            } else if (matchResult.getFinalStatus() == OrderStatus.REJECTED) {
                order.setStatus(OrderStatus.REJECTED.name());
                refundUnfilledReservations(order, order.getQuantity(), stock);
            } else {
                order.setStatus(OrderStatus.OPEN.name());
            }
        }

        order = orderRepository.save(order);
        recordOrderEvent(order.getId(), order.getStatus(), "Final match result: " + order.getStatus());

        // Notifications & Audit for Order Placement
        final Order savedOrder = order;
        notificationProvider.ifAvailable(ns -> {
            String title = "Order " + savedOrder.getStatus();
            String msg = savedOrder.getSide() + " " + savedOrder.getQuantity() + " " + stock.getSymbol() + " (" + savedOrder.getOrderType() + ") status: " + savedOrder.getStatus();
            ns.sendNotification(user.getId(), title, msg, "ORDER_" + savedOrder.getStatus(), "ord-" + savedOrder.getId() + "-" + savedOrder.getStatus(), savedOrder.getId(), null, "/orders");
        });
        auditProvider.ifAvailable(as -> {
            as.recordAudit(user.getId(), user.getUsername(), "ORDER_PLACED", "orders/" + savedOrder.getId(), "Order", String.valueOf(savedOrder.getId()), "ORD-" + savedOrder.getId(), "SUCCESS", "Placed " + savedOrder.getSide() + " " + savedOrder.getQuantity() + " " + stock.getSymbol(), null);
        });

        return order;
    }

    /**
     * Atomically settle an individual fill between buyer and seller.
     */
    private void settleTrade(Order takerOrder, TradeExecution exec, Stock stock) {
        User buyer = userRepository.findById(exec.getBuyerUserId()).orElseThrow();
        User seller = userRepository.findById(exec.getSellerUserId()).orElseThrow();
        BigDecimal tradeCost = exec.getTotalValue();

        // 1. Persist Trade Record
        Trade trade = new Trade();
        trade.setTradeId(exec.getTradeId());
        trade.setUser(takerOrder.getUser());
        trade.setBuyer(buyer);
        trade.setSeller(seller);
        trade.setStock(stock);
        trade.setOrder(takerOrder);
        trade.setSide(takerOrder.getSide());
        trade.setQuantity(exec.getQuantity());
        trade.setExecutionPrice(exec.getPrice());
        trade.setTotalValue(tradeCost);
        trade.setExecutedAt(LocalDateTime.now());
        tradeRepository.save(trade);

        // 2. Buyer Settlement
        Wallet buyerWallet = walletRepository.findByUserIdForUpdate(buyer.getId()).orElseThrow();
        BigDecimal reservedAmountToRelease = (takerOrder.getUser().getId().equals(buyer.getId()) && takerOrder.getPrice() != null)
                ? takerOrder.getPrice().multiply(exec.getQuantity()).setScale(4, RoundingMode.HALF_UP)
                : tradeCost;

        BigDecimal balBefore = buyerWallet.getBalance();
        buyerWallet.setBalance(buyerWallet.getBalance().subtract(tradeCost));
        buyerWallet.setReservedBalance(buyerWallet.getReservedBalance().subtract(reservedAmountToRelease).max(BigDecimal.ZERO));
        // Refund price improvement if execution price was lower than limit price
        if (reservedAmountToRelease.compareTo(tradeCost) > 0) {
            BigDecimal refund = reservedAmountToRelease.subtract(tradeCost);
            buyerWallet.setAvailableBalance(buyerWallet.getAvailableBalance().add(refund));
        }
        walletRepository.save(buyerWallet);

        WalletTransaction buyerTx = new WalletTransaction();
        buyerTx.setWallet(buyerWallet);
        buyerTx.setType("TRADE_DEBIT");
        buyerTx.setAmount(tradeCost);
        buyerTx.setBalanceBefore(balBefore);
        buyerTx.setBalanceAfter(buyerWallet.getBalance());
        buyerTx.setDescription("BUY " + exec.getQuantity() + " " + stock.getSymbol() + " @ $" + exec.getPrice());
        buyerTx.setReferenceId(takerOrder.getId());
        walletTxRepository.save(buyerTx);

        // Update Buyer Holdings
        Holding buyerHolding = holdingRepository.findByUserIdAndStockIdForUpdate(buyer.getId(), stock.getId())
                .orElseGet(() -> {
                    Holding h = new Holding();
                    h.setUser(buyer);
                    h.setStock(stock);
                    h.setQuantity(BigDecimal.ZERO);
                    h.setAveragePrice(BigDecimal.ZERO);
                    h.setRealizedPnl(BigDecimal.ZERO);
                    return h;
                });
        BigDecimal newQty = buyerHolding.getQuantity().add(exec.getQuantity());
        BigDecimal totalCost = buyerHolding.getQuantity().multiply(buyerHolding.getAveragePrice()).add(tradeCost);
        buyerHolding.setAveragePrice(totalCost.divide(newQty, 4, RoundingMode.HALF_UP));
        buyerHolding.setQuantity(newQty);
        holdingRepository.save(buyerHolding);

        // 3. Seller Settlement
        Wallet sellerWallet = walletRepository.findByUserIdForUpdate(seller.getId()).orElseThrow();
        BigDecimal sellerBalBefore = sellerWallet.getBalance();
        sellerWallet.setBalance(sellerWallet.getBalance().add(tradeCost));
        sellerWallet.setAvailableBalance(sellerWallet.getAvailableBalance().add(tradeCost));
        walletRepository.save(sellerWallet);

        WalletTransaction sellerTx = new WalletTransaction();
        sellerTx.setWallet(sellerWallet);
        sellerTx.setType("TRADE_CREDIT");
        sellerTx.setAmount(tradeCost);
        sellerTx.setBalanceBefore(sellerBalBefore);
        sellerTx.setBalanceAfter(sellerWallet.getBalance());
        sellerTx.setDescription("SELL " + exec.getQuantity() + " " + stock.getSymbol() + " @ $" + exec.getPrice());
        sellerTx.setReferenceId(exec.getMakerOrderId());
        walletTxRepository.save(sellerTx);

        // Update Seller Holdings
        Holding sellerHolding = holdingRepository.findByUserIdAndStockIdForUpdate(seller.getId(), stock.getId()).orElseThrow();
        BigDecimal costBasis = sellerHolding.getAveragePrice().multiply(exec.getQuantity());
        BigDecimal realizedGain = tradeCost.subtract(costBasis);
        sellerHolding.setRealizedPnl(sellerHolding.getRealizedPnl().add(realizedGain));
        sellerHolding.setQuantity(sellerHolding.getQuantity().subtract(exec.getQuantity()));
        if (sellerHolding.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            holdingRepository.delete(sellerHolding);
        } else {
            holdingRepository.save(sellerHolding);
        }

        // 4. Update Maker Order status & fills if different from taker
        if (!exec.getMakerOrderId().equals(takerOrder.getId())) {
            orderRepository.findById(exec.getMakerOrderId()).ifPresent(makerOrder -> {
                makerOrder.setFilledQuantity(makerOrder.getFilledQuantity().add(exec.getQuantity()));
                makerOrder.setExecutionPrice(exec.getPrice());
                if (makerOrder.getFilledQuantity().compareTo(makerOrder.getQuantity()) >= 0) {
                    makerOrder.setStatus(OrderStatus.FILLED.name());
                } else {
                    makerOrder.setStatus(OrderStatus.PARTIALLY_FILLED.name());
                }
                orderRepository.save(makerOrder);
                recordOrderEvent(makerOrder.getId(), makerOrder.getStatus(), "Maker filled " + exec.getQuantity() + " shares");
            });
        }

        // Update Taker Order filled quantity
        takerOrder.setFilledQuantity(takerOrder.getFilledQuantity().add(exec.getQuantity()));
        takerOrder.setExecutionPrice(exec.getPrice());

        // Notifications & Audit
        notificationProvider.ifAvailable(ns -> {
            ns.sendNotification(buyer.getId(), "Trade Executed", "Bought " + exec.getQuantity() + " " + stock.getSymbol() + " @ $" + exec.getPrice(), "TRADE_EXECUTED", "trade-buy-" + exec.getTradeId(), takerOrder.getId(), exec.getTradeId(), "/orders");
            ns.sendNotification(seller.getId(), "Trade Executed", "Sold " + exec.getQuantity() + " " + stock.getSymbol() + " @ $" + exec.getPrice(), "TRADE_EXECUTED", "trade-sell-" + exec.getTradeId(), exec.getMakerOrderId(), exec.getTradeId(), "/orders");
        });
        auditProvider.ifAvailable(as -> {
            as.recordAudit(buyer.getId(), buyer.getUsername(), "TRADE_EXECUTED", "trades/" + exec.getTradeId(), "Trade", exec.getTradeId(), "ORD-" + takerOrder.getId(), "SUCCESS", "Trade execution fill", null);
        });
    }

    private void refundUnfilledReservations(Order order, BigDecimal unfilledQty, Stock stock) {
        if (unfilledQty.compareTo(BigDecimal.ZERO) <= 0) return;

        if (order.getSide().equalsIgnoreCase("BUY")) {
            BigDecimal unspentCash = (order.getPrice() != null)
                    ? order.getPrice().multiply(unfilledQty).setScale(4, RoundingMode.HALF_UP)
                    : stock.getCurrentPrice().multiply(unfilledQty).setScale(4, RoundingMode.HALF_UP);

            walletRepository.findByUserIdForUpdate(order.getUser().getId()).ifPresent(wallet -> {
                wallet.setReservedBalance(wallet.getReservedBalance().subtract(unspentCash).max(BigDecimal.ZERO));
                wallet.setAvailableBalance(wallet.getAvailableBalance().add(unspentCash));
                walletRepository.save(wallet);
            });
        }
    }

    /**
     * Atomically modify an open or partially filled order.
     * Price modification or quantity increase loses time priority.
     */
    @Transactional
    public Order modifyOrder(String username, Long orderId, BigDecimal newPrice, BigDecimal newQuantity) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Order order = orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        OrderStatus status = OrderStatus.valueOf(order.getStatus());
        if (!status.isEligibleForModification()) {
            throw new IllegalStateException("Cannot modify order in state " + status);
        }

        if (newQuantity != null && newQuantity.compareTo(order.getFilledQuantity()) <= 0) {
            throw new IllegalArgumentException("New quantity must be greater than already filled quantity (" + order.getFilledQuantity() + ")");
        }

        boolean priceChanged = newPrice != null && order.getPrice() != null && newPrice.compareTo(order.getPrice()) != 0;
        boolean qtyIncreased = newQuantity != null && newQuantity.compareTo(order.getQuantity()) > 0;

        // Financial reservation adjustments
        if (order.getSide().equalsIgnoreCase("BUY")) {
            BigDecimal targetPrice = newPrice != null ? newPrice : order.getPrice();
            BigDecimal targetQty = newQuantity != null ? newQuantity : order.getQuantity();
            BigDecimal remainingTargetQty = targetQty.subtract(order.getFilledQuantity());
            BigDecimal newRequiredReservation = targetPrice.multiply(remainingTargetQty).setScale(4, RoundingMode.HALF_UP);

            BigDecimal currentRemainingQty = order.getQuantity().subtract(order.getFilledQuantity());
            BigDecimal currentReservation = order.getPrice().multiply(currentRemainingQty).setScale(4, RoundingMode.HALF_UP);

            BigDecimal diff = newRequiredReservation.subtract(currentReservation);
            Wallet wallet = walletRepository.findByUserIdForUpdate(user.getId()).orElseThrow();

            if (diff.compareTo(BigDecimal.ZERO) > 0) {
                if (wallet.getAvailableBalance().compareTo(diff) < 0) {
                    throw new RuntimeException("Insufficient available balance to increase order commitment");
                }
                wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(diff));
                wallet.setReservedBalance(wallet.getReservedBalance().add(diff));
            } else if (diff.compareTo(BigDecimal.ZERO) < 0) {
                BigDecimal refund = diff.abs();
                wallet.setReservedBalance(wallet.getReservedBalance().subtract(refund).max(BigDecimal.ZERO));
                wallet.setAvailableBalance(wallet.getAvailableBalance().add(refund));
            }
            walletRepository.save(wallet);
            order.setReservedAmount(newRequiredReservation);

        } else { // SELL
            if (qtyIncreased) {
                BigDecimal addedShares = newQuantity.subtract(order.getQuantity());
                Holding holding = holdingRepository.findByUserIdAndStockIdForUpdate(user.getId(), order.getStock().getId())
                        .orElseThrow(() -> new RuntimeException("No holdings found"));
                if (holding.getQuantity().compareTo(newQuantity) < 0) {
                    throw new RuntimeException("Insufficient shares owned to increase sell order quantity");
                }
            }
        }

        if (newPrice != null) order.setPrice(newPrice);
        if (newQuantity != null) order.setQuantity(newQuantity);

        // Time priority loss
        if (priceChanged || qtyIncreased) {
            order.setSequenceNumber(globalSequenceGenerator.incrementAndGet());
        }

        matchingEngine.modifyOrder(order.getStock().getSymbol(), order.getId(), newPrice, newQuantity, order.getSequenceNumber());
        recordOrderEvent(order.getId(), "MODIFIED", "Order modified. New price: " + newPrice + ", new qty: " + newQuantity);

        Order saved = orderRepository.save(order);
        notificationProvider.ifAvailable(ns -> {
            ns.sendNotification(user.getId(), "Order Modified", "Order #" + saved.getId() + " modified. Price: " + saved.getPrice() + ", Qty: " + saved.getQuantity(), "ORDER_MODIFIED", "ord-mod-" + saved.getId() + "-" + System.currentTimeMillis(), saved.getId(), null, "/orders");
        });
        auditProvider.ifAvailable(as -> {
            as.recordAudit(user.getId(), user.getUsername(), "ORDER_MODIFIED", "orders/" + saved.getId(), "Order", String.valueOf(saved.getId()), "ORD-" + saved.getId(), "SUCCESS", "Modified order price=" + newPrice + " qty=" + newQuantity, null);
        });

        return saved;
    }

    /**
     * Cancel an active order and refund remaining reserved funds/shares.
     */
    @Transactional
    public Order cancelOrder(String username, Long orderId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Order order = orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        OrderStatus currentStatus;
        try {
            currentStatus = OrderStatus.valueOf(order.getStatus());
        } catch (Exception e) {
            currentStatus = OrderStatus.OPEN;
        }

        if (!currentStatus.isEligibleForCancellation()) {
            throw new IllegalStateException("Cannot cancel order in state " + currentStatus);
        }

        matchingEngine.cancelOrder(order.getStock().getSymbol(), orderId);

        // Refund unspent funds
        BigDecimal unfilledQty = order.getQuantity().subtract(order.getFilledQuantity());
        refundUnfilledReservations(order, unfilledQty, order.getStock());

        order.setStatus(OrderStatus.CANCELLED.name());
        recordOrderEvent(order.getId(), "CANCELLED", "Order cancelled by user");

        Order saved = orderRepository.save(order);
        notificationProvider.ifAvailable(ns -> {
            ns.sendNotification(user.getId(), "Order Cancelled", "Order #" + saved.getId() + " cancelled successfully", "ORDER_CANCELLED", "ord-cancel-" + saved.getId(), saved.getId(), null, "/orders");
        });
        auditProvider.ifAvailable(as -> {
            as.recordAudit(user.getId(), user.getUsername(), "ORDER_CANCELLED", "orders/" + saved.getId(), "Order", String.valueOf(saved.getId()), "ORD-" + saved.getId(), "SUCCESS", "Cancelled order #" + saved.getId(), null);
        });

        return saved;
    }

    /**
     * Trigger stop-loss orders when market price updates.
     */
    @Transactional
    public void processStopLossTriggers(String symbol, BigDecimal latestPrice) {
        List<BookOrder> triggered = matchingEngine.evaluateStopLossTriggers(symbol, latestPrice);
        for (BookOrder bo : triggered) {
            orderRepository.findById(bo.getOrderId()).ifPresent(order -> {
                log.info("Stop-loss triggered for order #{} on {}", order.getId(), symbol);
                notificationProvider.ifAvailable(ns -> {
                    ns.sendNotification(order.getUser().getId(), "Stop-Loss Activated", "Stop-loss triggered for order #" + order.getId() + " at $" + latestPrice, "STOP_LOSS_ACTIVATED", "stop-act-" + order.getId(), order.getId(), null, "/orders");
                });
                auditProvider.ifAvailable(as -> {
                    as.recordAudit(order.getUser().getId(), order.getUser().getUsername(), "STOP_LOSS_TRIGGERED", "orders/" + order.getId(), "Order", String.valueOf(order.getId()), "ORD-" + order.getId(), "SUCCESS", "Stop loss activated at price $" + latestPrice, null);
                });

                // Convert to MARKET order and execute
                bo.setPrice(latestPrice);
                MatchResult result = matchingEngine.match(bo, latestPrice);
                for (TradeExecution exec : result.getTrades()) {
                    settleTrade(order, exec, order.getStock());
                }
                if (order.getFilledQuantity().compareTo(order.getQuantity()) >= 0) {
                    order.setStatus(OrderStatus.FILLED.name());
                } else if (order.getFilledQuantity().compareTo(BigDecimal.ZERO) > 0) {
                    order.setStatus(OrderStatus.PARTIALLY_FILLED.name());
                }
                orderRepository.save(order);
                recordOrderEvent(order.getId(), "STOP_TRIGGERED", "Stop loss executed at $" + latestPrice);
            });
        }
    }

    public List<Order> getOrders(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    public List<Order> getOpenOrders(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        return orderRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(user.getId(), List.of("OPEN", "PARTIALLY_FILLED", "NEW"));
    }

    public List<Order> getCompletedOrders(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        return orderRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(user.getId(), List.of("FILLED", "EXECUTED"));
    }

    public List<Order> getRejectedOrders(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        return orderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), "REJECTED");
    }

    public OrderDetailDto getOrderDetail(String username, Long orderId) {
        User user = userRepository.findByUsername(username).orElseThrow();
        Order order = orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        List<Trade> trades = tradeRepository.findByOrderIdOrderByExecutedAtDesc(orderId);
        List<TradeDto> tradeDtos = trades.stream().map(t -> {
            TradeDto dto = new TradeDto();
            dto.setId(t.getId());
            dto.setTradeId(t.getTradeId());
            dto.setOrderId(order.getId());
            dto.setSymbol(order.getStock().getSymbol());
            dto.setSide(t.getSide());
            dto.setQuantity(t.getQuantity());
            dto.setPrice(t.getExecutionPrice());
            dto.setTotalValue(t.getTotalValue());
            dto.setExecutedAt(t.getExecutedAt());
            return dto;
        }).collect(Collectors.toList());

        return new OrderDetailDto(order, tradeDtos);
    }

    public List<Trade> getTrades(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        return tradeRepository.findByUserIdOrderByExecutedAtDesc(user.getId());
    }

    public List<Holding> getHoldings(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        return holdingRepository.findByUserId(user.getId());
    }

    public OrderBookSnapshot getOrderBookDepth(String symbol, int depth) {
        return matchingEngine.getDepth(symbol, depth);
    }

    private void recordOrderEvent(Long orderId, String eventType, String payload) {
        try {
            OrderEvent event = new OrderEvent(orderId, eventType, payload, "gateway-node", System.currentTimeMillis());
            orderEventRepository.save(event);
        } catch (Exception e) {
            log.debug("Could not record order event: {}", e.getMessage());
        }
    }
}
