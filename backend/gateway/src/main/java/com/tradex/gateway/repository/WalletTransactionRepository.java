package com.tradex.gateway.repository;

import com.tradex.gateway.entity.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    List<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(Long walletId);

    List<WalletTransaction> findByCorrelationIdOrderByCreatedAtDesc(String correlationId);

    Optional<WalletTransaction> findByIdempotencyKey(String idempotencyKey);
}
