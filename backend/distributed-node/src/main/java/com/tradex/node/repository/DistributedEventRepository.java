package com.tradex.node.repository;

import com.tradex.node.entity.DistributedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DistributedEventRepository extends JpaRepository<DistributedEvent, Long> {
    List<DistributedEvent> findTop50ByOrderByWallClockTimeDesc();
    List<DistributedEvent> findByEventTypeOrderByWallClockTimeDesc(String eventType);
    List<DistributedEvent> findByNodeIdOrderByWallClockTimeDesc(String nodeId);
}
