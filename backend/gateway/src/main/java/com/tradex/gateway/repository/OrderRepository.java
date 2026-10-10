package com.tradex.gateway.repository;

import com.tradex.gateway.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Order> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);

    List<Order> findByUserIdAndStatusInOrderByCreatedAtDesc(Long userId, Collection<String> statuses);

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT o FROM Order o JOIN FETCH o.user JOIN FETCH o.stock WHERE o.status IN ('OPEN', 'PARTIALLY_FILLED', 'NEW') ORDER BY o.sequenceNumber ASC")
    List<Order> findActiveRestingOrders();

    @Query("SELECT o FROM Order o WHERE o.stock.symbol = :symbol AND o.status IN ('OPEN', 'PARTIALLY_FILLED') ORDER BY o.sequenceNumber ASC")
    List<Order> findActiveOrdersBySymbol(@Param("symbol") String symbol);
}
