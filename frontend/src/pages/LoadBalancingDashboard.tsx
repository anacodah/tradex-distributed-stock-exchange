import React, { useState, useEffect } from 'react';
import api from '../api/axios';
import { 
  GitMerge, 
  RotateCcw, 
  Play, 
  CheckCircle, 
  AlertTriangle, 
  Activity, 
  Server, 
  Zap, 
  TrendingUp, 
  ShieldCheck, 
  Clock, 
  RefreshCw 
} from 'lucide-react';

interface NodeLoadStats {
  nodeName: string;
  healthy: boolean;
  role: string;
  totalRequests: number;
  activeRequests: number;
  completedRequests: number;
  failedRequests: number;
  avgLatencyMs: number;
  errorRatePercent: number;
  lastRequestTimestamp: number;
}

interface RoutingDecision {
  timestamp: number;
  category: string;
  targetNode: string;
  strategy: string;
  reason: string;
}

interface LoadBalancerReport {
  currentAlgorithm: 'ROUND_ROBIN' | 'LEAST_CONNECTIONS';
  authoritativeLeader: string;
  leaderEpoch: number;
  nodeStats: Record<string, NodeLoadStats>;
  recentRoutingDecisions: RoutingDecision[];
  totalDispatchedRequests: number;
  timestamp: number;
}

