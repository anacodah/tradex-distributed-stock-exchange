package com.tradex.common.repository;

import com.tradex.common.entity.ReplicationLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReplicationLogRepository extends JpaRepository<ReplicationLogEntry, Long> {
    Optional<ReplicationLogEntry> findByLeaderEpochAndSequenceNumber(Long leaderEpoch, Long sequenceNumber);
    List<ReplicationLogEntry> findByLeaderEpochOrderBySequenceNumberAsc(Long leaderEpoch);
}
