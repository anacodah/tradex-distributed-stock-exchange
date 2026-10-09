package com.tradex.common.entity;

import jakarta.persistence.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "distributed_nodes")
public class DistributedNodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "node_name", nullable = false, unique = true, length = 50)
    private String nodeName;

    @Column(name = "host", nullable = false, length = 100)
    private String host;

    @Column(name = "port", nullable = false)
    private Integer port;

    @Column(name = "rmi_port")
    private Integer rmiPort;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "last_heartbeat")
    private ZonedDateTime lastHeartbeat;

    @Column(name = "is_leader")
    private Boolean isLeader = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt = ZonedDateTime.now();

    public DistributedNodeEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNodeName() { return nodeName; }
    public void setNodeName(String nodeName) { this.nodeName = nodeName; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public Integer getPort() { return port; }
    public void setPort(Integer port) { this.port = port; }

    public Integer getRmiPort() { return rmiPort; }
    public void setRmiPort(Integer rmiPort) { this.rmiPort = rmiPort; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public ZonedDateTime getLastHeartbeat() { return lastHeartbeat; }
    public void setLastHeartbeat(ZonedDateTime lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }

    public Boolean getIsLeader() { return isLeader; }
    public void setIsLeader(Boolean isLeader) { this.isLeader = isLeader; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }
}
