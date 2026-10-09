import React, { useState, useEffect } from 'react';
import api from '../api/axios';
import { 
  Activity, 
  AlertTriangle, 
  CheckCircle, 
  Clock, 
  Play, 
  RotateCcw, 
  Server, 
  ShieldAlert, 
  Wifi, 
  WifiOff, 
  Zap,
  RefreshCw,
  Flame
} from 'lucide-react';

interface HeartbeatStatus {
  nodeName: string;
  reachable: boolean;
  lastHeartbeatTimeMs: number;
  missedHeartbeatCount: number;
  healthStatus: 'HEALTHY' | 'SUSPECTED' | 'CONFIRMED_UNAVAILABLE';
  latencyMs: number;
}

interface FailoverReport {
  activeLeader: string;
  leaderEpoch: number;
  tradingState: 'AVAILABLE' | 'DEGRADED' | 'SUSPENDED';
  failoverStatus: 'STABLE' | 'ELECTION_IN_PROGRESS' | 'RECOVERING_STATE';
  nodeHeartbeats: Record<string, HeartbeatStatus>;
  lastFailoverTimestamp: number;
  lastFailoverReason: string;
  timestamp: number;
}

const FailoverRecovery: React.FC = () => {
  const [report, setReport] = useState<FailoverReport | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [feedbackMessage, setFeedbackMessage] = useState<{ type: 'success' | 'error' | 'info'; text: string } | null>(null);

  const fetchStatus = async () => {
    try {
      const res = await api.get('/cluster/failover');
      setReport(res.data);
    } catch (err: any) {
      console.error('Failed fetching failover report:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchStatus();
    const interval = setInterval(fetchStatus, 2000);
    return () => clearInterval(interval);
  }, []);

  const handleSimulateCrash = async (nodeId: string) => {
    setActionLoading(`crash-${nodeId}`);
    setFeedbackMessage(null);
    try {
      const res = await api.post(`/cluster/fault/crash/${nodeId}`);
      setFeedbackMessage({
        type: 'info',
        text: `Controlled fault injected: ${nodeId} simulated hardware/network crash. Failure detector engaged!`
      });
      fetchStatus();
    } catch (err: any) {
      setFeedbackMessage({
        type: 'error',
        text: `Fault injection failed on ${nodeId}: ${err.response?.data?.message || err.message}`
      });
    } finally {
      setActionLoading(null);
    }
  };

  const handleSimulateRecovery = async (nodeId: string) => {
    setActionLoading(`recover-${nodeId}`);
    setFeedbackMessage(null);
    try {
      const res = await api.post(`/cluster/fault/recover/${nodeId}`);
      setFeedbackMessage({
        type: 'success',
        text: `Recovery signaled for ${nodeId}. Node reconnected to cluster and syncing replica state!`
      });
      fetchStatus();
    } catch (err: any) {
      setFeedbackMessage({
        type: 'error',
        text: `Recovery failed on ${nodeId}: ${err.response?.data?.message || err.message}`
      });
    } finally {
      setActionLoading(null);
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'HEALTHY':
        return <span className="status-badge" style={{ background: '#059669', color: '#fff', padding: '4px 10px', borderRadius: '12px', fontSize: '12px', fontWeight: 600 }}>HEALTHY</span>;
      case 'SUSPECTED':
        return <span className="status-badge" style={{ background: '#d97706', color: '#fff', padding: '4px 10px', borderRadius: '12px', fontSize: '12px', fontWeight: 600 }}>SUSPECTED (MISSED)</span>;
      case 'CONFIRMED_UNAVAILABLE':
        return <span className="status-badge" style={{ background: '#dc2626', color: '#fff', padding: '4px 10px', borderRadius: '12px', fontSize: '12px', fontWeight: 600 }}>CONFIRMED UNAVAILABLE</span>;
      default:
        return <span className="status-badge">{status}</span>;
    }
  };

  const getTradingStateBadge = (state: string) => {
    switch (state) {
      case 'AVAILABLE':
        return <span style={{ background: '#10b981', color: '#fff', padding: '6px 14px', borderRadius: '16px', fontWeight: 700, fontSize: '13px', display: 'inline-flex', alignItems: 'center', gap: '6px' }}><CheckCircle size={16} /> AVAILABLE</span>;
      case 'DEGRADED':
        return <span style={{ background: '#f59e0b', color: '#fff', padding: '6px 14px', borderRadius: '16px', fontWeight: 700, fontSize: '13px', display: 'inline-flex', alignItems: 'center', gap: '6px' }}><AlertTriangle size={16} /> DEGRADED (PARTIAL QUORUM)</span>;
      case 'SUSPENDED':
        return <span style={{ background: '#ef4444', color: '#fff', padding: '6px 14px', borderRadius: '16px', fontWeight: 700, fontSize: '13px', display: 'inline-flex', alignItems: 'center', gap: '6px' }}><ShieldAlert size={16} /> SUSPENDED (SAFETY LOCK)</span>;
      default:
        return <span>{state}</span>;
    }
  };

  return (
    <div className="failover-dashboard" style={{ padding: '24px', color: '#f8fafc' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '24px' }}>
        <div>
          <h1 style={{ fontSize: '28px', fontWeight: 800, margin: 0, display: 'flex', alignItems: 'center', gap: '10px' }}>
            <Activity className="text-primary" size={32} color="#3b82f6" />
            Fault Detection, Failover & Recovery
          </h1>
          <p style={{ color: '#94a3b8', margin: '6px 0 0 0', fontSize: '14px' }}>
            Phase 10: Server-side heartbeat monitoring, Bully automatic election, split-brain epoch fencing, and controlled fault injection.
          </p>
        </div>
        <button 
          onClick={fetchStatus}
          style={{ 
            background: '#1e293b', 
            border: '1px solid #334155', 
            color: '#f8fafc', 
            padding: '8px 16px', 
            borderRadius: '8px', 
            display: 'flex', 
            alignItems: 'center', 
            gap: '8px',
            cursor: 'pointer',
            fontSize: '13px'
          }}
        >
          <RefreshCw size={14} /> Refresh Cluster
        </button>
      </div>

      {feedbackMessage && (
        <div style={{
          padding: '12px 18px',
          borderRadius: '8px',
          marginBottom: '20px',
          fontSize: '14px',
          fontWeight: 500,
          background: feedbackMessage.type === 'success' ? '#064e3b' : feedbackMessage.type === 'error' ? '#7f1d1d' : '#1e3a8a',
          color: '#ffffff',
          border: `1px solid ${feedbackMessage.type === 'success' ? '#059669' : feedbackMessage.type === 'error' ? '#b91c1c' : '#2563eb'}`
        }}>
          {feedbackMessage.text}
        </div>
      )}

      {/* Cluster Overview Ribbon */}
      <div style={{ 
        display: 'grid', 
        gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', 
        gap: '16px', 
        marginBottom: '24px' 
      }}>
        <div style={{ background: '#1e293b', padding: '18px', borderRadius: '12px', border: '1px solid #334155' }}>
          <div style={{ fontSize: '12px', color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Authoritative Leader</div>
          <div style={{ fontSize: '24px', fontWeight: 800, color: '#38bdf8', marginTop: '4px' }}>
            {report?.activeLeader || 'node3'}
          </div>
          <div style={{ fontSize: '12px', color: '#64748b', marginTop: '4px' }}>
            Active Epoch: <strong style={{ color: '#cbd5e1' }}>#{report?.leaderEpoch || 3}</strong>
          </div>
        </div>

        <div style={{ background: '#1e293b', padding: '18px', borderRadius: '12px', border: '1px solid #334155' }}>
          <div style={{ fontSize: '12px', color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Trading System Availability</div>
          <div style={{ marginTop: '8px' }}>
            {report ? getTradingStateBadge(report.tradingState) : <span>Loading...</span>}
          </div>
          <div style={{ fontSize: '11px', color: '#64748b', marginTop: '6px' }}>
            Authoritative order intake status
          </div>
        </div>

        <div style={{ background: '#1e293b', padding: '18px', borderRadius: '12px', border: '1px solid #334155' }}>
          <div style={{ fontSize: '12px', color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Failover Coordinator Status</div>
          <div style={{ fontSize: '18px', fontWeight: 700, color: report?.failoverStatus === 'STABLE' ? '#34d399' : '#fbbf24', marginTop: '6px' }}>
            {report?.failoverStatus || 'STABLE'}
          </div>
          <div style={{ fontSize: '11px', color: '#64748b', marginTop: '6px' }}>
            Last Reason: {report?.lastFailoverReason || 'Bootstrap'}
          </div>
        </div>

        <div style={{ background: '#1e293b', padding: '18px', borderRadius: '12px', border: '1px solid #334155' }}>
          <div style={{ fontSize: '12px', color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Heartbeat Interval / Timeout</div>
          <div style={{ fontSize: '20px', fontWeight: 700, color: '#a78bfa', marginTop: '4px' }}>
            2500ms / 3 fails
          </div>
          <div style={{ fontSize: '11px', color: '#64748b', marginTop: '6px' }}>
            Threshold for confirmed unavailability
          </div>
        </div>
      </div>

      {/* Nodes Heartbeat Matrix & Fault Injection Console */}
      <div style={{ background: '#1e293b', borderRadius: '12px', border: '1px solid #334155', padding: '20px', marginBottom: '24px' }}>
        <h2 style={{ fontSize: '18px', fontWeight: 700, margin: '0 0 16px 0', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Server size={20} color="#60a5fa" /> Node Heartbeat Matrix & Controlled Fault Injection
        </h2>
        <p style={{ color: '#94a3b8', fontSize: '13px', margin: '-8px 0 16px 0' }}>
          Crash simulation initiates authentic network failure inside node stubs, causing heartbeat misses, automatic election, and state recovery.
        </p>

        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '14px' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid #334155', color: '#94a3b8' }}>
                <th style={{ padding: '12px 16px' }}>NODE</th>
                <th style={{ padding: '12px 16px' }}>ROLE</th>
                <th style={{ padding: '12px 16px' }}>REACHABILITY</th>
                <th style={{ padding: '12px 16px' }}>HEALTH STATUS</th>
                <th style={{ padding: '12px 16px' }}>MISSED BEATS</th>
                <th style={{ padding: '12px 16px' }}>LATENCY</th>
                <th style={{ padding: '12px 16px', textAlign: 'right' }}>CONTROLLED FAULT ACTION</th>
              </tr>
            </thead>
            <tbody>
              {['node1', 'node2', 'node3'].map((nodeId) => {
                const hb = report?.nodeHeartbeats?.[nodeId];
                const isLeader = report?.activeLeader === nodeId;
                const isReachable = hb?.reachable ?? true;
                const status = hb?.healthStatus ?? 'HEALTHY';
                const missed = hb?.missedHeartbeatCount ?? 0;
                const latency = hb?.latencyMs ?? 15;

                return (
                  <tr key={nodeId} style={{ borderBottom: '1px solid #1e293b' }}>
                    <td style={{ padding: '14px 16px', fontWeight: 700 }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <div style={{ 
                          width: '10px', 
                          height: '10px', 
                          borderRadius: '50%', 
                          background: isReachable ? '#10b981' : '#ef4444' 
                        }} />
                        {nodeId.toUpperCase()}
                      </div>
                    </td>
                    <td style={{ padding: '14px 16px' }}>
                      {isLeader ? (
                        <span style={{ background: '#3b82f6', color: '#fff', padding: '3px 8px', borderRadius: '6px', fontSize: '11px', fontWeight: 700 }}>
                          AUTHORITATIVE LEADER
                        </span>
                      ) : (
                        <span style={{ background: '#334155', color: '#94a3b8', padding: '3px 8px', borderRadius: '6px', fontSize: '11px' }}>
                          BACKUP REPLICA
                        </span>
                      )}
                    </td>
                    <td style={{ padding: '14px 16px' }}>
                      {isReachable ? (
                        <span style={{ color: '#10b981', display: 'flex', alignItems: 'center', gap: '4px' }}>
                          <Wifi size={14} /> ONLINE
                        </span>
                      ) : (
                        <span style={{ color: '#ef4444', display: 'flex', alignItems: 'center', gap: '4px' }}>
                          <WifiOff size={14} /> DISCONNECTED
                        </span>
                      )}
                    </td>
                    <td style={{ padding: '14px 16px' }}>
                      {getStatusBadge(status)}
                    </td>
                    <td style={{ padding: '14px 16px', color: missed > 0 ? '#ef4444' : '#94a3b8' }}>
                      {missed} / 3
                    </td>
                    <td style={{ padding: '14px 16px', color: '#cbd5e1' }}>
                      {latency >= 0 ? `${latency} ms` : 'N/A'}
                    </td>
                    <td style={{ padding: '14px 16px', textAlign: 'right' }}>
                      <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
                        {isReachable ? (
                          <button
                            onClick={() => handleSimulateCrash(nodeId)}
                            disabled={actionLoading !== null}
                            style={{
                              background: '#ef4444',
                              color: '#fff',
                              border: 'none',
                              padding: '6px 12px',
                              borderRadius: '6px',
                              fontSize: '12px',
                              fontWeight: 600,
                              cursor: 'pointer',
                              display: 'flex',
                              alignItems: 'center',
                              gap: '4px'
                            }}
                          >
                            <Flame size={12} /> Crash Node
                          </button>
                        ) : (
                          <button
                            onClick={() => handleSimulateRecovery(nodeId)}
                            disabled={actionLoading !== null}
                            style={{
                              background: '#10b981',
                              color: '#fff',
                              border: 'none',
                              padding: '6px 12px',
                              borderRadius: '6px',
                              fontSize: '12px',
                              fontWeight: 600,
                              cursor: 'pointer',
                              display: 'flex',
                              alignItems: 'center',
                              gap: '4px'
                            }}
                          >
                            <RotateCcw size={12} /> Recover Node
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {/* Failover Protocol & Safeguards Architecture */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '20px' }}>
        <div style={{ background: '#1e293b', padding: '20px', borderRadius: '12px', border: '1px solid #334155' }}>
          <h3 style={{ fontSize: '16px', fontWeight: 700, margin: '0 0 10px 0', color: '#38bdf8' }}>
            Split-Brain & Fencing Mechanism
          </h3>
          <p style={{ fontSize: '13px', color: '#94a3b8', lineHeight: 1.6, margin: 0 }}>
            Every cluster election increments the authoritative <strong>Leader Epoch</strong>. Stale leaders that recover from network partitions or pauses have their writes rejected with <code>STALE_LEADER_EPOCH</code>. Gateway checks node epoch before routing authoritative matching operations.
          </p>
        </div>

        <div style={{ background: '#1e293b', padding: '20px', borderRadius: '12px', border: '1px solid #334155' }}>
          <h3 style={{ fontSize: '16px', fontWeight: 700, margin: '0 0 10px 0', color: '#34d399' }}>
            Trading Safety & Write Suspension
          </h3>
          <p style={{ fontSize: '13px', color: '#94a3b8', lineHeight: 1.6, margin: 0 }}>
            If active leadership cannot be confirmed, or fewer than quorum nodes are reachable, TradeX automatically shifts trading state to <strong>SUSPENDED</strong>. New orders are temporarily held or rejected rather than allowing duplicate executions or inconsistent wallet balances.
          </p>
        </div>

        <div style={{ background: '#1e293b', padding: '20px', borderRadius: '12px', border: '1px solid #334155' }}>
          <h3 style={{ fontSize: '16px', fontWeight: 700, margin: '0 0 10px 0', color: '#fbbf24' }}>
            State Recovery & Durability
          </h3>
          <p style={{ fontSize: '13px', color: '#94a3b8', lineHeight: 1.6, margin: 0 }}>
            Upon new leader confirmation, the node rebuilds its in-memory order books and sequence log directly from durable PostgreSQL records and available replica catch-up logs before resuming authoritative order acceptance.
          </p>
        </div>
      </div>
    </div>
  );
};

export default FailoverRecovery;
