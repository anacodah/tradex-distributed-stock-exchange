package com.tradex.common.repository;

import com.tradex.common.entity.DistributedNodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DistributedNodeRepository extends JpaRepository<DistributedNodeEntity, Long> {
    Optional<DistributedNodeEntity> findByNodeName(String nodeName);
    Optional<DistributedNodeEntity> findByIsLeaderTrue();
}
