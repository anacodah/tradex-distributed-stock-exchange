import React, { useState, useEffect } from 'react';
import api from '../api/axios';
import { 
  ShieldAlert, 
  Users, 
  Server, 
  ShoppingCart, 
  Clock, 
  RefreshCw, 
  AlertTriangle, 
  CheckCircle, 
  Flame, 
  Lock, 
  Unlock 
} from 'lucide-react';

interface AdminOverview {
  userCount: number;
  orderCount: number;
  tradeCount: number;
  activeLeader: string;
  leaderEpoch: number;
  tradingState: string;
  timestamp: number;
}

const AdminConsole: React.FC = () => {
  const [overview, setOverview] = useState<AdminOverview | null>(null);
  const [users, setUsers] = useState<any[]>([]);
  const [orders, setOrders] = useState<any[]>([]);
  const [activeTab, setActiveTab] = useState<'OVERVIEW' | 'USERS' | 'ORDERS'>('OVERVIEW');
  const [loading, setLoading] = useState<boolean>(true);
  const [breakerLoading, setBreakerLoading] = useState<boolean>(false);

  const fetchAdminData = async () => {
    try {
      const [overviewRes, usersRes, ordersRes] = await Promise.all([
        api.get('/admin/overview'),
        api.get('/admin/users'),
        api.get('/admin/orders')
      ]);
      setOverview(overviewRes.data);
      setUsers(usersRes.data);
      setOrders(ordersRes.data);
    } catch (err: any) {
      console.error('Failed fetching admin data:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAdminData();
    const interval = setInterval(fetchAdminData, 4000);
    return () => clearInterval(interval);
  }, []);

  const handleCircuitBreaker = async (targetState: 'AVAILABLE' | 'SUSPENDED') => {
    const confirmed = window.confirm(
      `CRITICAL ACTION: Are you sure you want to change trading state to ${targetState}? This action will be recorded in the immutable audit log.`
    );
    if (!confirmed) return;

    setBreakerLoading(true);
    try {
      await api.post('/admin/trading-circuit-breaker', { state: targetState });
      fetchAdminData();
    } catch (err: any) {
      alert(`Circuit breaker action failed: ${err.message}`);
    } finally {
      setBreakerLoading(false);
    }
  };

  if (loading && !overview) {
    return <div style={{ padding: '24px', color: '#94a3b8' }}>Loading Admin Console...</div>;
  }

  return (
    <div className="admin-console" style={{ padding: '24px', color: '#f8fafc' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '24px' }}>
        <div>
          <h1 style={{ fontSize: '28px', fontWeight: 800, margin: 0, display: 'flex', alignItems: 'center', gap: '10px' }}>
            <ShieldAlert size={32} color="#ef4444" />
            Administrative Console & Governance
          </h1>
          <p style={{ color: '#94a3b8', margin: '6px 0 0 0', fontSize: '14px' }}>
            Role-protected operations, user registry status, circuit breaker safety locks, and system audits.
          </p>
        </div>
        <button
          onClick={fetchAdminData}
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
          <RefreshCw size={14} /> Refresh Governance State
        </button>
      </div>

      {/* EMERGENCY CIRCUIT BREAKER BANNER */}
      <div style={{
        background: overview?.tradingState === 'AVAILABLE' ? '#0f172a' : '#450a0a',
        border: `2px solid ${overview?.tradingState === 'AVAILABLE' ? '#334155' : '#ef4444'}`,
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
            Emergency Trading Circuit Breaker
          </span>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginTop: '4px' }}>
            <h2 style={{ fontSize: '24px', fontWeight: 800, color: '#f8fafc', margin: 0 }}>
              System Trading Status:
            </h2>
            <span style={{
              background: overview?.tradingState === 'AVAILABLE' ? '#059669' : '#dc2626',
              color: '#fff',
              padding: '4px 12px',
              borderRadius: '8px',
              fontSize: '14px',
              fontWeight: 800
            }}>
              {overview?.tradingState || 'AVAILABLE'}
            </span>
          </div>
        </div>

        <div>
          {overview?.tradingState === 'AVAILABLE' ? (
            <button
              onClick={() => handleCircuitBreaker('SUSPENDED')}
              disabled={breakerLoading}
              style={{
                background: '#dc2626',
                color: '#fff',
                border: 'none',
                padding: '10px 20px',
                borderRadius: '8px',
                fontWeight: 700,
                fontSize: '13px',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '8px'
              }}
            >
              <Lock size={16} /> Trip Circuit Breaker (Halt Trading)
            </button>
          ) : (
            <button
              onClick={() => handleCircuitBreaker('AVAILABLE')}
              disabled={breakerLoading}
              style={{
                background: '#059669',
                color: '#fff',
                border: 'none',
                padding: '10px 20px',
                borderRadius: '8px',
                fontWeight: 700,
                fontSize: '13px',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '8px'
              }}
            >
              <Unlock size={16} /> Clear Lock & Resume Trading
            </button>
          )}
        </div>
      </div>

      {/* NAVIGATION TABS */}
      <div style={{ display: 'flex', gap: '12px', borderBottom: '1px solid #334155', paddingBottom: '12px', marginBottom: '24px' }}>
        <button
          onClick={() => setActiveTab('OVERVIEW')}
          style={{
            background: activeTab === 'OVERVIEW' ? '#3b82f6' : 'transparent',
            color: activeTab === 'OVERVIEW' ? '#fff' : '#94a3b8',
            border: 'none',
            padding: '8px 16px',
            borderRadius: '6px',
            fontWeight: 600,
            fontSize: '14px',
            cursor: 'pointer'
          }}
        >
          System Overview
        </button>
        <button
          onClick={() => setActiveTab('USERS')}
          style={{
            background: activeTab === 'USERS' ? '#3b82f6' : 'transparent',
            color: activeTab === 'USERS' ? '#fff' : '#94a3b8',
            border: 'none',
            padding: '8px 16px',
            borderRadius: '6px',
            fontWeight: 600,
            fontSize: '14px',
            cursor: 'pointer'
          }}
        >
          User Accounts ({users.length})
        </button>
        <button
          onClick={() => setActiveTab('ORDERS')}
          style={{
            background: activeTab === 'ORDERS' ? '#3b82f6' : 'transparent',
            color: activeTab === 'ORDERS' ? '#fff' : '#94a3b8',
            border: 'none',
            padding: '8px 16px',
            borderRadius: '6px',
            fontWeight: 600,
            fontSize: '14px',
            cursor: 'pointer'
          }}
        >
          Recent Orders ({orders.length})
        </button>
      </div>

      {/* TAB CONTENT: OVERVIEW */}
      {activeTab === 'OVERVIEW' && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '16px' }}>
          <div style={{ background: '#1e293b', border: '1px solid #334155', borderRadius: '12px', padding: '18px' }}>
            <div style={{ fontSize: '12px', color: '#94a3b8', textTransform: 'uppercase' }}>Registered Users</div>
            <div style={{ fontSize: '26px', fontWeight: 800, color: '#f8fafc', marginTop: '6px' }}>{overview?.userCount || 0}</div>
          </div>
          <div style={{ background: '#1e293b', border: '1px solid #334155', borderRadius: '12px', padding: '18px' }}>
            <div style={{ fontSize: '12px', color: '#94a3b8', textTransform: 'uppercase' }}>Total Orders Ingested</div>
            <div style={{ fontSize: '26px', fontWeight: 800, color: '#38bdf8', marginTop: '6px' }}>{overview?.orderCount || 0}</div>
          </div>
          <div style={{ background: '#1e293b', border: '1px solid #334155', borderRadius: '12px', padding: '18px' }}>
            <div style={{ fontSize: '12px', color: '#94a3b8', textTransform: 'uppercase' }}>Total Executed Trades</div>
            <div style={{ fontSize: '26px', fontWeight: 800, color: '#34d399', marginTop: '6px' }}>{overview?.tradeCount || 0}</div>
          </div>
          <div style={{ background: '#1e293b', border: '1px solid #334155', borderRadius: '12px', padding: '18px' }}>
            <div style={{ fontSize: '12px', color: '#94a3b8', textTransform: 'uppercase' }}>Active Cluster Leader</div>
            <div style={{ fontSize: '26px', fontWeight: 800, color: '#a78bfa', marginTop: '6px' }}>
              {overview?.activeLeader?.toUpperCase()} (Epoch #{overview?.leaderEpoch})
            </div>
          </div>
        </div>
      )}

      {/* TAB CONTENT: USERS */}
      {activeTab === 'USERS' && (
        <div style={{ background: '#1e293b', border: '1px solid #334155', borderRadius: '12px', padding: '20px' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '14px' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid #334155', color: '#94a3b8' }}>
                <th style={{ padding: '12px' }}>ID</th>
                <th style={{ padding: '12px' }}>USERNAME</th>
                <th style={{ padding: '12px' }}>EMAIL</th>
                <th style={{ padding: '12px' }}>FULL NAME</th>
                <th style={{ padding: '12px' }}>ROLES</th>
                <th style={{ padding: '12px' }}>REGISTERED</th>
              </tr>
            </thead>
            <tbody>
              {users.map(u => (
                <tr key={u.id} style={{ borderBottom: '1px solid #0f172a' }}>
                  <td style={{ padding: '12px' }}>#{u.id}</td>
                  <td style={{ padding: '12px', fontWeight: 700 }}>{u.username}</td>
                  <td style={{ padding: '12px', color: '#94a3b8' }}>{u.email}</td>
                  <td style={{ padding: '12px' }}>{u.fullName || '—'}</td>
                  <td style={{ padding: '12px' }}>
                    <span style={{ background: '#0f172a', padding: '3px 8px', borderRadius: '6px', fontSize: '11px', color: '#38bdf8' }}>
                      {u.roles?.join(', ')}
                    </span>
                  </td>
                  <td style={{ padding: '12px', color: '#64748b' }}>{new Date(u.createdAt).toLocaleDateString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* TAB CONTENT: ORDERS */}
      {activeTab === 'ORDERS' && (
        <div style={{ background: '#1e293b', border: '1px solid #334155', borderRadius: '12px', padding: '20px' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '14px' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid #334155', color: '#94a3b8' }}>
                <th style={{ padding: '12px' }}>ORDER #</th>
                <th style={{ padding: '12px' }}>SYMBOL</th>
                <th style={{ padding: '12px' }}>SIDE</th>
                <th style={{ padding: '12px' }}>TYPE</th>
                <th style={{ padding: '12px' }}>QTY</th>
                <th style={{ padding: '12px' }}>STATUS</th>
                <th style={{ padding: '12px' }}>DATE</th>
              </tr>
            </thead>
            <tbody>
              {orders.slice(0, 50).map(o => (
                <tr key={o.id} style={{ borderBottom: '1px solid #0f172a' }}>
                  <td style={{ padding: '12px' }}>#{o.id}</td>
                  <td style={{ padding: '12px', fontWeight: 700 }}>{o.stock?.symbol || o.symbol}</td>
                  <td style={{ padding: '12px', color: o.side === 'BUY' ? '#10b981' : '#ef4444', fontWeight: 700 }}>{o.side}</td>
                  <td style={{ padding: '12px' }}>{o.orderType}</td>
                  <td style={{ padding: '12px' }}>{o.quantity}</td>
                  <td style={{ padding: '12px' }}>
                    <span style={{ 
                      background: o.status === 'FILLED' ? '#064e3b' : (o.status === 'REJECTED' ? '#7f1d1d' : '#1e3a8a'),
                      color: '#fff',
                      padding: '3px 8px',
                      borderRadius: '6px',
                      fontSize: '11px',
                      fontWeight: 700
                    }}>
                      {o.status}
                    </span>
                  </td>
                  <td style={{ padding: '12px', color: '#64748b' }}>{new Date(o.createdAt).toLocaleTimeString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default AdminConsole;
