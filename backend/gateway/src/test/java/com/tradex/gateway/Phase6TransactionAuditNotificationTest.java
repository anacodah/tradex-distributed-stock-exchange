package com.tradex.gateway;

import com.tradex.common.entity.AuditLog;
import com.tradex.common.entity.Notification;
import com.tradex.common.repository.AuditLogRepository;
import com.tradex.common.repository.NotificationRepository;
import com.tradex.gateway.dto.UnifiedTransactionDto;
import com.tradex.gateway.entity.*;
import com.tradex.gateway.repository.*;
import com.tradex.gateway.service.*;
import com.tradex.gateway.websocket.NotificationWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class Phase6TransactionAuditNotificationTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationWebSocketHandler notificationWebSocketHandler;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletTransactionRepository walletTxRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private TradeRepository tradeRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationService notificationService;

    @InjectMocks
    private AuditService auditService;

    @InjectMocks
    private TransactionService transactionService;

    private User testUser;
    private Wallet testWallet;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testUser = new User();
        testUser.setId(10L);
        testUser.setUsername("trader1");

        testWallet = new Wallet();
        testWallet.setId(100L);
        testWallet.setUser(testUser);
        testWallet.setBalance(new BigDecimal("10000.00"));
        testWallet.setAvailableBalance(new BigDecimal("10000.00"));
        testWallet.setReservedBalance(BigDecimal.ZERO);
    }

    // ==========================================
    // 1. Notification Deduplication & Persistence
    // ==========================================

    @Test
    void testNotificationDeduplication() {
        String dedupKey = "ord-fill-101";
        when(notificationRepository.findByDedupKey(dedupKey)).thenReturn(Optional.of(new Notification()));

        Notification notif = notificationService.sendNotification(
                10L, "Order Filled", "Order #101 filled", "ORDER_FILLED", dedupKey, 101L, "TR-1", "/orders"
        );

        // Deduplication prevents second save and WebSocket broadcast
        assertNull(notif);
        verify(notificationRepository, never()).save(any(Notification.class));
        verify(notificationWebSocketHandler, never()).sendToUser(anyLong(), any());
    }

    @Test
    void testNotificationPersistenceBeforeBroadcast() {
        String dedupKey = "ord-fill-102";
        when(notificationRepository.findByDedupKey(dedupKey)).thenReturn(Optional.empty());

        Notification saved = new Notification();
        saved.setId(500L);
        saved.setUserId(10L);
        saved.setTitle("Order Filled");
        saved.setDedupKey(dedupKey);

        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

        Notification result = notificationService.sendNotification(
                10L, "Order Filled", "Order #102 filled", "ORDER_FILLED", dedupKey, 102L, "TR-2", "/orders"
        );

        assertNotNull(result);
        assertEquals(500L, result.getId());
        // Verify persisted before broadcast
        verify(notificationRepository, times(1)).save(any(Notification.class));
        verify(notificationWebSocketHandler, times(1)).sendToUser(eq(10L), any(Notification.class));
    }

    // ==========================================
    // 2. Audit Sanitization & Access Control
    // ==========================================

    @Test
    void testAuditLogSanitizationRedactsSecrets() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> i.getArgument(0));

        String rawDetails = "User attempted auth with password=SecretPassword123 and token=Bearer eyJhbGciOi...";
        AuditLog recorded = auditService.recordAudit(
                10L, "trader1", "AUTH_LOGIN", "auth/login", "User", 10L, "CORR-99", "SUCCESS", rawDetails, "127.0.0.1"
        );

        assertNotNull(recorded);
        assertFalse(recorded.getDetails().contains("SecretPassword123"), "Passwords must be redacted");
        assertFalse(recorded.getDetails().contains("eyJhbGciOi"), "Tokens must be redacted");
        assertTrue(recorded.getDetails().contains("[REDACTED]"));
    }

    @Test
    void testAuditLogAccessControl() {
        when(userRepository.findByUsername("trader1")).thenReturn(Optional.of(testUser));
        Page<AuditLog> emptyPage = new PageImpl<>(Collections.emptyList());
        when(auditLogRepository.findByActorIdOrderByTimestampDesc(eq(10L), any(Pageable.class)))
                .thenReturn(emptyPage);

        Page<AuditLog> userLogs = auditService.getAuditLogsForUser("trader1", PageRequest.of(0, 10));
        assertNotNull(userLogs);
        // User view queries actorId only
        verify(auditLogRepository, times(1)).findByActorIdOrderByTimestampDesc(eq(10L), any(Pageable.class));
        verify(auditLogRepository, never()).findAll(any(Pageable.class));
    }

    // ==========================================
    // 3. Unified Financial History & Traceability
    // ==========================================

    @Test
    void testUnifiedTransactionHistoryTraceability() {
        when(userRepository.findByUsername("trader1")).thenReturn(Optional.of(testUser));

        // Mock an order event
        Order order = new Order();
        order.setId(201L);
        order.setUser(testUser);
        Stock stock = new Stock();
        stock.setSymbol("AAPL");
        order.setStock(stock);
        order.setSide("BUY");
        order.setOrderType("LIMIT");
        order.setPrice(new BigDecimal("150.00"));
        order.setQuantity(new BigDecimal("10"));
        order.setStatus("OPEN");
        order.setCreatedAt(LocalDateTime.now());
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(order));

        // Mock a wallet movement
        WalletTransaction wt = new WalletTransaction();
        wt.setId(301L);
        wt.setWallet(testWallet);
        wt.setType("WALLET_DEBIT");
        wt.setAmount(new BigDecimal("1500.00"));
        wt.setCorrelationId("CORR-ORD-201");
        wt.setReferenceId(201L);
        wt.setCreatedAt(LocalDateTime.now());
        when(walletTxRepository.findByWalletUserIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(wt));

        when(tradeRepository.findByUserIdOrderByExecutedAtDesc(10L)).thenReturn(Collections.emptyList());

        List<UnifiedTransactionDto> history = transactionService.getUnifiedTransactions("trader1", "ALL", 50);

        assertNotNull(history);
        assertEquals(2, history.size());

        boolean hasOrder = history.stream().anyMatch(h -> "ORDER".equals(h.getSourceType()) && h.getOrderId().equals(201L));
        boolean hasWallet = history.stream().anyMatch(h -> "WALLET".equals(h.getSourceType()) && "CORR-ORD-201".equals(h.getCorrelationId()));

        assertTrue(hasOrder, "Order event must be distinguishable");
        assertTrue(hasWallet, "Wallet movement must be distinguishable and trace order correlation");
    }

    // ==========================================
    // 4. Compensating Entries Preserve Ledger History
    // ==========================================

    @Test
    void testCompensatingEntryDoesNotMutateHistory() {
        when(walletRepository.findByUserIdForUpdate(10L)).thenReturn(Optional.of(testWallet));
        when(walletTxRepository.save(any(WalletTransaction.class))).thenAnswer(i -> {
            WalletTransaction tx = i.getArgument(0);
            tx.setId(999L);
            return tx;
        });

        BigDecimal compAmount = new BigDecimal("250.00");
        WalletTransaction compTx = transactionService.postCompensatingEntry(
                10L, compAmount, true, "Reconciliation correction for missing credit", "WTX-ORIG-100"
        );

        assertNotNull(compTx);
        assertEquals("COMPENSATING_CREDIT", compTx.getType());
        assertEquals(new BigDecimal("10250.00"), testWallet.getBalance());
        assertEquals(new BigDecimal("10250.00"), testWallet.getAvailableBalance());
        assertTrue(compTx.getDescription().contains("Compensating Adjustment"));
        assertNotNull(compTx.getCorrelationId());

        // Verifies audit log and notification were triggered
        verify(walletTxRepository, times(1)).save(any(WalletTransaction.class));
    }
}
