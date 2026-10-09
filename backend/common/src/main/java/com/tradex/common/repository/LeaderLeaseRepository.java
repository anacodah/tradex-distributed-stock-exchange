package com.tradex.common.repository;

import com.tradex.common.entity.LeaderLease;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LeaderLeaseRepository extends JpaRepository<LeaderLease, Integer> {
    Optional<LeaderLease> findById(Integer id);
}
