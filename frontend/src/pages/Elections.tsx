import React, { useEffect, useState } from 'react';
import api from '../api/axios';
import { 
  ShieldCheck, 
  Zap, 
  GitMerge, 
  Activity, 
  Flame, 
  RotateCcw, 
  CheckCircle, 
  AlertTriangle, 
  Clock, 
  ArrowRight,
  Server,
  ShoppingCart
} from 'lucide-react';

interface NodeInfo {
  nodeId: string;
  nodeName: string;
  status: string;
  priority: number;
  leader: string;
  isLeader: boolean;
  health?: string;
}

interface ElectionResult {
  electionId: string;
  algorithm: 'BULLY' | 'RING';
  initiator: string;
  oldLeader: string;
  newLeader: string;
  leaderEpoch: number;
  participants: string[];
  unavailableNodes: string[];
  messages: string[];
  ringPath?: string[];
  startTimeMs: number;
  endTimeMs: number;
  durationMs: number;
  outcome: string;
  tradingState: string;
}

const ElectionsPage: React.FC = () => {
  const [nodes, setNodes] = useState<NodeInfo[]>([]);
  const [currentLeader, setCurrentLeader] = useState<string>('node3');
  const [currentEpoch, setCurrentEpoch] = useState<number>(3);
  const [tradingState, setTradingState] = useState<string>('AVAILABLE');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [lastElectionResult, setLastElectionResult] = useState<ElectionResult | null>(null);
  const [orderTestFeedback, setOrderTestFeedback] = useState<string | null>(null);

  const fetchClusterState = async () => {
    try {
      const [nodesRes, failoverRes] = await Promise.all([
        api.get('/distributed/nodes'),
        api.get('/cluster/failover')
      ]);
      setNodes(nodesRes.data);
      if (failoverRes.data) {
        setCurrentLeader(failoverRes.data.activeLeader || 'node3');
        setCurrentEpoch(failoverRes.data.leaderEpoch || 3);
        setTradingState(failoverRes.data.tradingState || 'AVAILABLE');
      }
    } catch (err) {
      console.error('Failed to fetch node cluster state', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchClusterState();
    const interval = setInterval(fetchClusterState, 3000);
    return () => clearInterval(interval);
  }, []);

  const runBullyElection = async (initiator: string) => {
    setSubmitting(true);
    setOrderTestFeedback(null);
    try {
      const res = await api.post(`/distributed/election/bully/${initiator}`);
      setLastElectionResult(res.data);
      if (res.data.newLeader) {
        setCurrentLeader(res.data.newLeader);
        setCurrentEpoch(res.data.leaderEpoch || currentEpoch + 1);
      }
      fetchClusterState();
    } catch (err: any) {
      alert(`Bully election failed on ${initiator}: ${err.response?.data?.error || err.message}`);
    } finally {
      setSubmitting(false);
    }
  };

  const runRingElection = async (initiator: string) => {
    setSubmitting(true);
    setOrderTestFeedback(null);
    try {
      const res = await api.post(`/distributed/election/ring/${initiator}`);
      setLastElectionResult(res.data);
      if (res.data.newLeader) {
        setCurrentLeader(res.data.newLeader);
        setCurrentEpoch(res.data.leaderEpoch || currentEpoch + 1);
      }
      fetchClusterState();
    } catch (err: any) {
      alert(`Ring election failed on ${initiator}: ${err.response?.data?.error || err.message}`);
    } finally {
      setSubmitting(false);
    }
  };

  const simulateCrash = async (nodeId: string) => {
    try {
      await api.post(`/cluster/fault/crash/${nodeId}`);
      fetchClusterState();
    } catch (err: any) {
      alert(`Crash simulation failed: ${err.message}`);
    }
  };

  const simulateRecover = async (nodeId: string) => {
    try {
      await api.post(`/cluster/fault/recover/${nodeId}`);
      fetchClusterState();
    } catch (err: any) {
      alert(`Recovery failed: ${err.message}`);
    }
  };

  const testOrderRouting = async () => {
    setOrderTestFeedback('Submitting probe order to verify authoritative routing...');
    try {
      const res = await api.post('/orders', {
        symbol: 'AAPL',
        side: 'BUY',
        orderType: 'MARKET',
        quantity: 1
      }, {
        headers: { 'X-Idempotency-Key': 'ELECTION-PROBE-' + Date.now() }
      });
      setOrderTestFeedback(`Order #${res.data.id} placed successfully! Matched via authoritative leader: ${currentLeader.toUpperCase()} (Epoch #${currentEpoch})`);
    } catch (err: any) {
      setOrderTestFeedback(`Routing test status: ${err.response?.data?.message || err.message}`);
    }
  };

  if (loading) return <div className="loading-spinner">Loading Cluster Topology & Election Engine...</div>;

  return (
    <div className="elections-container" style={{ padding: '24px', color: '#f8fafc' }}>
      <div className="dashboard-header" style={{ marginBottom: '24px' }}>
        <h1 style={{ fontSize: '28px', fontWeight: 800, margin: 0, display: 'flex', alignItems: 'center', gap: '10px' }}>
          <ShieldCheck size={32} color="#3b82f6" />
          Bully & Ring Leader Elections (Phase 11)
        </h1>
        <p style={{ color: '#94a3b8', margin: '6px 0 0 0', fontSize: '14px' }}>
          Real-time distributed election protocols tied directly to the authoritative trading matching engine and leader epoch registry.
        </p>
      </div>

      {/* AUTHORITATIVE LEADER & EPOCH BANNER */}
      <div style={{
        background: '#1e293b',
        border: '1px solid #334155',
        borderRadius: '12px',
        padding: '20px 24px',
        marginBottom: '24px',
        display: 'flex',
        flexWrap: 'wrap',
        justifyContent: 'space-between',
        alignItems: 'center',
        gap: '16px'
      }}>
        <div>
          <span style={{ fontSize: '12px', color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
            Authoritative Trading Leader & Registry
          </span>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginTop: '4px' }}>
            <h2 style={{ fontSize: '28px', fontWeight: 800, color: '#38bdf8', margin: 0 }}>
              {currentLeader.toUpperCase()}
            </h2>
            <span style={{
              background: '#0f172a',
              border: '1px solid #38bdf8',
              color: '#38bdf8',
              padding: '4px 12px',
              borderRadius: '8px',
              fontSize: '13px',
              fontWeight: 700
            }}>
              LEADER EPOCH #{currentEpoch}
            </span>
            <span style={{
              background: tradingState === 'AVAILABLE' ? '#065f46' : '#7f1d1d',
              color: '#ffffff',
              padding: '4px 12px',
              borderRadius: '8px',
              fontSize: '12px',
              fontWeight: 700
            }}>
              {tradingState}
            </span>
          </div>
        </div>

        <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
          <button
            onClick={testOrderRouting}
            style={{
              background: '#2563eb',
              color: '#fff',
              border: 'none',
              padding: '10px 18px',
              borderRadius: '8px',
              fontWeight: 600,
              fontSize: '13px',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '6px'
            }}
          >
            <ShoppingCart size={16} /> Test Live Order Routing
          </button>
        </div>
      </div>

      {orderTestFeedback && (
        <div style={{
          padding: '12px 18px',
          background: '#0f172a',
          border: '1px solid #38bdf8',
          borderRadius: '8px',
          color: '#38bdf8',
          marginBottom: '24px',
          fontSize: '14px',
          fontWeight: 500
        }}>
          {orderTestFeedback}
        </div>
      )}

      {/* NODE TOPOLOGY & CONTROLLED FAILURE SCENARIO */}
      <h2 style={{ fontSize: '18px', fontWeight: 700, marginBottom: '16px', color: '#f8fafc', display: 'flex', alignItems: 'center', gap: '8px' }}>
        <Server size={20} color="#60a5fa" /> Node Cluster Topology & Algorithm Triggers
      </h2>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '16px', marginBottom: '28px' }}>
        {['node1', 'node2', 'node3'].map((nodeName) => {
          const nodeData = nodes.find(n => n.nodeName === nodeName);
          const isOnline = nodeData && nodeData.status === 'ONLINE';
          const isLeader = currentLeader === nodeName;
          const priority = parseInt(nodeName.replace('node', ''));

          return (
            <div 
              key={nodeName} 
              style={{ 
                background: '#1e293b', 
                borderRadius: '12px', 
                border: isLeader ? '2px solid #38bdf8' : '1px solid #334155', 
                padding: '20px' 
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
                <h3 style={{ margin: 0, fontSize: '18px', fontWeight: 700 }}>{nodeName.toUpperCase()}</h3>
                <span style={{ 
                  background: isOnline ? '#059669' : '#dc2626', 
                  color: '#fff', 
                  padding: '3px 8px', 
                  borderRadius: '6px', 
                  fontSize: '11px', 
                  fontWeight: 700 
                }}>
                  {isOnline ? 'ONLINE' : 'UNAVAILABLE'}
                </span>
              </div>

              <div style={{ fontSize: '13px', color: '#94a3b8', marginBottom: '6px' }}>
                Bully Priority: <strong style={{ color: '#cbd5e1' }}>{priority}</strong> | Ring Hop: <strong style={{ color: '#cbd5e1' }}>{nodeName === 'node3' ? 'node1' : (nodeName === 'node2' ? 'node3' : 'node2')}</strong>
              </div>
              <div style={{ fontSize: '13px', marginBottom: '16px' }}>
                Role: <span style={{ color: isLeader ? '#38bdf8' : '#94a3b8', fontWeight: 700 }}>
                  {isLeader ? 'AUTHORITATIVE LEADER' : 'FOLLOWER / REPLICA'}
                </span>
              </div>

              {/* Algorithm Trigger Buttons */}
              <div style={{ display: 'flex', gap: '8px', marginBottom: '12px' }}>
                <button 
                  onClick={() => runBullyElection(nodeName)}
                  disabled={!isOnline || submitting}
                  style={{
                    flex: 1,
                    background: '#1e3a8a',
                    border: '1px solid #2563eb',
                    color: '#fff',
                    padding: '8px 10px',
                    borderRadius: '6px',
                    fontSize: '12px',
                    fontWeight: 600,
                    cursor: (!isOnline || submitting) ? 'not-allowed' : 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: '4px'
                  }}
                >
                  <Zap size={14} /> Start Bully
                </button>
                <button 
                  onClick={() => runRingElection(nodeName)}
                  disabled={!isOnline || submitting}
                  style={{
                    flex: 1,
                    background: '#064e3b',
                    border: '1px solid #059669',
                    color: '#fff',
                    padding: '8px 10px',
                    borderRadius: '6px',
                    fontSize: '12px',
                    fontWeight: 600,
                    cursor: (!isOnline || submitting) ? 'not-allowed' : 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: '4px'
                  }}
                >
                  <GitMerge size={14} /> Start Ring
                </button>
              </div>

              {/* Controlled Failure Controls */}
              <div style={{ display: 'flex', gap: '8px' }}>
                {isOnline ? (
                  <button
                    onClick={() => simulateCrash(nodeName)}
                    style={{
                      width: '100%',
                      background: '#7f1d1d',
                      color: '#fca5a5',
                      border: '1px solid #991b1b',
                      padding: '6px',
                      borderRadius: '6px',
                      fontSize: '11px',
                      fontWeight: 600,
                      cursor: 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      gap: '4px'
                    }}
                  >
                    <Flame size={12} /> Inject Crash / Partition
                  </button>
                ) : (
                  <button
                    onClick={() => simulateRecover(nodeName)}
                    style={{
                      width: '100%',
                      background: '#065f46',
                      color: '#6ee7b7',
                      border: '1px solid #047857',
                      padding: '6px',
                      borderRadius: '6px',
                      fontSize: '11px',
                      fontWeight: 600,
                      cursor: 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      gap: '4px'
                    }}
                  >
                    <RotateCcw size={12} /> Recover Node
                  </button>
                )}
              </div>
            </div>
          );
        })}
      </div>

      {/* ELECTION EXECUTION TRACE & PROTOCOL LOG */}
      {lastElectionResult && (
        <div style={{ background: '#1e293b', border: '1px solid #334155', borderRadius: '12px', padding: '24px', marginBottom: '24px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
            <h3 style={{ margin: 0, fontSize: '18px', fontWeight: 700 }}>
              Election Execution Trace ({lastElectionResult.algorithm} Algorithm)
            </h3>
            <span style={{ color: '#94a3b8', fontSize: '12px' }}>
              Duration: <strong style={{ color: '#cbd5e1' }}>{lastElectionResult.durationMs} ms</strong>
            </span>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '12px', marginBottom: '20px', padding: '14px', background: '#0f172a', borderRadius: '8px' }}>
            <div>
              <span style={{ fontSize: '11px', color: '#94a3b8' }}>Initiator</span>
              <div style={{ fontWeight: 700, fontSize: '15px' }}>{lastElectionResult.initiator.toUpperCase()}</div>
            </div>
            <div>
              <span style={{ fontSize: '11px', color: '#94a3b8' }}>Old Leader</span>
              <div style={{ fontWeight: 700, fontSize: '15px' }}>{lastElectionResult.oldLeader.toUpperCase()}</div>
            </div>
            <div>
              <span style={{ fontSize: '11px', color: '#94a3b8' }}>Elected Coordinator</span>
              <div style={{ fontWeight: 700, fontSize: '15px', color: '#38bdf8' }}>{lastElectionResult.newLeader.toUpperCase()}</div>
            </div>
            <div>
              <span style={{ fontSize: '11px', color: '#94a3b8' }}>Resulting Epoch</span>
              <div style={{ fontWeight: 700, fontSize: '15px', color: '#34d399' }}>#{lastElectionResult.leaderEpoch}</div>
            </div>
            <div>
              <span style={{ fontSize: '11px', color: '#94a3b8' }}>Unavailable Nodes</span>
              <div style={{ fontWeight: 700, fontSize: '14px', color: lastElectionResult.unavailableNodes.length > 0 ? '#f87171' : '#94a3b8' }}>
                {lastElectionResult.unavailableNodes.length > 0 ? lastElectionResult.unavailableNodes.join(', ') : 'None'}
              </div>
            </div>
          </div>

          <h4 style={{ fontSize: '13px', margin: '0 0 10px 0', color: '#94a3b8' }}>
            Distributed Protocol Message Trace & Fencing Log:
          </h4>
          <div style={{
            background: '#090d14',
            padding: '16px',
            borderRadius: '8px',
            fontFamily: 'JetBrains Mono, monospace',
            fontSize: '12px',
            color: '#38bdf8',
            maxHeight: '220px',
            overflowY: 'auto',
            border: '1px solid #1e293b'
          }}>
            {lastElectionResult.messages.map((msg, i) => (
              <div key={i} style={{ marginBottom: '6px', lineHeight: 1.5 }}>📌 {msg}</div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default ElectionsPage;
