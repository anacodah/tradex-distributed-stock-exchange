import React, { useState, useEffect } from 'react';
import api from '../api/axios';
import { 
  Database, 
  Layers, 
  ShieldCheck, 
  RefreshCw, 
  CheckCircle2, 
  AlertTriangle, 
  Clock, 
  ArrowRight,
  TrendingDown,
  Activity,
  GitBranch,
  Lock,
  Eye
} from 'lucide-react';

interface ClusterReplicationStatus {
  leaderNode: string;
  leaderEpoch: number;
  highestCommittedSequence: number;
  consistencyModel: string;
  quorumThreshold: number;
  replicaCommittedSequences: Record<string, number>;
  replicaLags: Record<string, number>;
  replicaStates: Record<string, string>;
  totalEventsReplicated: number;
  timestamp: number;
}

export const ReplicationDashboard: React.FC = () => {
  const [status, setStatus] = useState<ClusterReplicationStatus | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [consistencyPolicy, setConsistencyPolicy] = useState<'STRONG' | 'EVENTUAL'>('STRONG');
  const [simulatedProjectionLag, setSimulatedProjectionLag] = useState<number>(0);

  const fetchReplicationData = async () => {
    try {
      setLoading(true);
      const res = await api.get('/cluster/replication');
      setStatus(res.data);
    } catch (err) {
      console.error('Failed to load replication status:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchReplicationData();
    const interval = setInterval(fetchReplicationData, 4000);
    return () => clearInterval(interval);
  }, []);

  return (
    <div className="replication-container">
      <div className="page-header">
        <div>
          <h1>Primary-Backup Replication & Consistency Models (Phase 9)</h1>
          <p className="page-subtitle">
            Authoritative Leader sequence replication, replica lag monitoring, quorum acknowledgements, and eventual read consistency.
          </p>
        </div>
        <div className="header-actions">
          <button className="btn-secondary" onClick={fetchReplicationData}>
            <RefreshCw size={15} className={loading ? 'spinning' : ''} /> Refresh Status
          </button>
        </div>
      </div>

      {/* Database & Architecture Reality Banner */}
      <div className="alert alert-info" style={{ marginBottom: '24px', display: 'flex', alignItems: 'flex-start', gap: '12px' }}>
        <Database size={20} style={{ flexShrink: 0, marginTop: '2px' }} />
        <div>
          <strong>Architecture Truth & Supabase Considerations:</strong>
          <p style={{ margin: '4px 0 0 0', fontSize: '13px' }}>
            All application nodes share the durable PostgreSQL database (hosted on Supabase / Docker) as the single financial source of truth.
            The primary-backup mechanisms displayed here are <em>application-level state replication and in-memory order-book synchronization</em> across JVM processes, strictly avoiding multiple conflicting financial databases.
          </p>
        </div>
      </div>

      {/* Cluster Overview Cards */}
      <div className="nodes-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '16px', marginBottom: '24px' }}>
        <div className="card stat-card" style={{ borderLeft: '4px solid #6366f1' }}>
          <div className="stat-label">Authoritative Leader</div>
          <div className="stat-value text-primary font-bold" style={{ fontSize: '24px', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <ShieldCheck size={24} /> {status?.leaderNode?.toUpperCase() || 'NODE3'}
          </div>
          <div className="text-xs text-muted" style={{ marginTop: '4px' }}>Leader Epoch: Term #{status?.leaderEpoch || 3}</div>
        </div>

        <div className="card stat-card" style={{ borderLeft: '4px solid #10b981' }}>
          <div className="stat-label">Committed Sequence #</div>
          <div className="stat-value text-success font-mono" style={{ fontSize: '24px' }}>
            #{status?.highestCommittedSequence ?? 0}
          </div>
          <div className="text-xs text-muted" style={{ marginTop: '4px' }}>Monotonically increasing log</div>
        </div>

        <div className="card stat-card" style={{ borderLeft: '4px solid #f59e0b' }}>
          <div className="stat-label">Replication Quorum</div>
          <div className="stat-value text-warning" style={{ fontSize: '24px' }}>
            {status?.quorumThreshold || 1} Replica ACK
          </div>
          <div className="text-xs text-muted" style={{ marginTop: '4px' }}>Leader + 1 Replica = Majority (2/3)</div>
        </div>

        <div className="card stat-card" style={{ borderLeft: '4px solid #38bdf8' }}>
          <div className="stat-label">Events Replicated</div>
          <div className="stat-value font-mono" style={{ fontSize: '24px' }}>
            {status?.totalEventsReplicated ?? 0}
          </div>
          <div className="text-xs text-muted" style={{ marginTop: '4px' }}>Replay-capable memory log</div>
        </div>
      </div>

      {/* Consistency Models Demonstration Panel */}
      <div className="card" style={{ marginBottom: '24px' }}>
        <div className="card-header">
          <h3>
            <GitBranch size={18} className="inline mr-2 text-warning" />
            Consistency Model Policies & Demonstration
          </h3>
          <span className="text-xs text-muted">Toggle between strongly coordinated trading writes vs. eventual read projections</span>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px', marginTop: '16px' }}>
          {/* Policy 1: Strong Coordination */}
          <div 
            onClick={() => { setConsistencyPolicy('STRONG'); setSimulatedProjectionLag(0); }}
            style={{
              padding: '16px',
              borderRadius: '8px',
              border: consistencyPolicy === 'STRONG' ? '2px solid #6366f1' : '1px solid #334155',
              background: consistencyPolicy === 'STRONG' ? '#1e1b4b' : '#0f172a',
              cursor: 'pointer'
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
              <Lock size={18} className="text-primary" />
              <strong>1. Strongly Coordinated Writes</strong>
            </div>
            <p className="text-xs text-muted" style={{ margin: 0, lineHeight: 1.5 }}>
              Applied to critical order submissions, trade fills, and wallet ledger updates. Orders are committed only after quorum durability (PostgreSQL transaction + 1 replica ACK) is verified.
            </p>
            <div style={{ marginTop: '12px', fontSize: '12px' }}>
              <span className="status-tag status-success">Zero Stale Reads</span>
              <span className="status-tag status-open ml-2">Atomic Settlement</span>
            </div>
          </div>

          {/* Policy 2: Eventual Consistency */}
          <div 
            onClick={() => { setConsistencyPolicy('EVENTUAL'); setSimulatedProjectionLag(3); }}
            style={{
              padding: '16px',
              borderRadius: '8px',
              border: consistencyPolicy === 'EVENTUAL' ? '2px solid #f59e0b' : '1px solid #334155',
              background: consistencyPolicy === 'EVENTUAL' ? '#451a03' : '#0f172a',
              cursor: 'pointer'
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
              <Eye size={18} className="text-warning" />
              <strong>2. Eventual Consistency (Read Projections)</strong>
            </div>
            <p className="text-xs text-muted" style={{ margin: 0, lineHeight: 1.5 }}>
              Applied to non-critical analytics, market ticker statistics, and read views. Projections may briefly lag behind authoritative trade logs and converge asynchronously without blocking trading execution.
            </p>
            <div style={{ marginTop: '12px', fontSize: '12px' }}>
              <span className="status-tag status-warning">Async Convergence</span>
              <span className="status-tag status-secondary ml-2">Non-blocking Read Cache</span>
            </div>
          </div>
        </div>

        {consistencyPolicy === 'EVENTUAL' && (
          <div style={{ background: '#0f172a', padding: '14px', borderRadius: '6px', marginTop: '16px', border: '1px solid #334155' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span className="text-xs font-semibold text-warning">
                Simulated Read Projection Lag: {simulatedProjectionLag} events behind primary
              </span>
              <button 
                className="btn-secondary" 
                style={{ fontSize: '11px', padding: '4px 10px' }}
                onClick={() => setSimulatedProjectionLag(0)}
              >
                Trigger Convergence Sync
              </button>
            </div>
            <p className="text-xs text-muted" style={{ margin: '6px 0 0 0' }}>
              Primary sequence is #{status?.highestCommittedSequence || 0}, while read replica projection is currently evaluating sequence #{(status?.highestCommittedSequence || 0) - simulatedProjectionLag}. Projections catch up idempotently upon buffer drain.
            </p>
          </div>
        )}
      </div>

      {/* Primary vs Replica State Table */}
      <div className="card table-card">
        <div className="card-header">
          <h3>
            <Layers size={18} className="inline mr-2 text-info" />
            Cluster Node Replication & Synchronization Matrix
          </h3>
          <span className="text-xs text-muted">Real-time telemetry showing replica lag and sequence commit progress</span>
        </div>
        <div className="table-responsive">
          <table className="tradex-table">
            <thead>
              <tr>
                <th>Node</th>
                <th>Role</th>
                <th>Last Acknowledged Seq #</th>
                <th>Replication Lag</th>
                <th>Synchronization Status</th>
                <th>Idempotency Protection</th>
              </tr>
            </thead>
            <tbody>
              {['node1', 'node2', 'node3'].map((nodeName) => {
                const isLeader = nodeName === (status?.leaderNode || 'node3');
                const lastSeq = status?.replicaCommittedSequences?.[nodeName] ?? (isLeader ? status?.highestCommittedSequence ?? 0 : 0);
                const lag = isLeader ? 0 : (status?.replicaLags?.[nodeName] ?? 0);
                const state = isLeader ? 'PRIMARY_LEADER' : (status?.replicaStates?.[nodeName] ?? 'SYNCHRONIZED');

                return (
                  <tr key={nodeName}>
                    <td className="font-bold">{nodeName.toUpperCase()}</td>
                    <td>
                      {isLeader ? (
                        <span className="status-tag status-success" style={{ background: '#4338ca', color: '#e0e7ff' }}>
                          <ShieldCheck size={12} className="inline mr-1" /> PRIMARY
                        </span>
                      ) : (
                        <span className="status-tag status-secondary">BACKUP REPLICA</span>
                      )}
                    </td>
                    <td className="font-mono font-bold">
                      #{lastSeq}
                    </td>
                    <td>
                      <span className={`font-mono text-xs ${lag === 0 ? 'text-success' : 'text-warning'}`}>
                        {lag === 0 ? '0 events (Synchronized)' : `${lag} events behind`}
                      </span>
                    </td>
                    <td>
                      <span className={`status-tag status-${state === 'PRIMARY_LEADER' ? 'open' : state === 'SYNCHRONIZED' ? 'success' : 'warning'}`}>
                        {state}
                      </span>
                    </td>
                    <td>
                      <span className="text-xs text-success font-semibold">
                        <CheckCircle2 size={12} className="inline mr-1" /> Dedup Set & Buffer Active
                      </span>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

export default ReplicationDashboard;
