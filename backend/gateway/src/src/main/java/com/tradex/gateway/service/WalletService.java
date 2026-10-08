package com.tradex.gateway.service;

import com.tradex.gateway.entity.User;
import com.tradex.gateway.entity.Wallet;
import com.tradex.gateway.entity.WalletTransaction;
import com.tradex.gateway.repository.UserRepository;
import com.tradex.gateway.repository.WalletRepository;
import com.tradex.gateway.repository.WalletTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class WalletService {

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
                .orElseThrow(() -> new RuntimeException("User not found"));
        return walletRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Wallet w = new Wallet();
                    w.setUser(user);
                    w.setBalance(BigDecimal.ZERO);
                    w.setAvailableBalance(BigDecimal.ZERO);
                    w.setReservedBalance(BigDecimal.ZERO);
                    return walletRepository.save(w);
                });
    }

    @Transactional
    public Wallet deposit(String username, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Deposit amount must be positive");
        }
        Wallet wallet = getOrCreateWallet(username);
        BigDecimal before = wallet.getBalance();
        BigDecimal after = before.add(amount);
        wallet.setBalance(after);
        wallet.setAvailableBalance(wallet.getAvailableBalance().add(amount));
        walletRepository.save(wallet);

        WalletTransaction tx = new WalletTransaction();
        tx.setWallet(wallet);
        tx.setType("DEPOSIT");
        tx.setAmount(amount);
        tx.setBalanceBefore(before);
        tx.setBalanceAfter(after);
        tx.setDescription("Cash deposit");
        txRepository.save(tx);

        return wallet;
    }

    @Transactional
    public Wallet withdraw(String username, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Withdrawal amount must be positive");
        }
        Wallet wallet = getOrCreateWallet(username);
        if (wallet.getAvailableBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient available balance");
        }
        BigDecimal before = wallet.getBalance();
        BigDecimal after = before.subtract(amount);
        wallet.setBalance(after);
        wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(amount));
        walletRepository.save(wallet);

        WalletTransaction tx = new WalletTransaction();
        tx.setWallet(wallet);
        tx.setType("WITHDRAWAL");
        tx.setAmount(amount);
        tx.setBalanceBefore(before);
        tx.setBalanceAfter(after);
        tx.setDescription("Cash withdrawal");
        txRepository.save(tx);

        return wallet;
    }

    public List<WalletTransaction> getTransactions(String username) {
        Wallet wallet = getOrCreateWallet(username);
        return txRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId());
    }
}
