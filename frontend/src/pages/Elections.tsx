import React, { useEffect, useState } from 'react';
import api from '../api/axios';
import { ShieldCheck, Zap, GitMerge } from 'lucide-react';

interface NodeInfo {
  nodeId: string;
  nodeName: string;
  status: string;
  priority: number;
  leader: string;
  isLeader: boolean;
}

interface ElectionResult {
  electionId: string;
  algorithm: 'BULLY' | 'RING';
  initiator: string;
  oldLeader: string;
  newLeader: string;
  messages: string[];
  ringPath?: string[];
  timestamp: number;
  status: string;
}

const ElectionsPage: React.FC = () => {
  const [nodes, setNodes] = useState<NodeInfo[]>([]);
  const [currentLeader, setCurrentLeader] = useState<string>('node3');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [lastElectionResult, setLastElectionResult] = useState<ElectionResult | null>(null);

  const fetchClusterState = async () => {
    try {
      const res = await api.get('/distributed/nodes');
      setNodes(res.data);
      const activeLeader = res.data.find((n: NodeInfo) => n.isLeader)?.nodeName || 'node3';
      setCurrentLeader(activeLeader);
    } catch (err) {
      console.error('Failed to fetch node cluster state', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchClusterState();
    const interval = setInterval(fetchClusterState, 4000);
    return () => clearInterval(interval);
  }, []);

  const runBullyElection = async (initiator: string) => {
    setSubmitting(true);
    try {
      const res = await api.post(`/distributed/election/bully/${initiator}`);
      setLastElectionResult(res.data);
      if (res.data.newLeader) setCurrentLeader(res.data.newLeader);
      fetchClusterState();
    } catch (err: any) {
      alert(`Bully election failed on ${initiator}`);
    } finally {
      setSubmitting(false);
    }
  };

  const runRingElection = async (initiator: string) => {
    setSubmitting(true);
    try {
      const res = await api.post(`/distributed/election/ring/${initiator}`);
      setLastElectionResult(res.data);
      if (res.data.newLeader) setCurrentLeader(res.data.newLeader);
      fetchClusterState();
    } catch (err: any) {
      alert(`Ring election failed on ${initiator}`);
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <div className="loading-spinner">Loading Cluster Topology & Election Engine...</div>;

  return (
    <div className="elections-container">
      <div className="dashboard-header">
        <h1>Leader Election Algorithms (Exp 4)</h1>
        <p>Bully Algorithm & Ring Leader Election across real distributed Node containers</p>
      </div>

      {/* CURRENT LEADER BANNER */}
      <div className="card full-width" style={{ marginBottom: '28px', borderLeft: '4px solid var(--accent-blue)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <span style={{ fontSize: '12px', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '1px' }}>Primary Matching Engine (Leader)</span>
          <h2 style={{ fontSize: '28px', fontWeight: 700, color: 'var(--green)', marginTop: '4px' }}>
            <ShieldCheck size={28} style={{ display: 'inline', verticalAlign: 'middle', marginRight: '8px' }} />
            {currentLeader.toUpperCase()} (Priority {currentLeader.replace('node', '')})
          </h2>
        </div>
        <div style={{ textAlign: 'right' }}>
          <span className="badge badge-deposit">MATCHING ENGINE ACTIVE</span>
          <p style={{ fontSize: '12px', color: 'var(--text-secondary)', marginTop: '4px' }}>Handling critical trade validations</p>
        </div>
      </div>

      {/* NODE CLUSTER TOPOLOGY */}
      <h2 style={{ fontSize: '18px', fontWeight: 700, marginBottom: '16px', color: 'var(--text-primary)' }}>
        Node Cluster Topology
      </h2>
      <div className="stats-grid">
        {['node1', 'node2', 'node3'].map((nodeName) => {
          const nodeData = nodes.find(n => n.nodeName === nodeName);
          const isOnline = nodeData && nodeData.status === 'ONLINE';
          const isLeader = nodeData?.isLeader || currentLeader === nodeName;

          return (
            <div key={nodeName} className="card" style={{ padding: '20px', border: isLeader ? '2px solid var(--accent-blue)' : undefined }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
                <h3 style={{ margin: 0 }}>{nodeName.toUpperCase()}</h3>
                <span className={`badge ${isOnline ? 'badge-deposit' : 'badge-withdraw'}`}>
                  {isOnline ? 'ONLINE' : 'OFFLINE'}
                </span>
              </div>

              <div style={{ fontSize: '14px', marginBottom: '8px', color: 'var(--text-secondary)' }}>
                Priority: <strong style={{ color: 'var(--text-primary)' }}>{nodeName.replace('node', '')}</strong>
              </div>
              <div style={{ fontSize: '14px', marginBottom: '16px' }}>
                Role: <span className={`badge ${isLeader ? 'badge-deposit' : 'badge-status-pending'}`}>
                  {isLeader ? 'PRIMARY ENGINE' : 'REPLICA / FOLLOWER'}
                </span>
              </div>

              <div style={{ display: 'flex', gap: '6px' }}>
                <button 
                  className="refresh-btn" 
                  style={{ flex: 1, fontSize: '11px', padding: '6px' }}
                  onClick={() => runBullyElection(nodeName)}
                  disabled={!isOnline || submitting}
                >
                  <Zap size={12} style={{ display: 'inline', marginRight: '2px' }} /> Bully
                </button>
                <button 
                  className="tab-btn active buy" 
                  style={{ flex: 1, fontSize: '11px', padding: '6px' }}
                  onClick={() => runRingElection(nodeName)}
                  disabled={!isOnline || submitting}
                >
                  <GitMerge size={12} style={{ display: 'inline', marginRight: '2px' }} /> Ring
                </button>
              </div>
            </div>
          );
        })}
      </div>

      {/* ELECTION EXECUTION TRACE */}
      {lastElectionResult && (
        <div className="card full-width" style={{ marginBottom: '28px' }}>
          <h3>Last Election Execution Trace ({lastElectionResult.algorithm} Algorithm)</h3>
          <div style={{ display: 'flex', gap: '24px', fontSize: '13px', marginBottom: '16px', padding: '12px', background: 'var(--bg-secondary)', borderRadius: 'var(--radius)' }}>
            <div>Initiator: <strong>{lastElectionResult.initiator}</strong></div>
            <div>Old Engine: <strong>{lastElectionResult.oldLeader}</strong></div>
            <div>New Primary Engine: <strong style={{ color: 'var(--green)' }}>{lastElectionResult.newLeader}</strong></div>
            <div>Status: <span className="badge badge-deposit">{lastElectionResult.status}</span></div>
          </div>

          <h4 style={{ fontSize: '13px', marginBottom: '8px', color: 'var(--text-secondary)' }}>Message Exchanges & Protocol Log:</h4>
          <div style={{ background: '#090d14', padding: '16px', borderRadius: 'var(--radius)', fontFamily: 'JetBrains Mono, monospace', fontSize: '12px', color: '#388bfd', maxHeight: '200px', overflowY: 'auto' }}>
            {lastElectionResult.messages.map((msg, i) => (
              <div key={i} style={{ marginBottom: '4px' }}>📌 {msg}</div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default ElectionsPage;
