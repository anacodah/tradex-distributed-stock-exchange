package com.tradex.gateway.repository;

import com.tradex.gateway.entity.Holding;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HoldingRepository extends JpaRepository<Holding, Long> {

    List<Holding> findByUserId(Long userId);

    Optional<Holding> findByUserIdAndStockId(Long userId, Long stockId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM Holding h WHERE h.user.id = :userId AND h.stock.id = :stockId")
    Optional<Holding> findByUserIdAndStockIdForUpdate(@Param("userId") Long userId, @Param("stockId") Long stockId);

    Optional<Holding> findByUserIdAndStockSymbol(Long userId, String symbol);
}
