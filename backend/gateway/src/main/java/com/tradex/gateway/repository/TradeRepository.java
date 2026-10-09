package com.tradex.gateway.repository;

import com.tradex.gateway.entity.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TradeRepository extends JpaRepository<Trade, Long> {

    List<Trade> findByUserIdOrderByExecutedAtDesc(Long userId);

    List<Trade> findByOrderIdOrderByExecutedAtDesc(Long orderId);

    Optional<Trade> findByTradeId(String tradeId);

    List<Trade> findByStockSymbolOrderByExecutedAtDesc(String symbol);
}
