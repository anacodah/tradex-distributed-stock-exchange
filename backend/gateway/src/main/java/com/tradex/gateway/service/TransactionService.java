package com.tradex.gateway.service;

import com.tradex.common.entity.OrderEvent;
import com.tradex.common.repository.OrderEventRepository;
import com.tradex.gateway.dto.UnifiedTransactionDto;
import com.tradex.gateway.entity.*;
import com.tradex.gateway.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

@Service
public class TransactionService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final TradeRepository tradeRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTxRepository;
    private final OrderEventRepository orderEventRepository;

    public TransactionService(UserRepository userRepository,
                              OrderRepository orderRepository,
                              TradeRepository tradeRepository,
                              WalletRepository walletRepository,
                              WalletTransactionRepository walletTxRepository,
                              OrderEventRepository orderEventRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.tradeRepository = tradeRepository;
        this.walletRepository = walletRepository;
        this.walletTxRepository = walletTxRepository;
        this.orderEventRepository = orderEventRepository;
    }

    @Transactional(readOnly = true)
    public List<UnifiedTransactionDto> getUnifiedHistory(String username, String filterType) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        List<UnifiedTransactionDto> list = new ArrayList<>();

        // 1. Order submissions and lifecycle
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        for (Order o : orders) {
            UnifiedTransactionDto dto = new UnifiedTransactionDto();
            dto.setId("TX-ORD-" + o.getId());
            dto.setType(determineOrderTxType(o.getStatus()));
            dto.setTimestamp(o.getCreatedAt().atZone(ZoneId.systemDefault()));
            dto.setUserId(user.getId());
            dto.setUsername(user.getUsername());
            dto.setSymbol(o.getStock().getSymbol());
            dto.setOrderId(o.getId());
            dto.setCorrelationId("ORD-" + o.getId());
            dto.setQuantity(o.getQuantity());
            dto.setPrice(o.getPrice());
            dto.setDescription(o.getSide() + " " + o.getQuantity() + " " + o.getStock().getSymbol() + " [" + o.getOrderType() + "]");
            dto.setStatus(o.getStatus());
            list.add(dto);
        }

        // 2. Executed trades
        List<Trade> trades = tradeRepository.findByUserIdOrderByExecutedAtDesc(user.getId());
        for (Trade t : trades) {
            UnifiedTransactionDto dto = new UnifiedTransactionDto();
            dto.setId("TX-TRD-" + (t.getTradeId() != null ? t.getTradeId() : t.getId()));
            dto.setType("TRADE_EXECUTION");
            dto.setTimestamp(t.getExecutedAt().atZone(ZoneId.systemDefault()));
            dto.setUserId(user.getId());
            dto.setUsername(user.getUsername());
            dto.setSymbol(t.getStock().getSymbol());
            dto.setOrderId(t.getOrder() != null ? t.getOrder().getId() : null);
            dto.setTradeId(t.getTradeId());
            dto.setCorrelationId("ORD-" + (t.getOrder() != null ? t.getOrder().getId() : "NA"));
            dto.setQuantity(t.getQuantity());
            dto.setPrice(t.getExecutionPrice());
            dto.setAmount(t.getTotalValue());
            dto.setDescription("Executed " + t.getSide() + " " + t.getQuantity() + " " + t.getStock().getSymbol() + " @ $" + t.getExecutionPrice());
            dto.setStatus("EXECUTED");
            list.add(dto);
        }

        // 3. Wallet movements
        walletRepository.findByUserId(user.getId()).ifPresent(wallet -> {
            List<WalletTransaction> walletTxs = walletTxRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId());
            for (WalletTransaction wtx : walletTxs) {
                UnifiedTransactionDto dto = new UnifiedTransactionDto();
                dto.setId("TX-WAL-" + wtx.getId());
                dto.setType(mapWalletTxType(wtx.getType()));
                dto.setTimestamp(wtx.getCreatedAt().atZone(ZoneId.systemDefault()));
                dto.setUserId(user.getId());
                dto.setUsername(user.getUsername());
                dto.setOrderId(wtx.getReferenceId());
                dto.setCorrelationId(wtx.getCorrelationId() != null ? wtx.getCorrelationId() : "WAL-" + wtx.getId());
                dto.setAmount(wtx.getAmount());
                dto.setDescription(wtx.getDescription());
                dto.setStatus("COMPLETED");
                list.add(dto);
            }
        });

        // Filter if requested
        if (filterType != null && !filterType.isBlank() && !filterType.equalsIgnoreCase("ALL")) {
            list.removeIf(item -> !item.getType().equalsIgnoreCase(filterType.trim()));
        }

        // Sort descending by timestamp
        list.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
        return list;
    }

    /**
     * Creates an auditable compensating entry for financial corrections without deleting past records.
     */
    @Transactional
    public WalletTransaction postCompensatingEntry(String username, BigDecimal adjustmentAmount, String reason, String originalCorrelationId) {
        User user = userRepository.findByUsername(username).orElseThrow();
        Wallet wallet = walletRepository.findByUserIdForUpdate(user.getId()).orElseThrow();

        BigDecimal balBefore = wallet.getBalance();
        boolean isCredit = adjustmentAmount.compareTo(BigDecimal.ZERO) >= 0;
        BigDecimal absAmount = adjustmentAmount.abs();

        if (isCredit) {
            wallet.setBalance(wallet.getBalance().add(absAmount));
            wallet.setAvailableBalance(wallet.getAvailableBalance().add(absAmount));
        } else {
            wallet.setBalance(wallet.getBalance().subtract(absAmount));
            wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(absAmount));
        }
        walletRepository.save(wallet);

        WalletTransaction tx = new WalletTransaction();
        tx.setWallet(wallet);
        tx.setType(isCredit ? "COMPENSATING_CREDIT" : "COMPENSATING_DEBIT");
        tx.setAmount(absAmount);
        tx.setBalanceBefore(balBefore);
        tx.setBalanceAfter(wallet.getBalance());
        tx.setDescription("Compensating entry: " + reason);
        tx.setCorrelationId(originalCorrelationId);
        tx.setStatus("COMPLETED");
        return walletTxRepository.save(tx);
    }

    private String determineOrderTxType(String status) {
        if ("CANCELLED".equalsIgnoreCase(status)) return "ORDER_CANCEL";
        if ("REJECTED".equalsIgnoreCase(status)) return "ORDER_REJECTED";
        if ("MODIFIED".equalsIgnoreCase(status)) return "ORDER_MODIFY";
        return "ORDER_SUBMISSION";
    }

    private String mapWalletTxType(String type) {
        if ("TRADE_DEBIT".equalsIgnoreCase(type)) return "WALLET_DEBIT";
        if ("TRADE_CREDIT".equalsIgnoreCase(type)) return "WALLET_CREDIT";
        if ("DEPOSIT".equalsIgnoreCase(type)) return "WALLET_CREDIT";
        if ("WITHDRAWAL".equalsIgnoreCase(type)) return "WALLET_DEBIT";
        return type;
    }
}
