import React, { useState, useEffect } from 'react';
import { 
  Network, 
  Server, 
  Clock, 
  ShieldCheck, 
  Database, 
  Activity, 
  GitMerge, 
  Cpu, 
  Flame,
  Radio,
  RefreshCw
} from 'lucide-react';

// Sub-components / views
import ClusterNodes from './ClusterNodes';
import Clocks from './Clocks';
import ElectionsPage from './Elections';
import ReplicationDashboard from './ReplicationDashboard';
import FailoverRecovery from './FailoverRecovery';
import LoadBalancingDashboard from './LoadBalancingDashboard';

const DistributedControlCenter: React.FC = () => {
  const [activeTab, setActiveTab] = useState<
    'TOPOLOGY' | 'CLOCKS' | 'ELECTIONS' | 'REPLICATION' | 'FAILOVER' | 'LOAD_BALANCER'
  >('TOPOLOGY');

  return (
    <div className="distributed-control-center" style={{ padding: '24px', color: '#f8fafc' }}>
      <div style={{ marginBottom: '24px' }}>
        <h1 style={{ fontSize: '28px', fontWeight: 800, margin: 0, display: 'flex', alignItems: 'center', gap: '10px' }}>
          <Network size={32} color="#38bdf8" />
          Unified Distributed Systems Control Center
        </h1>
        <p style={{ color: '#94a3b8', margin: '6px 0 0 0', fontSize: '14px' }}>
          Phase 13: Live telemetry, RMI RPC coordination, Lamport & Vector clocks, Primary-Backup replication, fault injection, and load balancing in one unified dashboard.
        </p>
      </div>

      {/* TOP-LEVEL NAVIGATION BAR */}
      <div style={{
        display: 'flex',
        gap: '8px',
        overflowX: 'auto',
        background: '#1e293b',
        padding: '6px',
        borderRadius: '10px',
        marginBottom: '24px',
        border: '1px solid #334155'
      }}>
        <button
          onClick={() => setActiveTab('TOPOLOGY')}
          style={{
            background: activeTab === 'TOPOLOGY' ? '#2563eb' : 'transparent',
            color: activeTab === 'TOPOLOGY' ? '#fff' : '#94a3b8',
            border: 'none',
            padding: '8px 16px',
            borderRadius: '6px',
            fontWeight: 600,
            fontSize: '13px',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '6px',
            whiteSpace: 'nowrap'
          }}
        >
          <Server size={14} /> Cluster & RMI
        </button>

        <button
          onClick={() => setActiveTab('CLOCKS')}
          style={{
            background: activeTab === 'CLOCKS' ? '#2563eb' : 'transparent',
            color: activeTab === 'CLOCKS' ? '#fff' : '#94a3b8',
            border: 'none',
            padding: '8px 16px',
            borderRadius: '6px',
            fontWeight: 600,
            fontSize: '13px',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '6px',
            whiteSpace: 'nowrap'
          }}
        >
          <Clock size={14} /> Clocks & Sync
        </button>

        <button
          onClick={() => setActiveTab('ELECTIONS')}
          style={{
            background: activeTab === 'ELECTIONS' ? '#2563eb' : 'transparent',
            color: activeTab === 'ELECTIONS' ? '#fff' : '#94a3b8',
            border: 'none',
            padding: '8px 16px',
            borderRadius: '6px',
            fontWeight: 600,
            fontSize: '13px',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '6px',
            whiteSpace: 'nowrap'
          }}
        >
          <ShieldCheck size={14} /> Bully & Ring
        </button>

        <button
          onClick={() => setActiveTab('REPLICATION')}
          style={{
            background: activeTab === 'REPLICATION' ? '#2563eb' : 'transparent',
            color: activeTab === 'REPLICATION' ? '#fff' : '#94a3b8',
            border: 'none',
            padding: '8px 16px',
            borderRadius: '6px',
            fontWeight: 600,
            fontSize: '13px',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '6px',
            whiteSpace: 'nowrap'
          }}
        >
          <Database size={14} /> Replication Lag
        </button>

        <button
          onClick={() => setActiveTab('FAILOVER')}
          style={{
            background: activeTab === 'FAILOVER' ? '#2563eb' : 'transparent',
            color: activeTab === 'FAILOVER' ? '#fff' : '#94a3b8',
            border: 'none',
            padding: '8px 16px',
            borderRadius: '6px',
            fontWeight: 600,
            fontSize: '13px',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '6px',
            whiteSpace: 'nowrap'
          }}
        >
          <Flame size={14} /> Failover & Faults
        </button>

        <button
          onClick={() => setActiveTab('LOAD_BALANCER')}
          style={{
            background: activeTab === 'LOAD_BALANCER' ? '#2563eb' : 'transparent',
            color: activeTab === 'LOAD_BALANCER' ? '#fff' : '#94a3b8',
            border: 'none',
            padding: '8px 16px',
            borderRadius: '6px',
            fontWeight: 600,
            fontSize: '13px',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '6px',
            whiteSpace: 'nowrap'
          }}
        >
          <GitMerge size={14} /> Load Balancing
        </button>
      </div>

      {/* RENDER ACTIVE TAB */}
      <div style={{ background: '#0f172a', borderRadius: '12px', border: '1px solid #1e293b', overflow: 'hidden' }}>
        {activeTab === 'TOPOLOGY' && <ClusterNodes />}
        {activeTab === 'CLOCKS' && <Clocks />}
        {activeTab === 'ELECTIONS' && <ElectionsPage />}
        {activeTab === 'REPLICATION' && <ReplicationDashboard />}
        {activeTab === 'FAILOVER' && <FailoverRecovery />}
        {activeTab === 'LOAD_BALANCER' && <LoadBalancingDashboard />}
      </div>
    </div>
  );
};

export default DistributedControlCenter;
