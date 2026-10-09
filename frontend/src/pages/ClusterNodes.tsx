import React, { useState, useEffect } from 'react';
import api from '../api/axios';
import { 
  Server, 
  Cpu, 
  Activity, 
  ArrowRight, 
  ShieldCheck, 
  Clock, 
  RefreshCw, 
  CheckCircle2, 
  AlertTriangle,
  Layers,
  Zap,
  Radio
} from 'lucide-react';

interface WorkerPoolMetrics {
  activeWorkers?: number;
  corePoolSize?: number;
  maxPoolSize?: number;
  queuedTasks?: number;
  completedTasks?: number;
  rejectedTasks?: number;
}

interface NodeStatus {
  nodeId: number;
  nodeName: string;
  host: string;
  rmiPort: number;
  leader: boolean;
  currentLeader: string;
  termEpoch: number;
  lamportTimestamp: number;
  capabilities: string[];
  workerPoolMetrics?: WorkerPoolMetrics;
}

interface RpcTelemetry {
  requestId: string;
  correlationId?: string;
  sourceNode: string;
  destinationNode: string;
  communicationMethod: string;
  orderId?: number;
  symbol?: string;
  quantity?: number;
  latencyMs?: number;
  outcome: string;
  matchedByNode?: string;
  status?: string;
  errorMessage?: string;
  timestamp: string;
}

