import React, { useEffect, useState } from 'react';
import api from '../api/axios';
import { Send, Play, RefreshCw, GitCommit, Split, Clock, Activity, Zap, CheckCircle2, Sliders } from 'lucide-react';

interface NodeInfo {
  nodeId: string;
  nodeName: string;
  status: string;
  lamportClock: number;
  clockOffsetMs: number;
  driftRateMsPerSec?: number;
  simulatedPhysicalTimeMs?: number;
  wallClockTimeMs?: number;
}

interface EventLog {
  id: number;
  nodeId: string;
  eventType: string;
  lamportTimestamp: number;
  sourceNode: string;
  destinationNode: string;
  description: string;
  wallClockTime: string;
}

interface BerkeleyResult {
  coordinator: string;
  participatingNodes: string[];
  originalOffsets: Record<string, number>;
  averageOffsetMs: number;
  adjustmentsMs: Record<string, number>;
  finalOffsets: Record<string, number>;
}

interface VectorCompareResult {
  v1: Record<string, number>;
  v2: Record<string, number>;
  relationship: 'EQUAL' | 'BEFORE' | 'AFTER' | 'CONCURRENT';
  v1HappenedBeforeV2: boolean;
  isConcurrent: boolean;
}

const ClocksPage: React.FC = () => {
  const [nodes, setNodes] = useState<NodeInfo[]>([]);
  const [events, setEvents] = useState<EventLog[]>([]);
  const [vectorClocks, setVectorClocks] = useState<Record<string, Record<string, number>>>({});
  const [berkeleyResult, setBerkeleyResult] = useState<BerkeleyResult | null>(null);
  const [vectorCompareResult, setVectorCompareResult] = useState<VectorCompareResult | null>(null);
  const [loading, setLoading] = useState(true);
  const [actionMessage, setActionMessage] = useState<string | null>(null);

  // Vector comparison manual test inputs
  const [testV1, setTestV1] = useState('{"node1":2, "node2":1, "node3":0}');
  const [testV2, setTestV2] = useState('{"node1":1, "node2":2, "node3":0}');

  // Drift configuration inputs
  const [driftNode, setDriftNode] = useState('node1');
  const [driftRate, setDriftRate] = useState('2.5');
  const [manualOffset, setManualOffset] = useState('150');

  const fetchClockData = async () => {
    try {
      const [nodesRes, eventsRes] = await Promise.all([
        api.get('/distributed/nodes'),
        api.get('/distributed/events')
      ]);
      setNodes(nodesRes.data || []);
      setEvents(eventsRes.data || []);

      // Fetch vector clocks for all nodes
      const vcs: Record<string, Record<string, number>> = {};
      for (const n of ['node1', 'node2', 'node3']) {
        try {
          const vcRes = await api.get(`/distributed/vector/${n}`);
          if (vcRes.data?.vector) {
            vcs[n] = vcRes.data.vector;
          }
        } catch (ignored) {}
      }
      setVectorClocks(vcs);
    } catch (err) {
      console.error('Failed to fetch clock data', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchClockData();
    const interval = setInterval(fetchClockData, 4000);
    return () => clearInterval(interval);
  }, []);

  const triggerLocalEvent = async (nodeId: string) => {
    try {
      setActionMessage(`Triggering Lamport local event on ${nodeId}...`);
      await api.post(`/distributed/clock/event/${nodeId}`, { description: `Manual event on ${nodeId}` });
      setActionMessage(`Local event generated on ${nodeId}`);
      fetchClockData();
    } catch (err) {
      setActionMessage(`Failed to trigger event on ${nodeId}`);
    }
  };

  const sendMessage = async (fromNode: string, toNode: string) => {
    try {
      setActionMessage(`Sending Lamport message: ${fromNode} -> ${toNode}...`);
      await api.post('/distributed/clock/send', { fromNode, toNode });
      setActionMessage(`Lamport message sent: ${fromNode} -> ${toNode}`);
      fetchClockData();
    } catch (err) {
      setActionMessage(`Failed to send message from ${fromNode} to ${toNode}`);
    }
  };

  const triggerVectorLocalEvent = async (nodeId: string) => {
    try {
      setActionMessage(`Incrementing Vector Clock on ${nodeId}...`);
      await api.post(`/distributed/vector/event/${nodeId}`, { description: `Vector local event on ${nodeId}` });
      setActionMessage(`Vector Clock incremented on ${nodeId}`);
      fetchClockData();
    } catch (err) {
      setActionMessage(`Failed to update vector clock on ${nodeId}`);
    }
  };

  const sendVectorMessage = async (fromNode: string, toNode: string) => {
    try {
      setActionMessage(`Propagating Vector Clock: ${fromNode} -> ${toNode}...`);
      await api.post('/distributed/vector/send', { fromNode, toNode });
      setActionMessage(`Vector Clock propagated: ${fromNode} -> ${toNode}`);
      fetchClockData();
    } catch (err) {
      setActionMessage(`Failed to send vector message from ${fromNode} to ${toNode}`);
    }
  };

  const handleCompareVectors = async () => {
    try {
      const v1Parsed = JSON.parse(testV1);
      const v2Parsed = JSON.parse(testV2);
      const res = await api.post('/distributed/vector/compare', { v1: v1Parsed, v2: v2Parsed });
      setVectorCompareResult(res.data);
      setActionMessage(`Vector comparison evaluated: ${res.data.relationship}`);
    } catch (err: any) {
      alert(`Invalid JSON vector format: ${err.message}`);
    }
  };

  const handleApplyDrift = async () => {
    try {
      await api.post(`/distributed/berkeley/drift/${driftNode}`, {
        driftRateMsPerSec: parseFloat(driftRate),
        offsetMs: parseFloat(manualOffset)
      });
      setActionMessage(`Applied drift and offset to ${driftNode}`);
      fetchClockData();
    } catch (err) {
      setActionMessage(`Failed to apply drift to ${driftNode}`);
    }
  };

  const runBerkeleySync = async (coordinatorNode: string) => {
    try {
      setActionMessage(`Executing Berkeley Clock Sync with coordinator ${coordinatorNode}...`);
      const res = await api.post(`/distributed/berkeley/sync/${coordinatorNode}`);
      setBerkeleyResult(res.data);
      setActionMessage(`Berkeley Synchronization completed!`);
      fetchClockData();
    } catch (err) {
      setActionMessage(`Failed to run Berkeley Sync on ${coordinatorNode}`);
    }
  };

  if (loading) return <div className="loading-spinner">Loading Distributed Clocks...</div>;

  return (
    <div className="clocks-container">
      <div className="dashboard-header">
        <h1>Distributed Clock Systems (Phase 8)</h1>
        <p className="subtitle">
          Lamport Logical Clocks, Vector Clocks (Causality & Concurrency), and Berkeley Physical Clock Synchronization with Drift Modeling.
        </p>
      </div>

      {actionMessage && (
        <div className="alert alert-info" style={{ marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Activity size={16} /> {actionMessage}
        </div>
      )}

      {/* 1. NODE OVERVIEW CARDS */}
      <div className="nodes-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '16px', marginBottom: '24px' }}>
        {nodes.map(node => {
          const vc = vectorClocks[node.nodeName] || {};
          const nextNode = node.nodeName === 'node1' ? 'node2' : node.nodeName === 'node2' ? 'node3' : 'node1';

          return (
            <div key={node.nodeName} className="card node-card" style={{ borderTop: '4px solid #6366f1' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
                <h3 style={{ margin: 0 }}>{node.nodeName.toUpperCase()}</h3>
                <span className={`status-tag ${node.status === 'ONLINE' ? 'status-open' : 'status-danger'}`}>
                  {node.status}
                </span>
              </div>

              <div style={{ fontSize: '13px', display: 'flex', flexDirection: 'column', gap: '8px', marginBottom: '16px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span className="text-muted">Lamport Logical Clock:</span>
                  <strong className="text-primary font-mono">L = {node.lamportClock}</strong>
                </div>

                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span className="text-muted">Vector Clock VC:</span>
                  <span className="font-mono text-warning" style={{ fontWeight: 'bold' }}>
                    {JSON.stringify(vc)}
                  </span>
                </div>

                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span className="text-muted">Physical Offset:</span>
                  <span className="font-mono text-info">
                    {node.clockOffsetMs >= 0 ? `+${node.clockOffsetMs?.toFixed(1)}` : node.clockOffsetMs?.toFixed(1)} ms
                  </span>
                </div>

                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span className="text-muted">Simulated Physical Time:</span>
                  <span className="font-mono text-xs text-muted">
                    {node.simulatedPhysicalTimeMs ? new Date(node.simulatedPhysicalTimeMs).toLocaleTimeString() : '-'}
                  </span>
                </div>
              </div>

              {/* Action Buttons */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px', marginTop: '10px' }}>
                <button className="btn-secondary" style={{ fontSize: '11px', padding: '6px' }} onClick={() => triggerLocalEvent(node.nodeName)}>
                  <Play size={11} className="inline mr-1" /> Lamport +1
                </button>
                <button className="btn-secondary" style={{ fontSize: '11px', padding: '6px' }} onClick={() => sendMessage(node.nodeName, nextNode)}>
                  <Send size={11} className="inline mr-1" /> Send to {nextNode}
                </button>
                <button className="btn-primary" style={{ fontSize: '11px', padding: '6px' }} onClick={() => triggerVectorLocalEvent(node.nodeName)}>
                  <GitCommit size={11} className="inline mr-1" /> Vector +1
                </button>
                <button className="btn-primary" style={{ fontSize: '11px', padding: '6px' }} onClick={() => sendVectorMessage(node.nodeName, nextNode)}>
                  <Split size={11} className="inline mr-1" /> Propagate VC
                </button>
              </div>
            </div>
          );
        })}
      </div>

      {/* 2. VECTOR CLOCK CAUSALITY & CONCURRENCY COMPARISON */}
      <div className="card" style={{ marginBottom: '24px' }}>
        <div className="card-header">
          <h3>
            <Split size={18} className="inline mr-2 text-warning" />
            Vector Clock Causality & Concurrency Evaluator
          </h3>
          <span className="text-xs text-muted">Demonstrates causal precedence (BEFORE/AFTER) vs. concurrent events (CONCURRENT / ||)</span>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr auto', gap: '16px', alignItems: 'end', marginTop: '12px' }}>
          <div>
            <label className="text-xs font-semibold text-muted">Vector V1 (JSON)</label>
            <input
              type="text"
              value={testV1}
              onChange={e => setTestV1(e.target.value)}
              className="font-mono text-xs"
              style={{ width: '100%', padding: '8px', background: '#0f172a', border: '1px solid #334155', borderRadius: '4px', color: '#fff' }}
            />
          </div>
          <div>
            <label className="text-xs font-semibold text-muted">Vector V2 (JSON)</label>
            <input
              type="text"
              value={testV2}
              onChange={e => setTestV2(e.target.value)}
              className="font-mono text-xs"
              style={{ width: '100%', padding: '8px', background: '#0f172a', border: '1px solid #334155', borderRadius: '4px', color: '#fff' }}
            />
          </div>
          <button className="btn-primary" onClick={handleCompareVectors} style={{ height: '36px' }}>
            Evaluate Causality
          </button>
        </div>

        {vectorCompareResult && (
          <div style={{ background: '#0f172a', padding: '14px', borderRadius: '6px', marginTop: '16px', border: '1px solid #334155' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
              <span className="text-sm font-semibold">Causal Relationship:</span>
              <span className={`status-tag status-${vectorCompareResult.isConcurrent ? 'warning' : 'success'}`} style={{ fontSize: '13px', padding: '4px 10px' }}>
                {vectorCompareResult.relationship === 'CONCURRENT' ? 'CONCURRENT (V1 || V2)' : `V1 ${vectorCompareResult.relationship} V2`}
              </span>
            </div>
            <p className="text-xs text-muted" style={{ marginTop: '8px', marginBottom: 0 }}>
              {vectorCompareResult.relationship === 'CONCURRENT'
                ? 'Neither vector dominates across all components. These events happened concurrently without a causal link.'
                : vectorCompareResult.relationship === 'BEFORE'
                ? 'V1 happened strictly before V2 (V1 <= V2 component-wise and V1 != V2).'
                : vectorCompareResult.relationship === 'AFTER'
                ? 'V1 happened strictly after V2 (V2 <= V1 component-wise).'
                : 'Both vectors are component-wise identical.'}
            </p>
          </div>
        )}
      </div>

      {/* 3. SIMULATED PHYSICAL CLOCKS & BERKELEY SYNCHRONIZATION */}
      <div className="card" style={{ marginBottom: '24px' }}>
        <div className="card-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div>
            <h3>
              <Clock size={18} className="inline mr-2 text-info" />
              Berkeley Algorithm & Physical Clock Drift Modeling
            </h3>
            <span className="text-xs text-muted">Coordinator queries peer simulated physical clocks, computes average, and adjusts offsets. Host OS clock is never touched.</span>
          </div>
          <div style={{ display: 'flex', gap: '8px' }}>
            <button className="btn-secondary" onClick={() => runBerkeleySync('node1')}>
              Sync via Node 1
            </button>
            <button className="btn-secondary" onClick={() => runBerkeleySync('node2')}>
              Sync via Node 2
            </button>
          </div>
        </div>

        {/* Drift Simulation Configuration */}
        <div style={{ background: '#0f172a', padding: '12px', borderRadius: '6px', margin: '14px 0', border: '1px solid #334155', display: 'flex', gap: '16px', alignItems: 'flex-end' }}>
          <div>
            <label className="text-xs text-muted font-semibold">Target Node</label>
            <select value={driftNode} onChange={e => setDriftNode(e.target.value)} style={{ padding: '6px', background: '#1e293b', color: '#fff', borderRadius: '4px' }}>
              <option value="node1">node1</option>
              <option value="node2">node2</option>
              <option value="node3">node3</option>
            </select>
          </div>
          <div>
            <label className="text-xs text-muted font-semibold">Drift Rate (ms/sec)</label>
            <input type="number" step="0.5" value={driftRate} onChange={e => setDriftRate(e.target.value)} style={{ width: '110px', padding: '6px', background: '#1e293b', color: '#fff', borderRadius: '4px' }} />
          </div>
          <div>
            <label className="text-xs text-muted font-semibold">Base Offset (ms)</label>
            <input type="number" step="10" value={manualOffset} onChange={e => setManualOffset(e.target.value)} style={{ width: '110px', padding: '6px', background: '#1e293b', color: '#fff', borderRadius: '4px' }} />
          </div>
          <button className="btn-primary" onClick={handleApplyDrift} style={{ height: '34px', fontSize: '12px' }}>
            <Sliders size={13} className="inline mr-1" /> Configure Drift
          </button>
        </div>

        {berkeleyResult && (
          <div style={{ background: '#0f172a', padding: '16px', borderRadius: '6px', border: '1px solid #334155' }}>
            <div style={{ display: 'flex', gap: '24px', marginBottom: '14px', fontSize: '13px' }}>
              <div><strong>Coordinator:</strong> <span className="text-primary font-bold">{berkeleyResult.coordinator}</span></div>
              <div><strong>Calculated Cluster Average:</strong> <span className="text-success font-bold">{berkeleyResult.averageOffsetMs.toFixed(2)} ms</span></div>
            </div>

            <table className="tradex-table">
              <thead>
                <tr>
                  <th>Node</th>
                  <th>Pre-Sync Offset</th>
                  <th>Applied Correction</th>
                  <th>Post-Sync Converged Offset</th>
                </tr>
              </thead>
              <tbody>
                {berkeleyResult.participatingNodes.map(pNode => (
                  <tr key={pNode}>
                    <td className="font-bold">{pNode}</td>
                    <td className="font-mono">{berkeleyResult.originalOffsets[pNode]?.toFixed(2)} ms</td>
                    <td className={`font-mono ${(berkeleyResult.adjustmentsMs[pNode] || 0) >= 0 ? 'text-success' : 'text-danger'}`}>
                      {(berkeleyResult.adjustmentsMs[pNode] || 0) >= 0 ? '+' : ''}{berkeleyResult.adjustmentsMs[pNode]?.toFixed(2)} ms
                    </td>
                    <td className="font-mono font-bold text-success">
                      {berkeleyResult.finalOffsets[pNode]?.toFixed(2)} ms
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* 4. REAL DISTRIBUTED EVENT TIMELINE (LAMPORT ENGINE) */}
      <div className="card table-card">
        <div className="card-header">
          <h3>
            <Zap size={18} className="inline mr-2 text-warning" />
            Distributed Event Log with Lamport Logical Order
          </h3>
          <span className="text-xs text-muted">Total causal ordering tie-breaker: (Lamport Timestamp L, Node ID)</span>
        </div>
        <div className="table-responsive">
          <table className="tradex-table">
            <thead>
              <tr>
                <th>Lamport Time</th>
                <th>Node</th>
                <th>Event Type</th>
                <th>Source</th>
                <th>Destination</th>
                <th>Description</th>
                <th>Wall Clock</th>
              </tr>
            </thead>
            <tbody>
              {events.length === 0 ? (
                <tr>
                  <td colSpan={7} className="text-center py-4">No distributed events recorded yet.</td>
                </tr>
              ) : (
                events.slice(0, 20).map(ev => (
                  <tr key={ev.id || Math.random()}>
                    <td className="font-mono text-primary font-bold">L = {ev.lamportTimestamp}</td>
                    <td className="font-semibold">{ev.nodeId}</td>
                    <td>
                      <span className="badge-pill badge-secondary text-xs">{ev.eventType}</span>
                    </td>
                    <td>{ev.sourceNode || '-'}</td>
                    <td>{ev.destinationNode || '-'}</td>
                    <td className="text-xs">{ev.description}</td>
                    <td className="text-xs text-muted">{new Date(ev.wallClockTime).toLocaleTimeString()}</td>
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

export default ClocksPage;
