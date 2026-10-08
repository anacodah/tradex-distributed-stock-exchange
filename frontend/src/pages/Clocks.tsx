import React, { useEffect, useState } from 'react';
import api from '../api/axios';
import { Send, Play, RefreshCw } from 'lucide-react';

interface NodeInfo {
  nodeId: string;
  nodeName: string;
  status: string;
  lamportClock: number;
  clockOffsetMs: number;
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

const ClocksPage: React.FC = () => {
  const [nodes, setNodes] = useState<NodeInfo[]>([]);
  const [events, setEvents] = useState<EventLog[]>([]);
  const [berkeleyResult, setBerkeleyResult] = useState<BerkeleyResult | null>(null);
  const [loading, setLoading] = useState(true);
  const [actionMessage, setActionMessage] = useState<string | null>(null);

  const fetchClockData = async () => {
    try {
      const [nodesRes, eventsRes] = await Promise.all([
        api.get('/distributed/nodes'),
        api.get('/distributed/events')
      ]);
      setNodes(nodesRes.data);
      setEvents(eventsRes.data);
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
      setActionMessage(`Triggering local event on ${nodeId}...`);
      await api.post(`/distributed/clock/event/${nodeId}`, { description: `Manual event on ${nodeId}` });
      setActionMessage(`Local event generated on ${nodeId}`);
      fetchClockData();
    } catch (err: any) {
      setActionMessage(`Failed to trigger event on ${nodeId}`);
    }
  };

  const sendMessage = async (fromNode: string, toNode: string) => {
    try {
      setActionMessage(`Sending message from ${fromNode} -> ${toNode}...`);
      await api.post('/distributed/clock/send', { fromNode, toNode });
      setActionMessage(`Message sent successfully: ${fromNode} -> ${toNode}`);
      fetchClockData();
    } catch (err: any) {
      setActionMessage(`Failed to send message from ${fromNode} to ${toNode}`);
    }
  };

  const runBerkeleySync = async (coordinatorNode: string) => {
    try {
      setActionMessage(`Executing Berkeley Clock Sync with coordinator ${coordinatorNode}...`);
      const res = await api.post(`/distributed/berkeley/sync/${coordinatorNode}`);
      setBerkeleyResult(res.data);
      setActionMessage(`Berkeley Synchronization completed!`);
      fetchClockData();
    } catch (err: any) {
      setActionMessage(`Failed to run Berkeley Sync on ${coordinatorNode}`);
    }
  };

  if (loading) return <div className="loading-spinner">Loading Distributed Clocks...</div>;

  return (
    <div className="clocks-container">
      <div className="dashboard-header">
        <h1>Distributed Clock Synchronization (Exp 3)</h1>
        <p>Real-time Lamport Logical Clocks & Berkeley Physical Clock Synchronization across Node Containers</p>
      </div>

      {actionMessage && (
        <div className="alert alert-success" style={{ marginBottom: '20px' }}>
          {actionMessage}
        </div>
      )}

      {/* LAMPORT CLOCKS SECTION */}
      <h2 style={{ fontSize: '18px', fontWeight: 700, marginBottom: '16px', color: 'var(--text-primary)' }}>
        1. Lamport Logical Clocks
      </h2>

      <div className="stats-grid">
        {['node1', 'node2', 'node3'].map((nodeName) => {
          const nodeData = nodes.find(n => n.nodeName === nodeName);
          const isOnline = nodeData && nodeData.status === 'ONLINE';

          return (
            <div key={nodeName} className="card" style={{ padding: '20px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
                <h3 style={{ margin: 0 }}>{nodeName.toUpperCase()}</h3>
                <span className={`badge ${isOnline ? 'badge-deposit' : 'badge-withdraw'}`}>
                  {isOnline ? 'ONLINE' : 'OFFLINE'}
                </span>
              </div>
              <div style={{ fontSize: '32px', fontWeight: 700, color: 'var(--accent-blue)', marginBottom: '12px' }}>
                L = {nodeData?.lamportClock ?? 0}
              </div>
              <div style={{ fontSize: '12px', color: 'var(--text-muted)', marginBottom: '16px' }}>
                Offset: {nodeData?.clockOffsetMs?.toFixed(2) ?? '0.00'} ms
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                <button 
                  className="refresh-btn" 
                  style={{ width: '100%', fontSize: '12px', padding: '6px 12px' }}
                  onClick={() => triggerLocalEvent(nodeName)}
                  disabled={!isOnline}
                >
                  <Play size={12} style={{ display: 'inline', marginRight: '4px' }} /> Local Event
                </button>
                <button 
                  className="tab-btn active buy" 
                  style={{ width: '100%', fontSize: '12px', padding: '6px 12px' }}
                  onClick={() => sendMessage(nodeName, nodeName === 'node1' ? 'node2' : nodeName === 'node2' ? 'node3' : 'node1')}
                  disabled={!isOnline}
                >
                  <Send size={12} style={{ display: 'inline', marginRight: '4px' }} /> Send to Next Node
                </button>
              </div>
            </div>
          );
        })}
      </div>

      {/* BERKELEY SYNCHRONIZATION SECTION */}
      <div className="card full-width" style={{ marginBottom: '28px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          <div>
            <h3>2. Berkeley Clock Synchronization Algorithm</h3>
            <p style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>
              Coordinator collects physical offsets, calculates average, and distributes individual adjustments.
            </p>
          </div>
          <button className="refresh-btn" onClick={() => runBerkeleySync('node1')}>
            <RefreshCw size={14} style={{ display: 'inline', marginRight: '6px' }} /> Synchronize via Node 1
          </button>
        </div>

        {berkeleyResult && (
          <div style={{ background: 'var(--bg-secondary)', padding: '16px', borderRadius: 'var(--radius)', border: '1px solid var(--border)' }}>
            <div style={{ display: 'flex', gap: '20px', marginBottom: '16px', fontSize: '14px' }}>
              <div><strong>Coordinator:</strong> <span style={{ color: 'var(--accent-blue)' }}>{berkeleyResult.coordinator}</span></div>
              <div><strong>Calculated Average Offset:</strong> <span style={{ color: 'var(--green)' }}>{berkeleyResult.averageOffsetMs.toFixed(2)} ms</span></div>
            </div>

            <table className="data-table">
              <thead>
                <tr>
                  <th>Node</th>
                  <th>Original Offset</th>
                  <th>Applied Adjustment</th>
                  <th>Final Synchronized Offset</th>
                </tr>
              </thead>
              <tbody>
                {berkeleyResult.participatingNodes.map(pNode => (
                  <tr key={pNode}>
                    <td><strong>{pNode}</strong></td>
                    <td>{berkeleyResult.originalOffsets[pNode]?.toFixed(2)} ms</td>
                    <td className={(berkeleyResult.adjustmentsMs[pNode] || 0) >= 0 ? 'text-green' : 'text-red'}>
                      {(berkeleyResult.adjustmentsMs[pNode] || 0) >= 0 ? '+' : ''}{berkeleyResult.adjustmentsMs[pNode]?.toFixed(2)} ms
                    </td>
                    <td><strong>{berkeleyResult.finalOffsets[pNode]?.toFixed(2)} ms</strong></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* RECENT DISTRIBUTED EVENTS TIMELINE */}
      <div className="card full-width">
        <h3>Distributed Event Timeline (Lamport Engine)</h3>
        {events.length === 0 ? (
          <p style={{ color: 'var(--text-muted)', padding: '12px 0' }}>No distributed events recorded yet.</p>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>Lamport L</th>
                <th>Node</th>
                <th>Type</th>
                <th>Source</th>
                <th>Destination</th>
                <th>Description</th>
                <th>Wall Clock</th>
              </tr>
            </thead>
            <tbody>
              {events.slice(0, 15).map(ev => (
                <tr key={ev.id || Math.random()}>
                  <td><strong style={{ color: 'var(--accent-blue)' }}>L = {ev.lamportTimestamp}</strong></td>
                  <td>{ev.nodeId}</td>
                  <td><span className="badge badge-deposit">{ev.eventType}</span></td>
                  <td>{ev.sourceNode || '-'}</td>
                  <td>{ev.destinationNode || '-'}</td>
                  <td>{ev.description}</td>
                  <td>{new Date(ev.wallClockTime).toLocaleTimeString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
};

export default ClocksPage;
