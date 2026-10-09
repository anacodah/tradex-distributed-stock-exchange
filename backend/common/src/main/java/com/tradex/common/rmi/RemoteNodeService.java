package com.tradex.common.rmi;

import com.tradex.common.rmi.dto.*;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface RemoteNodeService extends Remote {

    /**
     * Get node health, role (Leader/Follower), term epoch, capabilities, and worker-pool metrics.
     */
    NodeStatusDto getNodeStatus() throws RemoteException;

    /**
     * Execute or match an authoritative order request on this node (if Leader).
     */
    RemoteOrderResponseDto submitOrder(RemoteOrderRequestDto request) throws RemoteException;

    /**
     * Replicate a committed state change/event to this node.
     */
    boolean replicateEvent(ReplicationEventDto event) throws RemoteException;

    /**
     * Notify node of leader election, heartbeat or epoch change.
     */
    boolean updateLeaderEpoch(int leaderId, String leaderName, long termEpoch) throws RemoteException;

    /**
     * Exchange and advance logical clocks (Lamport/Vector).
     */
    long exchangeClock(ClockSyncDto clockSync) throws RemoteException;

    /**
     * Replicate a committed state change/event to this node and return structured ACK.
     */
    ReplicationAckDto replicateEventWithAck(ReplicationEventDto event) throws RemoteException;

    /**
     * Get replication lag, committed sequence, and cluster synchronization status.
     */
    ClusterReplicationStatusDto getReplicationStatus() throws RemoteException;

    /**
     * Request state catch-up / recovery events from sequence number.
     */
    List<ReplicationEventDto> syncState(long fromSequenceNumber) throws RemoteException;
}
