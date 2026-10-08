package com.tradex.gateway.repository;

import com.tradex.gateway.entity.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TradeRepository extends JpaRepository<Trade, Long> {
    List<Trade> findByUserIdOrderByExecutedAtDesc(Long userId);
}