export const ClusterNodes: React.FC = () => {
  const [nodes, setNodes] = useState<NodeStatus[]>([]);
  const [telemetry, setTelemetry] = useState<RpcTelemetry[]>([]);
  const [leaderName, setLeaderName] = useState<string>('node3');
  const [loading, setLoading] = useState<boolean>(true);

  const fetchClusterData = async () => {
    try {
      setLoading(true);
      const [nodesRes, teleRes, leaderRes] = await Promise.all([
        api.get('/cluster/nodes'),
        api.get('/cluster/telemetry'),
        api.get('/cluster/leader')
      ]);
      setNodes(nodesRes.data || []);
      setTelemetry(teleRes.data || []);
      if (leaderRes.data?.leader) setLeaderName(leaderRes.data.leader);
    } catch (err) {
      console.error('Failed to load cluster telemetry:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchClusterData();
    const timer = setInterval(fetchClusterData, 4000);
    return () => clearInterval(timer);
  }, []);

  return (
    <div className="cluster-nodes-container">
      <div className="page-header">
        <div>
          <h1>Distributed Nodes & Java RMI Telemetry</h1>
          <p className="page-subtitle">
            Real-time inter-node communication, remote method invocation trace, and thread pool worker metrics.
          </p>
        </div>
        <div className="header-actions">
          <span className="badge-pill badge-primary">
            <Radio size={14} className="animate-pulse mr-1" />
            Authoritative Leader: <strong className="ml-1">{leaderName}</strong>
          </span>
          <button className="btn-secondary" onClick={fetchClusterData}>
            <RefreshCw size={15} className={loading ? 'spinning' : ''} /> Refresh
          </button>
        </div>
      </div>

      {/* Cluster Node Cards */}
      <div className="nodes-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '20px', marginBottom: '24px' }}>
        {nodes.map((node) => {
          const isLeader = node.nodeName === leaderName || node.leader;
          const isOnline = !node.capabilities?.includes('OFFLINE');
          const pool = node.workerPoolMetrics || {};

          return (
            <div key={node.nodeName} className={`card node-card ${isLeader ? 'border-primary' : ''}`} style={{ borderTop: isLeader ? '4px solid #6366f1' : '4px solid #475569' }}>
              <div className="card-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <Server size={20} className={isLeader ? 'text-primary' : 'text-secondary'} />
                  <h3 style={{ margin: 0 }}>{node.nodeName.toUpperCase()}</h3>
                </div>
                <div style={{ display: 'flex', gap: '6px' }}>
                  {isLeader ? (
                    <span className="status-tag status-success" style={{ background: '#4338ca', color: '#e0e7ff' }}>
                      <ShieldCheck size={12} className="inline mr-1" /> LEADER
                    </span>
                  ) : (
                    <span className="status-tag status-secondary">FOLLOWER</span>
                  )}
                  <span className={`status-tag ${isOnline ? 'status-open' : 'status-danger'}`}>
                    {isOnline ? 'ONLINE' : 'OFFLINE'}
                  </span>
                </div>
              </div>

              <div className="node-details" style={{ fontSize: '13px', margin: '14px 0', display: 'flex', flexDirection: 'column', gap: '6px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span className="text-muted">Host & RMI Port:</span>
                  <span className="font-mono">{node.host}:{node.rmiPort || 1099}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span className="text-muted">Priority / Term:</span>
                  <span>Priority {node.termEpoch || node.nodeId}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span className="text-muted">Lamport Clock:</span>
                  <span className="font-mono text-warning">T = {node.lamportTimestamp || 0}</span>
                </div>
              </div>

              {/* Bounded Worker Pool Telemetry */}
              <div className="worker-pool-section" style={{ background: '#0f172a', borderRadius: '8px', padding: '12px', marginTop: '10px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '8px', color: '#94a3b8', fontSize: '12px', fontWeight: 'bold' }}>
                  <Cpu size={14} /> ORDER WORKER POOL
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px', fontSize: '12px' }}>
                  <div>
                    <span className="text-muted">Active Workers:</span>{' '}
                    <strong className="text-info">{pool.activeWorkers ?? 0} / {pool.maxPoolSize ?? 16}</strong>
                  </div>
                  <div>
                    <span className="text-muted">Queue Depth:</span>{' '}
                    <strong className="text-warning">{pool.queuedTasks ?? 0} / 500</strong>
                  </div>
                  <div>
                    <span className="text-muted">Tasks Completed:</span>{' '}
                    <strong>{pool.completedTasks ?? 0}</strong>
                  </div>
                  <div>
                    <span className="text-muted">Tasks Rejected:</span>{' '}
                    <strong className="text-success">{pool.rejectedTasks ?? 0}</strong>
                  </div>
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {/* RMI Invocation Real-Time Telemetry Table */}
      <div className="card table-card">
        <div className="card-header">
          <h3>
            <Zap size={18} className="inline mr-2 text-warning" />
            Inter-Node Remote Call Activity (Real Java RMI Boundaries)
          </h3>
          <span className="text-xs text-muted">Real-time traces showing cross-server RPC executions</span>
        </div>
        <div className="table-responsive">
          <table className="tradex-table">
            <thead>
              <tr>
                <th>Request ID</th>
                <th>Source</th>
                <th></th>
                <th>Destination</th>
                <th>Method</th>
                <th>Order Target</th>
                <th>Latency</th>
                <th>Outcome</th>
                <th>Timestamp</th>
              </tr>
            </thead>
            <tbody>
              {telemetry.length === 0 ? (
                <tr>
                  <td colSpan={9} className="text-center py-4">
                    No remote method calls recorded yet. Place an order to observe inter-node RMI routing!
                  </td>
                </tr>
              ) : (
                telemetry.map((t, idx) => (
                  <tr key={t.requestId || idx}>
                    <td className="font-mono text-xs text-muted">
                      {t.requestId ? t.requestId.substring(0, 13) + '...' : '-'}
                    </td>
                    <td>
                      <span className="badge-pill badge-secondary">{t.sourceNode}</span>
                    </td>
                    <td className="text-center text-muted">
                      <ArrowRight size={14} className="inline" />
                    </td>
                    <td>
                      <span className="badge-pill badge-primary font-bold">{t.destinationNode}</span>
                    </td>
                    <td className="text-xs font-mono">{t.communicationMethod}</td>
                    <td className="text-xs">
                      {t.orderId ? (
                        <span>
                          Order #{t.orderId} ({t.quantity} {t.symbol})
                        </span>
                      ) : (
                        '-'
                      )}
                    </td>
                    <td className="font-mono text-xs">
                      <span className={t.latencyMs && t.latencyMs > 100 ? 'text-warning' : 'text-success'}>
                        {t.latencyMs !== undefined ? `${t.latencyMs} ms` : '-'}
                      </span>
                    </td>
                    <td>
                      <span className={`status-tag status-${t.outcome.toLowerCase()}`}>
                        {t.outcome}
                      </span>
                    </td>
                    <td className="text-xs text-muted">
                      {new Date(t.timestamp).toLocaleTimeString()}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

export default ClusterNodes;
