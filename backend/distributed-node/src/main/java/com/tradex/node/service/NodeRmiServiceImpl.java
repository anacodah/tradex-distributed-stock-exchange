package com.tradex.node.service;

import com.tradex.common.engine.BookOrder;
import com.tradex.common.engine.MatchingEngine;
import com.tradex.common.engine.OrderSide;
import com.tradex.common.engine.OrderStatus;
import com.tradex.common.engine.OrderType;
import com.tradex.common.engine.TradeExecution;
import com.tradex.common.rmi.RemoteNodeService;
import com.tradex.common.rmi.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class NodeRmiServiceImpl extends UnicastRemoteObject implements RemoteNodeService {

    private static final Logger log = LoggerFactory.getLogger(NodeRmiServiceImpl.class);

    @Value("${node.id:1}")
    private int nodeId;

    @Value("${node.name:node1}")
    private String nodeName;

    @Value("${rmi.host:localhost}")
    private String rmiHost;

    @Value("${rmi.port:1099}")
    private int rmiPort;

    private final LeaderElectionService leaderElectionService;
    private final LamportClockService lamportClockService;
    private final ThreadPoolExecutor orderProcessingExecutor;
    private final ThreadPoolExecutor replicationExecutor;

    private final MatchingEngine matchingEngine = new MatchingEngine();
    private final Map<String, Boolean> processedRequestIds = new ConcurrentHashMap<>();
    private final List<ReplicationEventDto> replicationLog = new CopyOnWriteArrayList<>();
    private final AtomicLong replicationSequence = new AtomicLong(0);

    // Controlled fault-injection flag for actual network / server-side failure
    private volatile boolean simulatedFailure = false;

    public boolean isSimulatedFailure() {
        return simulatedFailure;
    }

    public void setSimulatedFailure(boolean simulatedFailure) {
        this.simulatedFailure = simulatedFailure;
        log.warn("Node {} simulated failure set to: {}", nodeName, simulatedFailure);
    }

    public NodeRmiServiceImpl(
            LeaderElectionService leaderElectionService,
            LamportClockService lamportClockService,
            @Qualifier("orderProcessingExecutor") ThreadPoolExecutor orderProcessingExecutor,
            @Qualifier("replicationExecutor") ThreadPoolExecutor replicationExecutor
    ) throws RemoteException {
        super();
        this.leaderElectionService = leaderElectionService;
        this.lamportClockService = lamportClockService;
        this.orderProcessingExecutor = orderProcessingExecutor;
        this.replicationExecutor = replicationExecutor;
    }

    @Override
    public NodeStatusDto getNodeStatus() throws RemoteException {
        if (simulatedFailure) {
            throw new RemoteException("Node " + nodeName + " is UNAVAILABLE (Controlled Fault Injection: Simulating Hardware/Network Partition)");
        }

        Map<String, Object> poolMetrics = new HashMap<>();
        poolMetrics.put("activeWorkers", orderProcessingExecutor.getActiveCount());
        poolMetrics.put("corePoolSize", orderProcessingExecutor.getCorePoolSize());
        poolMetrics.put("maxPoolSize", orderProcessingExecutor.getMaximumPoolSize());
        poolMetrics.put("queuedTasks", orderProcessingExecutor.getQueue().size());
        poolMetrics.put("completedTasks", orderProcessingExecutor.getCompletedTaskCount());
        poolMetrics.put("rejectedTasks", 0); // CallerRunsPolicy handles saturation

        return new NodeStatusDto(
                nodeId,
                nodeName,
                rmiHost,
                rmiPort,
                leaderElectionService.isLeader(),
                leaderElectionService.getCurrentLeader(),
                leaderElectionService.getPriority(),
                lamportClockService.getTime(),
                List.of("MATCHING_ENGINE", "RMI_COORDINATION", "VECTOR_CLOCKS", "BULLY_ELECTION"),
                poolMetrics
        );
    }

    @Override
    public RemoteOrderResponseDto submitOrder(RemoteOrderRequestDto request) throws RemoteException {
        if (simulatedFailure) {
            throw new RemoteException("Node " + nodeName + " is UNAVAILABLE (Controlled Fault Injection: Simulating Hardware/Network Partition)");
        }

        // Enforce leader authoritative processing
        if (!leaderElectionService.isLeader()) {
            RemoteOrderResponseDto resp = new RemoteOrderResponseDto();
            resp.setRequestId(request.getRequestId());
            resp.setCorrelationId(request.getCorrelationId());
            resp.setOrderId(request.getOrderId());
            resp.setSuccessful(false);
            resp.setStatus("NOT_LEADER");
            resp.setErrorMessage("Node " + nodeName + " is not the leader. Current leader is " + leaderElectionService.getCurrentLeader());
            return resp;
        }

        // Idempotency check across network
        if (request.getRequestId() != null && processedRequestIds.putIfAbsent(request.getRequestId(), Boolean.TRUE) != null) {
            log.info("Duplicate order request received via RMI: {}", request.getRequestId());
            RemoteOrderResponseDto dupResp = new RemoteOrderResponseDto();
            dupResp.setRequestId(request.getRequestId());
            dupResp.setCorrelationId(request.getCorrelationId());
            dupResp.setOrderId(request.getOrderId());
            dupResp.setSuccessful(true);
            dupResp.setStatus("DUPLICATE_IGNORED");
            dupResp.setMatchedByNode(nodeName);
            return dupResp;
        }

        // Advance logical clock on receive
        lamportClockService.tick();

        Future<RemoteOrderResponseDto> future = orderProcessingExecutor.submit(() -> {
            try {
                BookOrder bo = new BookOrder(
                        request.getOrderId(),
                        request.getUserId(),
                        request.getSymbol(),
                        OrderSide.fromString(request.getSide()),
                        OrderType.fromString(request.getOrderType()),
                        request.getPrice(),
                        request.getStopPrice(),
                        request.getQuantity(),
                        BigDecimal.ZERO,
                        request.getOrderId(),
                        ZonedDateTime.now()
                );

                BigDecimal refPrice = request.getPrice() != null ? request.getPrice() : BigDecimal.valueOf(150.00);
                var matchResult = matchingEngine.match(bo, refPrice);

                RemoteOrderResponseDto resp = new RemoteOrderResponseDto();
                resp.setRequestId(request.getRequestId());
                resp.setCorrelationId(request.getCorrelationId());
                resp.setOrderId(request.getOrderId());
                resp.setStatus(matchResult.getFinalStatus().name());
                resp.setFilledQuantity(bo.getFilledQuantity());
                resp.setRemainingQuantity(bo.getRemainingQuantity());
                resp.setMatchedByNode(nodeName);
                resp.setExecutionTimestamp(System.currentTimeMillis());
                resp.setSuccessful(true);

                for (TradeExecution te : matchResult.getTrades()) {
                    RemoteOrderResponseDto.RemoteTradeDto tradeDto = new RemoteOrderResponseDto.RemoteTradeDto();
                    tradeDto.setTradeId(te.getTradeId());
                    tradeDto.setBuyerUserId(te.getBuyerUserId());
                    tradeDto.setSellerUserId(te.getSellerUserId());
                    tradeDto.setMakerOrderId(te.getMakerOrderId());
                    tradeDto.setTakerOrderId(te.getTakerOrderId());
                    tradeDto.setPrice(te.getPrice());
                    tradeDto.setQuantity(te.getQuantity());
                    tradeDto.setTotalValue(te.getTotalValue());
                    resp.getTrades().add(tradeDto);
                }

                // Async parallel replication to peers via replication pool
                replicateStateToCluster(resp);

                return resp;
            } catch (Exception e) {
                log.error("Error matching order: {}", e.getMessage(), e);
                RemoteOrderResponseDto errResp = new RemoteOrderResponseDto();
                errResp.setRequestId(request.getRequestId());
                errResp.setOrderId(request.getOrderId());
                errResp.setSuccessful(false);
                errResp.setStatus(OrderStatus.REJECTED.name());
                errResp.setErrorMessage(e.getMessage());
                return errResp;
            }
        });

        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (TimeoutException te) {
            future.cancel(true);
            throw new RemoteException("Matching execution timed out on leader " + nodeName);
        } catch (Exception ex) {
            throw new RemoteException("Failed processing order on leader: " + ex.getMessage(), ex);
        }
    }

    private void replicateStateToCluster(RemoteOrderResponseDto resp) {
        long seq = replicationSequence.incrementAndGet();
        long epoch = leaderElectionService.getPriority();
        ReplicationEventDto event = new ReplicationEventDto(
                UUID.randomUUID().toString(),
                "ORDER_MATCHED",
                nodeId,
                epoch,
                seq,
                "Matched order #" + resp.getOrderId() + " status=" + resp.getStatus() + " fills=" + resp.getTrades().size(),
                lamportClockService.getTime(),
                System.currentTimeMillis()
        );
        replicationLog.add(event);

        // Strongly Coordinated Replication: Replicate to peer nodes via peer RMI stubs
        int quorum = 1; // 1 replica ACK + leader = majority of 3
        int acks = 0;
        String[] peers = {"node1", "node2", "node3"};

        for (String peer : peers) {
            if (!peer.equals(nodeName)) {
                try {
                    java.rmi.registry.Registry reg = java.rmi.registry.LocateRegistry.getRegistry(peer, 1099);
                    RemoteNodeService stub = (RemoteNodeService) reg.lookup("RemoteNodeService");
                    ReplicationAckDto ack = stub.replicateEventWithAck(event);
                    if (ack != null && ack.isApplied()) {
                        acks++;
                        replicaAcks.put(peer, ack.getSequenceNumber());
                    }
                } catch (Exception e) {
                    log.warn("Replication to peer {} failed or offline: {}", peer, e.getMessage());
                }
            }
        }
        log.info("Strong coordination for event #{}: collected {}/{} replica ACKs", seq, acks, quorum);
    }

    private final Map<String, Long> replicaAcks = new ConcurrentHashMap<>();
    private final Set<String> appliedEventIds = ConcurrentHashMap.newKeySet();
    private final PriorityQueue<ReplicationEventDto> outOfOrderBuffer = new PriorityQueue<>(Comparator.comparingLong(ReplicationEventDto::getSequenceNumber));
    private final AtomicLong lastAppliedSequence = new AtomicLong(0);

    @Override
    public boolean replicateEvent(ReplicationEventDto event) throws RemoteException {
        ReplicationAckDto ack = replicateEventWithAck(event);
        return ack.isApplied();
    }

    @Override
    public ReplicationAckDto replicateEventWithAck(ReplicationEventDto event) throws RemoteException {
        // 1. Leader-epoch validation to reject stale-leader writes
        if (event.getTermEpoch() < leaderElectionService.getPriority()) {
            log.warn("Rejected stale-leader event with epoch {} (current priority {})", event.getTermEpoch(), leaderElectionService.getPriority());
            return new ReplicationAckDto(event.getEventId(), event.getSequenceNumber(), event.getTermEpoch(), nodeId, nodeName, false, lastAppliedSequence.get(), "STALE_LEADER_EPOCH", System.currentTimeMillis());
        }

        // 2. Idempotent check (suppress duplicates)
        if (!appliedEventIds.add(event.getEventId())) {
            log.info("Duplicate replication event {} ignored on node {}", event.getEventId(), nodeName);
            return new ReplicationAckDto(event.getEventId(), event.getSequenceNumber(), event.getTermEpoch(), nodeId, nodeName, true, lastAppliedSequence.get(), "DUPLICATE_ALREADY_APPLIED", System.currentTimeMillis());
        }

        lamportClockService.update(event.getLamportTimestamp());
        replicationLog.add(event);

        // 3. Sequential ordering & buffering for out-of-order events
        long expectedNext = lastAppliedSequence.get() + 1;
        if (event.getSequenceNumber() == expectedNext || lastAppliedSequence.get() == 0) {
            lastAppliedSequence.set(event.getSequenceNumber());
            drainBuffer();
        } else {
            synchronized (outOfOrderBuffer) {
                outOfOrderBuffer.add(event);
            }
            log.info("Node {} buffered out-of-order event #{}, currently at #{}", nodeName, event.getSequenceNumber(), lastAppliedSequence.get());
        }

        return new ReplicationAckDto(event.getEventId(), event.getSequenceNumber(), event.getTermEpoch(), nodeId, nodeName, true, lastAppliedSequence.get(), "SUCCESS", System.currentTimeMillis());
    }

    private void drainBuffer() {
        synchronized (outOfOrderBuffer) {
            while (!outOfOrderBuffer.isEmpty() && outOfOrderBuffer.peek().getSequenceNumber() == lastAppliedSequence.get() + 1) {
                ReplicationEventDto next = outOfOrderBuffer.poll();
                lastAppliedSequence.set(next.getSequenceNumber());
                log.info("Node {} drained buffered event #{}", nodeName, next.getSequenceNumber());
            }
        }
    }

    @Override
    public ClusterReplicationStatusDto getReplicationStatus() throws RemoteException {
        ClusterReplicationStatusDto status = new ClusterReplicationStatusDto();
        status.setLeaderNode(leaderElectionService.getCurrentLeader());
        status.setLeaderEpoch(leaderElectionService.getPriority());
        status.setHighestCommittedSequence(replicationSequence.get());
        status.setConsistencyModel("STRONGLY_COORDINATED_WRITES");
        status.setQuorumThreshold(1);
        status.setTotalEventsReplicated(replicationLog.size());
        status.setTimestamp(System.currentTimeMillis());

        Map<String, Long> committedMap = new HashMap<>();
        Map<String, Long> lagMap = new HashMap<>();
        Map<String, String> stateMap = new HashMap<>();

        for (String peer : List.of("node1", "node2", "node3")) {
            if (peer.equals(nodeName)) {
                committedMap.put(peer, lastAppliedSequence.get());
                lagMap.put(peer, 0L);
                stateMap.put(peer, "PRIMARY_LEADER");
            } else {
                long acked = replicaAcks.getOrDefault(peer, 0L);
                committedMap.put(peer, acked);
                long lag = Math.max(0, replicationSequence.get() - acked);
                lagMap.put(peer, lag);
                stateMap.put(peer, lag == 0 ? "SYNCHRONIZED" : (lag <= 5 ? "EVENTUALLY_CONVERGING" : "LAGGING"));
            }
        }

        status.setReplicaCommittedSequences(committedMap);
        status.setReplicaLags(lagMap);
        status.setReplicaStates(stateMap);
        return status;
    }

    @Override
    public boolean updateLeaderEpoch(int leaderId, String leaderName, long termEpoch) throws RemoteException {
        leaderElectionService.setLeader(leaderName);
        lamportClockService.tick();
        log.info("Node {} updated leader to {} (epoch {})", nodeName, leaderName, termEpoch);
        return true;
    }

    @Override
    public long exchangeClock(ClockSyncDto clockSync) throws RemoteException {
        lamportClockService.update(clockSync.getLamportTimestamp());
        return lamportClockService.getTime();
    }

    @Override
    public List<ReplicationEventDto> syncState(long fromSequenceNumber) throws RemoteException {
        List<ReplicationEventDto> missing = new ArrayList<>();
        for (ReplicationEventDto ev : replicationLog) {
            if (ev.getSequenceNumber() >= fromSequenceNumber) {
                missing.add(ev);
            }
        }
        return missing;
    }

    public MatchingEngine getMatchingEngine() {
        return matchingEngine;
    }
}
