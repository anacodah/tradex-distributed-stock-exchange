package com.tradex.gateway;

import com.tradex.common.engine.*;
import com.tradex.gateway.dto.OrderRequest;
import com.tradex.gateway.entity.*;
import com.tradex.gateway.repository.*;
import com.tradex.gateway.service.TradingService;
import com.tradex.common.repository.IdempotencyRecordRepository;
import com.tradex.common.repository.OrderEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class Phase4OrderAndMatchingEngineTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private TradeRepository tradeRepository;

    @Mock
    private HoldingRepository holdingRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletTransactionRepository walletTxRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderEventRepository orderEventRepository;

    @Mock
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @InjectMocks
    private TradingService tradingService;

    private MatchingEngine matchingEngine;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        matchingEngine = new MatchingEngine();
    }

    @Test
    void testPriceTimePriorityMatching() {
        // Setup Bids (Buyers):
        // Buyer 1: $100, seq 100
        // Buyer 2: $102, seq 101 (higher price, higher priority)
        // Buyer 3: $102, seq 102 (same price as Buyer 2, but later sequence)
        BookOrder b1 = new BookOrder(1L, 10L, "AAPL", OrderSide.BUY, OrderType.LIMIT,
                new BigDecimal("100.00"), null, new BigDecimal("10"), BigDecimal.ZERO, 100L, ZonedDateTime.now());
        BookOrder b2 = new BookOrder(2L, 11L, "AAPL", OrderSide.BUY, OrderType.LIMIT,
                new BigDecimal("102.00"), null, new BigDecimal("5"), BigDecimal.ZERO, 101L, ZonedDateTime.now());
        BookOrder b3 = new BookOrder(3L, 12L, "AAPL", OrderSide.BUY, OrderType.LIMIT,
                new BigDecimal("102.00"), null, new BigDecimal("5"), BigDecimal.ZERO, 102L, ZonedDateTime.now());

        matchingEngine.getOrCreateBook("AAPL").addOrder(b1);
        matchingEngine.getOrCreateBook("AAPL").addOrder(b2);
        matchingEngine.getOrCreateBook("AAPL").addOrder(b3);

        // Seller enters with Market Sell of 7 shares
        BookOrder seller = new BookOrder(4L, 20L, "AAPL", OrderSide.SELL, OrderType.MARKET,
                null, null, new BigDecimal("7"), BigDecimal.ZERO, 200L, ZonedDateTime.now());

        MatchResult result = matchingEngine.match(seller, new BigDecimal("102.00"));

        assertEquals(2, result.getTrades().size(), "Should match against b2 and b3 (highest price)");
        // First trade: against b2 (5 shares @ 102.00)
        assertEquals(new BigDecimal("5"), result.getTrades().get(0).getQuantity());
        assertEquals(2L, result.getTrades().get(0).getMakerOrderId());
        assertEquals(new BigDecimal("102.00"), result.getTrades().get(0).getPrice());

        // Second trade: against b3 (2 shares @ 102.00, partial fill of b3)
        assertEquals(new BigDecimal("2"), result.getTrades().get(1).getQuantity());
        assertEquals(3L, result.getTrades().get(1).getMakerOrderId());
        assertEquals(OrderStatus.FILLED, result.getFinalStatus());
        assertEquals(new BigDecimal("3"), b3.getRemainingQuantity());
    }

    @Test
    void testPartialFillsAndRemainingRestingLimitOrder() {
        // Resting Ask (Seller): 10 shares @ $150
        BookOrder ask = new BookOrder(101L, 30L, "TSLA", OrderSide.SELL, OrderType.LIMIT,
                new BigDecimal("150.00"), null, new BigDecimal("10"), BigDecimal.ZERO, 50L, ZonedDateTime.now());
        matchingEngine.getOrCreateBook("TSLA").addOrder(ask);

        // Buyer wants 25 shares @ $150
        BookOrder buyer = new BookOrder(102L, 31L, "TSLA", OrderSide.BUY, OrderType.LIMIT,
                new BigDecimal("150.00"), null, new BigDecimal("25"), BigDecimal.ZERO, 60L, ZonedDateTime.now());

        MatchResult result = matchingEngine.match(buyer, new BigDecimal("150.00"));

        assertEquals(1, result.getTrades().size());
        assertEquals(new BigDecimal("10"), result.getTrades().get(0).getQuantity());
        assertEquals(OrderStatus.PARTIALLY_FILLED, result.getFinalStatus());
        assertTrue(result.isRestedInBook());
        assertEquals(new BigDecimal("15"), buyer.getRemainingQuantity());
        assertTrue(ask.isFullyFilled());
    }

    @Test
    void testMarketOrderInsufficientLiquidityNeverPromisesFills() {
        // Empty book (no resting asks)
        BookOrder buyer = new BookOrder(201L, 40L, "GOOGL", OrderSide.BUY, OrderType.MARKET,
                null, null, new BigDecimal("10"), BigDecimal.ZERO, 70L, ZonedDateTime.now());

        MatchResult result = matchingEngine.match(buyer, new BigDecimal("180.00"));

        assertEquals(0, result.getTrades().size());
        assertEquals(OrderStatus.REJECTED, result.getFinalStatus());
        assertFalse(result.isRestedInBook());
        assertEquals(BigDecimal.ZERO, buyer.getFilledQuantity());
    }

    @Test
    void testStopLossTriggerSemantics() {
        // Resting Stop Loss SELL order with stopPrice = $140.00
        BookOrder stopSell = new BookOrder(301L, 50L, "AAPL", OrderSide.SELL, OrderType.STOP_LOSS,
                null, new BigDecimal("140.00"), new BigDecimal("15"), BigDecimal.ZERO, 80L, ZonedDateTime.now());

        matchingEngine.match(stopSell, new BigDecimal("145.00"));

        // Price at $142: Should NOT trigger
        List<BookOrder> triggered1 = matchingEngine.evaluateStopLossTriggers("AAPL", new BigDecimal("142.00"));
        assertTrue(triggered1.isEmpty());

        // Price drops to $139.50: Should trigger
        List<BookOrder> triggered2 = matchingEngine.evaluateStopLossTriggers("AAPL", new BigDecimal("139.50"));
        assertEquals(1, triggered2.size());
        assertEquals(301L, triggered2.get(0).getOrderId());
    }

    @Test
    void testOrderModificationLossOfTimePriority() {
        OrderBook book = matchingEngine.getOrCreateBook("NVDA");

        BookOrder o1 = new BookOrder(401L, 60L, "NVDA", OrderSide.BUY, OrderType.LIMIT,
                new BigDecimal("500.00"), null, new BigDecimal("10"), BigDecimal.ZERO, 1L, ZonedDateTime.now());
        BookOrder o2 = new BookOrder(402L, 61L, "NVDA", OrderSide.BUY, OrderType.LIMIT,
                new BigDecimal("500.00"), null, new BigDecimal("10"), BigDecimal.ZERO, 2L, ZonedDateTime.now());

        book.addOrder(o1);
        book.addOrder(o2);

        // Before modification: o1 has earlier sequence number (1 < 2)
        assertEquals(o1, book.getBids().first());

        // Modify o1 with new price (or higher quantity) -> receives sequence 999
        matchingEngine.modifyOrder("NVDA", 401L, new BigDecimal("500.00"), new BigDecimal("15"), 999L);

        // Now o2 has earlier sequence (2 < 999), so o2 must be prioritized over modified o1
        assertEquals(402L, book.getBids().first().getOrderId());
    }

    @Test
    void testOrderStatusValidTransitions() {
        // Valid transitions
        assertTrue(OrderStatus.isValidTransition(OrderStatus.NEW, OrderStatus.OPEN));
        assertTrue(OrderStatus.isValidTransition(OrderStatus.OPEN, OrderStatus.PARTIALLY_FILLED));
        assertTrue(OrderStatus.isValidTransition(OrderStatus.PARTIALLY_FILLED, OrderStatus.FILLED));
        assertTrue(OrderStatus.isValidTransition(OrderStatus.OPEN, OrderStatus.CANCELLED));

        // Invalid transitions: cannot cancel a FILLED order
        assertFalse(OrderStatus.isValidTransition(OrderStatus.FILLED, OrderStatus.CANCELLED));
        // Cannot modify or transition a REJECTED or CANCELLED order back to OPEN
        assertFalse(OrderStatus.isValidTransition(OrderStatus.REJECTED, OrderStatus.OPEN));
        assertFalse(OrderStatus.isValidTransition(OrderStatus.CANCELLED, OrderStatus.OPEN));
    }

    @Test
    void testInsufficientFundsRejectionInTradingService() {
        User user = new User();
        user.setId(1L);
        user.setUsername("bob");

        Stock stock = new Stock();
        stock.setId(10L);
        stock.setSymbol("MSFT");
        stock.setCurrentPrice(new BigDecimal("400.00"));

        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(new BigDecimal("100.00"));
        wallet.setAvailableBalance(new BigDecimal("100.00"));
        wallet.setReservedBalance(BigDecimal.ZERO);

        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(user));
        when(stockRepository.findBySymbol("MSFT")).thenReturn(Optional.of(stock));
        when(walletRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.of(wallet));

        OrderRequest req = new OrderRequest();
        req.setSymbol("MSFT");
        req.setSide("BUY");
        req.setOrderType("LIMIT");
        req.setQuantity(new BigDecimal("5")); // 5 * 400 = 2000 required, only 100 available
        req.setPrice(new BigDecimal("400.00"));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                tradingService.placeOrder("bob", req, null)
        );

        assertTrue(ex.getMessage().contains("Insufficient funds"));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void testInsufficientHoldingsRejectionInTradingService() {
        User user = new User();
        user.setId(2L);
        user.setUsername("carol");

        Stock stock = new Stock();
        stock.setId(11L);
        stock.setSymbol("AMZN");
        stock.setCurrentPrice(new BigDecimal("180.00"));

        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(new BigDecimal("5000.00"));
        wallet.setAvailableBalance(new BigDecimal("5000.00"));

        Holding holding = new Holding();
        holding.setUser(user);
        holding.setStock(stock);
        holding.setQuantity(new BigDecimal("5")); // Carol only owns 5 shares

        when(userRepository.findByUsername("carol")).thenReturn(Optional.of(user));
        when(stockRepository.findBySymbol("AMZN")).thenReturn(Optional.of(stock));
        when(walletRepository.findByUserIdForUpdate(2L)).thenReturn(Optional.of(wallet));
        when(holdingRepository.findByUserIdAndStockIdForUpdate(2L, 11L)).thenReturn(Optional.of(holding));

        OrderRequest req = new OrderRequest();
        req.setSymbol("AMZN");
        req.setSide("SELL");
        req.setOrderType("LIMIT");
        req.setQuantity(new BigDecimal("10")); // Trying to sell 10 shares
        req.setPrice(new BigDecimal("185.00"));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                tradingService.placeOrder("carol", req, null)
        );

        assertTrue(ex.getMessage().contains("Insufficient shares"));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void testIdempotentDuplicateOrderSubmission() {
        Order existing = new Order();
        existing.setId(999L);
        existing.setIdempotencyKey("dup-key-123");
        existing.setStatus("OPEN");

        when(orderRepository.findByIdempotencyKey("dup-key-123")).thenReturn(Optional.of(existing));

        OrderRequest req = new OrderRequest();
        req.setSymbol("AAPL");
        req.setSide("BUY");
        req.setQuantity(new BigDecimal("10"));

        Order result = tradingService.placeOrder("alice", req, "dup-key-123");

        assertEquals(999L, result.getId());
        verify(walletRepository, never()).findByUserIdForUpdate(any());
    }
}
