package com.tradex.common.repository;

import com.tradex.common.entity.OrderEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderEventRepository extends JpaRepository<OrderEvent, Long> {
    List<OrderEvent> findByOrderIdOrderByCreatedAtAsc(Long orderId);
}