const LoadBalancingDashboard: React.FC = () => {
  const [report, setReport] = useState<LoadBalancerReport | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [testCount, setTestCount] = useState<number>(30);
  const [loadTesting, setLoadTesting] = useState<boolean>(false);
  const [testResult, setTestResult] = useState<any | null>(null);

  const fetchReport = async () => {
    try {
      const res = await api.get('/cluster/load-balancer');
      setReport(res.data);
    } catch (err: any) {
      console.error('Failed fetching load balancer metrics', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchReport();
    const interval = setInterval(fetchReport, 2000);
    return () => clearInterval(interval);
  }, []);

  const handleAlgorithmChange = async (algo: 'ROUND_ROBIN' | 'LEAST_CONNECTIONS') => {
    try {
      await api.post('/cluster/load-balancer/algorithm', { algorithm: algo });
      fetchReport();
    } catch (err: any) {
      alert(`Failed changing algorithm: ${err.message}`);
    }
  };

  const handleRunLoadTest = async () => {
    setLoadTesting(true);
    setTestResult(null);
    try {
      const res = await api.post('/cluster/load-balancer/load-test', {
        count: testCount,
        concurrency: 5
      });
      setTestResult(res.data);
      fetchReport();
    } catch (err: any) {
      alert(`Load test failed: ${err.message}`);
    } finally {
      setLoadTesting(false);
    }
  };

  if (loading && !report) {
    return <div style={{ padding: '24px', color: '#94a3b8' }}>Loading Distributed Load Balancer...</div>;
  }

  return (
    <div className="load-balancer-dashboard" style={{ padding: '24px', color: '#f8fafc' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '24px' }}>
        <div>
          <h1 style={{ fontSize: '28px', fontWeight: 800, margin: 0, display: 'flex', alignItems: 'center', gap: '10px' }}>
            <GitMerge size={32} color="#3b82f6" />
            Distributed Load Balancing (Phase 12)
          </h1>
          <p style={{ color: '#94a3b8', margin: '6px 0 0 0', fontSize: '14px' }}>
            Multi-strategy routing (Round Robin & Health-Aware Least Connections) with category-based routing policies.
          </p>
        </div>
        <button 
          onClick={fetchReport}
          style={{
            background: '#1e293b',
            border: '1px solid #334155',
            color: '#f8fafc',
            padding: '8px 16px',
            borderRadius: '8px',
            cursor: 'pointer',
            fontSize: '13px',
            display: 'flex',
            alignItems: 'center',
            gap: '6px'
          }}
        >
          <RefreshCw size={14} /> Refresh Metrics
        </button>
      </div>

      {/* OVERVIEW STATS CARDS */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '16px', marginBottom: '24px' }}>
        <div style={{ background: '#1e293b', border: '1px solid #334155', borderRadius: '12px', padding: '18px' }}>
          <div style={{ fontSize: '12px', color: '#94a3b8', textTransform: 'uppercase' }}>Active Strategy</div>
          <div style={{ fontSize: '20px', fontWeight: 800, color: '#38bdf8', marginTop: '6px' }}>
            {report?.currentAlgorithm === 'ROUND_ROBIN' ? 'Round Robin' : 'Least Connections'}
          </div>
          <div style={{ display: 'flex', gap: '6px', marginTop: '10px' }}>
            <button
              onClick={() => handleAlgorithmChange('ROUND_ROBIN')}
              style={{
                flex: 1,
                padding: '4px 8px',
                fontSize: '11px',
                fontWeight: 600,
                borderRadius: '6px',
                border: '1px solid #38bdf8',
                background: report?.currentAlgorithm === 'ROUND_ROBIN' ? '#38bdf8' : 'transparent',
                color: report?.currentAlgorithm === 'ROUND_ROBIN' ? '#0f172a' : '#38bdf8',
                cursor: 'pointer'
              }}
            >
              Round Robin
            </button>
            <button
              onClick={() => handleAlgorithmChange('LEAST_CONNECTIONS')}
              style={{
                flex: 1,
                padding: '4px 8px',
                fontSize: '11px',
                fontWeight: 600,
                borderRadius: '6px',
                border: '1px solid #38bdf8',
                background: report?.currentAlgorithm === 'LEAST_CONNECTIONS' ? '#38bdf8' : 'transparent',
                color: report?.currentAlgorithm === 'LEAST_CONNECTIONS' ? '#0f172a' : '#38bdf8',
                cursor: 'pointer'
              }}
            >
              Least Conn.
            </button>
          </div>
        </div>

        <div style={{ background: '#1e293b', border: '1px solid #334155', borderRadius: '12px', padding: '18px' }}>
          <div style={{ fontSize: '12px', color: '#94a3b8', textTransform: 'uppercase' }}>Authoritative Leader</div>
          <div style={{ fontSize: '24px', fontWeight: 800, color: '#34d399', marginTop: '6px' }}>
            {report?.authoritativeLeader?.toUpperCase() || 'NODE3'}
          </div>
          <div style={{ fontSize: '12px', color: '#64748b', marginTop: '4px' }}>
            Epoch #{report?.leaderEpoch || 3} (Strict Order Writes Target)
          </div>
        </div>

        <div style={{ background: '#1e293b', border: '1px solid #334155', borderRadius: '12px', padding: '18px' }}>
          <div style={{ fontSize: '12px', color: '#94a3b8', textTransform: 'uppercase' }}>Total Dispatched Requests</div>
          <div style={{ fontSize: '24px', fontWeight: 800, color: '#f8fafc', marginTop: '6px' }}>
            {report?.totalDispatchedRequests || 0}
          </div>
          <div style={{ fontSize: '12px', color: '#64748b', marginTop: '4px' }}>
            Read queries & load test probes
          </div>
        </div>

        <div style={{ background: '#1e293b', border: '1px solid #334155', borderRadius: '12px', padding: '18px' }}>
          <div style={{ fontSize: '12px', color: '#94a3b8', textTransform: 'uppercase' }}>Routing Policy</div>
          <div style={{ fontSize: '13px', fontWeight: 600, color: '#cbd5e1', marginTop: '6px' }}>
            • Reads: Load balanced<br/>
            • Matching Writes: Leader only
          </div>
        </div>
      </div>

      {/* NODE METRICS TABLE */}
      <div style={{ background: '#1e293b', border: '1px solid #334155', borderRadius: '12px', padding: '20px', marginBottom: '24px' }}>
        <h2 style={{ fontSize: '18px', fontWeight: 700, margin: '0 0 16px 0', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Server size={20} color="#60a5fa" /> Per-Node Request Counters & Concurrency
        </h2>

        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '14px' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid #334155', color: '#94a3b8' }}>
                <th style={{ padding: '12px 16px' }}>NODE</th>
                <th style={{ padding: '12px 16px' }}>ROLE</th>
                <th style={{ padding: '12px 16px' }}>HEALTH</th>
                <th style={{ padding: '12px 16px' }}>TOTAL REQS</th>
                <th style={{ padding: '12px 16px' }}>ACTIVE (CONCURRENT)</th>
                <th style={{ padding: '12px 16px' }}>COMPLETED</th>
                <th style={{ padding: '12px 16px' }}>AVG LATENCY</th>
                <th style={{ padding: '12px 16px' }}>ERROR RATE</th>
              </tr>
            </thead>
            <tbody>
              {['node1', 'node2', 'node3'].map((nodeId) => {
                const stat = report?.nodeStats?.[nodeId];
                const isLeader = report?.authoritativeLeader === nodeId;
                const isHealthy = stat?.healthy ?? true;

                return (
                  <tr key={nodeId} style={{ borderBottom: '1px solid #1e293b' }}>
                    <td style={{ padding: '14px 16px', fontWeight: 700 }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <div style={{ 
                          width: '10px', 
                          height: '10px', 
                          borderRadius: '50%', 
                          background: isHealthy ? '#10b981' : '#ef4444' 
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
                      <span style={{ color: isHealthy ? '#10b981' : '#ef4444', fontWeight: 600 }}>
                        {isHealthy ? 'HEALTHY' : 'UNAVAILABLE'}
                      </span>
                    </td>
                    <td style={{ padding: '14px 16px', fontWeight: 600 }}>
                      {stat?.totalRequests || 0}
                    </td>
                    <td style={{ padding: '14px 16px', color: '#38bdf8', fontWeight: 700 }}>
                      {stat?.activeRequests || 0}
                    </td>
                    <td style={{ padding: '14px 16px', color: '#34d399' }}>
                      {stat?.completedRequests || 0}
                    </td>
                    <td style={{ padding: '14px 16px' }}>
                      {stat?.avgLatencyMs ? `${stat.avgLatencyMs.toFixed(1)} ms` : '< 1 ms'}
                    </td>
                    <td style={{ padding: '14px 16px', color: (stat?.errorRatePercent || 0) > 0 ? '#ef4444' : '#94a3b8' }}>
                      {stat?.errorRatePercent ? `${stat.errorRatePercent.toFixed(1)}%` : '0.0%'}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {/* SAFE NON-FINANCIAL LOAD TEST CONSOLE */}
      <div style={{ background: '#1e293b', border: '1px solid #334155', borderRadius: '12px', padding: '20px', marginBottom: '24px' }}>
        <h2 style={{ fontSize: '18px', fontWeight: 700, margin: '0 0 8px 0', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Zap size={20} color="#fbbf24" /> Safe Non-Financial Load Test Generator
        </h2>
        <p style={{ color: '#94a3b8', fontSize: '13px', margin: '0 0 16px 0' }}>
          Simulates safe, read-only market data and query operations across nodes to test actual traffic distribution without risking funds or trade duplication.
        </p>

        <div style={{ display: 'flex', alignItems: 'center', gap: '16px', flexWrap: 'wrap' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <label style={{ fontSize: '13px', color: '#94a3b8' }}>Number of Requests:</label>
            <input 
              type="number" 
              value={testCount} 
              onChange={(e) => setTestCount(Math.max(1, parseInt(e.target.value) || 1))}
              min={1}
              max={200}
              style={{
                background: '#0f172a',
                border: '1px solid #334155',
                color: '#fff',
                padding: '6px 12px',
                borderRadius: '6px',
                width: '80px',
                fontSize: '13px'
              }}
            />
          </div>

          <button
            onClick={handleRunLoadTest}
            disabled={loadTesting}
            style={{
              background: '#2563eb',
              color: '#fff',
              border: 'none',
              padding: '8px 18px',
              borderRadius: '6px',
              fontWeight: 600,
              fontSize: '13px',
              cursor: loadTesting ? 'not-allowed' : 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '6px'
            }}
          >
            <Play size={14} /> {loadTesting ? 'Dispatching Probes...' : 'Execute Load Test'}
          </button>
        </div>

        {testResult && (
          <div style={{ marginTop: '16px', padding: '14px', background: '#0f172a', borderRadius: '8px', border: '1px solid #334155' }}>
            <div style={{ fontSize: '13px', fontWeight: 700, color: '#38bdf8', marginBottom: '8px' }}>
              Load Test Run Complete: {testResult.totalRequests} requests dispatched in {testResult.elapsedMs} ms ({testResult.algorithm})
            </div>
            <div style={{ display: 'flex', gap: '20px', fontSize: '13px' }}>
              {Object.entries(testResult.distribution || {}).map(([node, cnt]: any) => (
                <div key={node}>
                  <strong style={{ color: '#fff' }}>{node.toUpperCase()}:</strong> {cnt} reqs ({((cnt / testResult.totalRequests) * 100).toFixed(1)}%)
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* RECENT ROUTING DECISIONS AUDIT LOG */}
      <div style={{ background: '#1e293b', border: '1px solid #334155', borderRadius: '12px', padding: '20px' }}>
        <h2 style={{ fontSize: '18px', fontWeight: 700, margin: '0 0 14px 0', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Activity size={20} color="#a78bfa" /> Live Routing Decision Audit Log
        </h2>

        <div style={{
          background: '#090d14',
          padding: '16px',
          borderRadius: '8px',
          fontFamily: 'JetBrains Mono, monospace',
          fontSize: '12px',
          maxHeight: '260px',
          overflowY: 'auto',
          border: '1px solid #1e293b'
        }}>
          {report?.recentRoutingDecisions && report.recentRoutingDecisions.length > 0 ? (
            report.recentRoutingDecisions.map((item, idx) => (
              <div key={idx} style={{ marginBottom: '6px', color: item.category === 'LEADER_ONLY_WRITE' ? '#34d399' : '#38bdf8' }}>
                <span style={{ color: '#64748b' }}>[{new Date(item.timestamp).toLocaleTimeString()}]</span>{' '}
                <strong>[{item.category}]</strong> &rarr; Target: <strong>{item.targetNode.toUpperCase()}</strong> | Strategy: {item.strategy} ({item.reason})
              </div>
            ))
          ) : (
            <div style={{ color: '#64748b' }}>No routing decisions logged yet. Dispatch requests or run load test.</div>
          )}
        </div>
      </div>
    </div>
  );
};

export default LoadBalancingDashboard;
