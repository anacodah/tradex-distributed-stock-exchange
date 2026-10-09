package com.tradex.common.repository;

import com.tradex.common.entity.AuditLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findTop100ByOrderByCreatedAtDesc();

    List<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<AuditLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<AuditLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<AuditLog> findByCorrelationIdOrderByCreatedAtDesc(String correlationId);

    List<AuditLog> findByActionOrderByCreatedAtDesc(String action);
}
