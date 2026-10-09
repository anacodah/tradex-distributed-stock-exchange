package com.tradex.common.entity;

import jakarta.persistence.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "leader_lease")
public class LeaderLease {

    @Id
    private Integer id = 1;

    @Column(name = "leader_name", nullable = false, length = 50)
    private String leaderName;

    @Column(name = "epoch", nullable = false)
    private Long epoch = 1L;

    @Column(name = "lease_expires_at", nullable = false)
    private ZonedDateTime leaseExpiresAt;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt = ZonedDateTime.now();

    public LeaderLease() {}

    public LeaderLease(String leaderName, Long epoch, ZonedDateTime leaseExpiresAt) {
        this.id = 1;
        this.leaderName = leaderName;
        this.epoch = epoch;
        this.leaseExpiresAt = leaseExpiresAt;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getLeaderName() { return leaderName; }
    public void setLeaderName(String leaderName) { this.leaderName = leaderName; }

    public Long getEpoch() { return epoch; }
    public void setEpoch(Long epoch) { this.epoch = epoch; }

    public ZonedDateTime getLeaseExpiresAt() { return leaseExpiresAt; }
    public void setLeaseExpiresAt(ZonedDateTime leaseExpiresAt) { this.leaseExpiresAt = leaseExpiresAt; }

    public ZonedDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(ZonedDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = ZonedDateTime.now();
    }
}
