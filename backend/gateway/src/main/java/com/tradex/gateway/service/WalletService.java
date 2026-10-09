package com.tradex.gateway.service;

import com.tradex.gateway.entity.User;
import com.tradex.gateway.entity.Wallet;
import com.tradex.gateway.entity.WalletTransaction;
import com.tradex.gateway.repository.UserRepository;
import com.tradex.gateway.repository.WalletRepository;
import com.tradex.gateway.repository.WalletTransactionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class WalletService {

    @Value("${tradex.initial.wallet.balance:10000.00}")
    private BigDecimal initialBalance;

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository txRepository;
    private final UserRepository userRepository;

    public WalletService(WalletRepository walletRepository,
                          WalletTransactionRepository txRepository,
                          UserRepository userRepository) {
        this.walletRepository = walletRepository;
        this.txRepository = txRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Wallet getOrCreateWallet(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return walletRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    BigDecimal initBal = initialBalance != null ? initialBalance : new BigDecimal("10000.00");
                    Wallet w = new Wallet();
                    w.setUser(user);
                    w.setBalance(initBal.setScale(4, RoundingMode.HALF_UP));
                    w.setAvailableBalance(initBal.setScale(4, RoundingMode.HALF_UP));
                    w.setReservedBalance(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
                    w = walletRepository.save(w);

                    if (initBal.compareTo(BigDecimal.ZERO) > 0) {
                        WalletTransaction tx = new WalletTransaction();
                        tx.setWallet(w);
                        tx.setType("DEPOSIT");
                        tx.setAmount(initBal);
                        tx.setBalanceBefore(BigDecimal.ZERO);
                        tx.setBalanceAfter(initBal);
                        tx.setDescription("Initial virtual paper-trading funds");
                        txRepository.save(tx);
                    }
                    return w;
                });
    }

    @Transactional
    public Wallet deposit(String username, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Deposit amount must be positive");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        Wallet wallet = walletRepository.findByUserIdForUpdate(user.getId())
                .orElseGet(() -> getOrCreateWallet(username));

        BigDecimal before = wallet.getBalance();
        BigDecimal after = before.add(amount).setScale(4, RoundingMode.HALF_UP);
        wallet.setBalance(after);
        wallet.setAvailableBalance(wallet.getAvailableBalance().add(amount).setScale(4, RoundingMode.HALF_UP));
        walletRepository.save(wallet);

        WalletTransaction tx = new WalletTransaction();
        tx.setWallet(wallet);
        tx.setType("DEPOSIT");
        tx.setAmount(amount.setScale(4, RoundingMode.HALF_UP));
        tx.setBalanceBefore(before);
        tx.setBalanceAfter(after);
        tx.setDescription("Virtual simulator cash deposit");
        txRepository.save(tx);

        return wallet;
    }

    @Transactional
    public Wallet withdraw(String username, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Withdrawal amount must be positive");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        Wallet wallet = walletRepository.findByUserIdForUpdate(user.getId())
                .orElseThrow(() -> new RuntimeException("Wallet not found for user: " + username));

        if (wallet.getAvailableBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient available balance. Available: $" + wallet.getAvailableBalance() + ", Requested: $" + amount);
        }

        BigDecimal before = wallet.getBalance();
        BigDecimal after = before.subtract(amount).setScale(4, RoundingMode.HALF_UP);
        wallet.setBalance(after);
        wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(amount).setScale(4, RoundingMode.HALF_UP));
        walletRepository.save(wallet);

        WalletTransaction tx = new WalletTransaction();
        tx.setWallet(wallet);
        tx.setType("WITHDRAWAL");
        tx.setAmount(amount.setScale(4, RoundingMode.HALF_UP));
        tx.setBalanceBefore(before);
        tx.setBalanceAfter(after);
        tx.setDescription("Virtual simulator cash withdrawal");
        txRepository.save(tx);

        return wallet;
    }

    @Transactional
    public Wallet reserveFunds(String username, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return getOrCreateWallet(username);
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        Wallet wallet = walletRepository.findByUserIdForUpdate(user.getId())
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        if (wallet.getAvailableBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient available funds for pending order reservation. Available: $" + wallet.getAvailableBalance() + ", Required: $" + amount);
        }

        wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(amount).setScale(4, RoundingMode.HALF_UP));
        wallet.setReservedBalance(wallet.getReservedBalance().add(amount).setScale(4, RoundingMode.HALF_UP));
        return walletRepository.save(wallet);
    }

    @Transactional
    public Wallet releaseFunds(String username, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return getOrCreateWallet(username);
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        Wallet wallet = walletRepository.findByUserIdForUpdate(user.getId())
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        BigDecimal releaseQty = amount.min(wallet.getReservedBalance());
        wallet.setReservedBalance(wallet.getReservedBalance().subtract(releaseQty).setScale(4, RoundingMode.HALF_UP));
        wallet.setAvailableBalance(wallet.getAvailableBalance().add(releaseQty).setScale(4, RoundingMode.HALF_UP));
        return walletRepository.save(wallet);
    }

    public List<WalletTransaction> getTransactions(String username) {
        Wallet wallet = getOrCreateWallet(username);
        return txRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId());
    }
}
