package com.tradex.gateway.service;

import com.tradex.gateway.entity.*;
import com.tradex.gateway.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class TradingService {

    private final OrderRepository orderRepository;
    private final TradeRepository tradeRepository;
    private final HoldingRepository holdingRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTxRepository;
    private final StockRepository stockRepository;
    private final UserRepository userRepository;

    public TradingService(OrderRepository orderRepository,
                          TradeRepository tradeRepository,
                          HoldingRepository holdingRepository,
                          WalletRepository walletRepository,
                          WalletTransactionRepository walletTxRepository,
                          StockRepository stockRepository,
                          UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.tradeRepository = tradeRepository;
        this.holdingRepository = holdingRepository;
        this.walletRepository = walletRepository;
        this.walletTxRepository = walletTxRepository;
        this.stockRepository = stockRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Order placeOrder(String username, String symbol, String side, String orderType,
                            BigDecimal quantity, BigDecimal price, BigDecimal stopPrice) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Stock stock = stockRepository.findBySymbol(symbol.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Stock not found: " + symbol));
        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Wallet not found. Please deposit funds first."));

        // Create the order record
        Order order = new Order();
        order.setUser(user);
        order.setStock(stock);
        order.setSide(side.toUpperCase());
        order.setOrderType(orderType.toUpperCase());
        order.setQuantity(quantity);
        order.setPrice(price);
        order.setStopPrice(stopPrice);
        order.setStatus("PENDING");
        order = orderRepository.save(order);

        // Attempt execution based on type
        if ("MARKET".equals(orderType.toUpperCase())) {
            executeOrder(order, stock, user, wallet, stock.getCurrentPrice());
        } else if ("LIMIT".equals(orderType.toUpperCase())) {
            // Execute immediately if condition satisfied
            if ("BUY".equals(side.toUpperCase()) && price != null && stock.getCurrentPrice().compareTo(price) <= 0) {
                executeOrder(order, stock, user, wallet, stock.getCurrentPrice());
            } else if ("SELL".equals(side.toUpperCase()) && price != null && stock.getCurrentPrice().compareTo(price) >= 0) {
                executeOrder(order, stock, user, wallet, stock.getCurrentPrice());
            }
            // Otherwise leave PENDING
        }
        // STOP_LOSS stays PENDING until triggered

        return orderRepository.save(order);
    }

    private void executeOrder(Order order, Stock stock, User user, Wallet wallet, BigDecimal execPrice) {
        BigDecimal totalValue = execPrice.multiply(order.getQuantity()).setScale(4, RoundingMode.HALF_UP);

        if ("BUY".equals(order.getSide())) {
            // Check sufficient funds
            if (wallet.getAvailableBalance().compareTo(totalValue) < 0) {
                order.setStatus("REJECTED");
                throw new RuntimeException("Insufficient funds. Required: $" + totalValue + ", Available: $" + wallet.getAvailableBalance());
            }
            // Deduct cash
            BigDecimal balBefore = wallet.getBalance();
            wallet.setBalance(wallet.getBalance().subtract(totalValue));
            wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(totalValue));
            walletRepository.save(wallet);

            WalletTransaction tx = new WalletTransaction();
            tx.setWallet(wallet);
            tx.setType("TRADE_DEBIT");
            tx.setAmount(totalValue);
            tx.setBalanceBefore(balBefore);
            tx.setBalanceAfter(wallet.getBalance());
            tx.setDescription("BUY " + order.getQuantity() + " " + stock.getSymbol() + " @ $" + execPrice);
            tx.setReferenceId(order.getId());
            walletTxRepository.save(tx);

            // Update holding
            Holding holding = holdingRepository.findByUserIdAndStockId(user.getId(), stock.getId())
                    .orElseGet(() -> {
                        Holding h = new Holding();
                        h.setUser(user);
                        h.setStock(stock);
                        h.setQuantity(BigDecimal.ZERO);
                        h.setAveragePrice(BigDecimal.ZERO);
                        h.setRealizedPnl(BigDecimal.ZERO);
                        return h;
                    });

            // Weighted average price
            BigDecimal totalQty = holding.getQuantity().add(order.getQuantity());
            BigDecimal totalCost = holding.getQuantity().multiply(holding.getAveragePrice())
                    .add(order.getQuantity().multiply(execPrice));
            holding.setAveragePrice(totalCost.divide(totalQty, 4, RoundingMode.HALF_UP));
            holding.setQuantity(totalQty);
            holdingRepository.save(holding);

        } else if ("SELL".equals(order.getSide())) {
            // Check sufficient shares
            Holding holding = holdingRepository.findByUserIdAndStockId(user.getId(), stock.getId())
                    .orElseThrow(() -> new RuntimeException("No holding found for " + stock.getSymbol()));
            if (holding.getQuantity().compareTo(order.getQuantity()) < 0) {
                order.setStatus("REJECTED");
                throw new RuntimeException("Insufficient shares. Owned: " + holding.getQuantity() + ", Selling: " + order.getQuantity());
            }

            // Realized P&L
            BigDecimal costBasis = holding.getAveragePrice().multiply(order.getQuantity());
            BigDecimal proceeds = execPrice.multiply(order.getQuantity());
            BigDecimal realizedGain = proceeds.subtract(costBasis);
            holding.setRealizedPnl(holding.getRealizedPnl().add(realizedGain));
            holding.setQuantity(holding.getQuantity().subtract(order.getQuantity()));
            if (holding.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
                holdingRepository.delete(holding);
            } else {
                holdingRepository.save(holding);
            }

            // Add cash
            BigDecimal balBefore = wallet.getBalance();
            wallet.setBalance(wallet.getBalance().add(totalValue));
            wallet.setAvailableBalance(wallet.getAvailableBalance().add(totalValue));
            walletRepository.save(wallet);

            WalletTransaction tx = new WalletTransaction();
            tx.setWallet(wallet);
            tx.setType("TRADE_CREDIT");
            tx.setAmount(totalValue);
            tx.setBalanceBefore(balBefore);
            tx.setBalanceAfter(wallet.getBalance());
            tx.setDescription("SELL " + order.getQuantity() + " " + stock.getSymbol() + " @ $" + execPrice);
            tx.setReferenceId(order.getId());
            walletTxRepository.save(tx);
        }

        // Create trade record
        Trade trade = new Trade();
        trade.setUser(user);
        trade.setStock(stock);
        trade.setOrder(order);
        trade.setSide(order.getSide());
        trade.setQuantity(order.getQuantity());
        trade.setExecutionPrice(execPrice);
        trade.setTotalValue(totalValue);
        tradeRepository.save(trade);

        order.setStatus("EXECUTED");
        order.setExecutionPrice(execPrice);
    }

    public List<Order> getOrders(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    @Transactional
    public Order cancelOrder(String username, Long orderId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Order order = orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (!"PENDING".equals(order.getStatus())) {
            throw new RuntimeException("Only PENDING orders can be cancelled");
        }
        order.setStatus("CANCELLED");
        return orderRepository.save(order);
    }

    public List<Trade> getTrades(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return tradeRepository.findByUserIdOrderByExecutedAtDesc(user.getId());
    }

    public List<Holding> getHoldings(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return holdingRepository.findByUserId(user.getId());
    }
}
