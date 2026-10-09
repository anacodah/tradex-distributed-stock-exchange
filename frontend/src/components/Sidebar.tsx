import React, { useState, useEffect } from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import NotificationCenter from './NotificationCenter';
import api from '../api/axios';
import { 
  LayoutDashboard, 
  TrendingUp, 
  Briefcase, 
  ShoppingCart, 
  Wallet, 
  Server, 
  Clock, 
  ShieldCheck, 
  Database, 
  GitMerge, 
  Settings,
  LogOut,
  Bell,
  Activity
} from 'lucide-react';

const Sidebar: React.FC = () => {
  const { user, logout } = useAuth();
  const [showNotifications, setShowNotifications] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);

  useEffect(() => {
    const fetchUnread = async () => {
      try {
        const res = await api.get('/notifications/unread-count');
        setUnreadCount(res.data.unreadCount || 0);
      } catch (err) {
        // ignore if not logged in
      }
    };
    fetchUnread();
    const interval = setInterval(fetchUnread, 15000);
    return () => clearInterval(interval);
  }, []);

  return (
    <aside className="sidebar">
      <div className="sidebar-header">
        <div className="brand">
          <div className="brand-logo">TX</div>
          <div className="brand-text">
            <h2>TradeX</h2>
            <span className="brand-badge">Distributed Ex.</span>
          </div>
        </div>
        <div className="user-profile">
          <div className="avatar">{user?.username ? user.username.charAt(0).toUpperCase() : 'U'}</div>
          <div className="user-details">
            <span className="username">{user?.username}</span>
            <span className="role-tag">{user?.roles?.[0] || 'USER'}</span>
          </div>
          <button 
            onClick={() => setShowNotifications(!showNotifications)} 
            className="icon-notif-btn" 
            title="Notifications"
            style={{ position: 'relative', background: 'none', border: 'none', color: '#94a3b8', cursor: 'pointer', padding: '6px' }}
          >
            <Bell size={16} />
            {unreadCount > 0 && (
              <span style={{
                position: 'absolute',
                top: 0,
                right: 0,
                background: '#ef4444',
                color: 'white',
                borderRadius: '9999px',
                fontSize: '10px',
                padding: '1px 5px',
                fontWeight: 'bold'
              }}>
                {unreadCount}
              </span>
            )}
          </button>
          <button onClick={logout} className="icon-logout-btn" title="Logout">
            <LogOut size={16} />
          </button>
        </div>
      </div>

      {showNotifications && (
        <div style={{
          position: 'fixed',
          top: '20px',
          left: '260px',
          zIndex: 1000,
          width: '380px',
          boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.5), 0 8px 10px -6px rgba(0, 0, 0, 0.5)'
        }}>
          <NotificationCenter onClose={() => setShowNotifications(false)} />
        </div>
      )}

      <nav className="sidebar-nav">
        <div className="nav-section">
          <div className="section-title">MARKETPLACE</div>
          <NavLink to="/dashboard" className={({isActive}) => isActive ? "nav-link active" : "nav-link"}>
            <LayoutDashboard size={18} /> Dashboard
          </NavLink>
          <NavLink to="/market" className={({isActive}) => isActive ? "nav-link active" : "nav-link"}>
            <TrendingUp size={18} /> Market Watch
          </NavLink>
          <NavLink to="/portfolio" className={({isActive}) => isActive ? "nav-link active" : "nav-link"}>
            <Briefcase size={18} /> Portfolio
          </NavLink>
          <NavLink to="/orders" className={({isActive}) => isActive ? "nav-link active" : "nav-link"}>
            <ShoppingCart size={18} /> Orders & Trades
          </NavLink>
          <NavLink to="/wallet" className={({isActive}) => isActive ? "nav-link active" : "nav-link"}>
            <Wallet size={18} /> Wallet
          </NavLink>
          <NavLink to="/transactions" className={({isActive}) => isActive ? "nav-link active" : "nav-link"}>
            <Clock size={18} /> Transactions & Ledger
          </NavLink>
          <NavLink to="/audit" className={({isActive}) => isActive ? "nav-link active" : "nav-link"}>
            <ShieldCheck size={18} /> Audit Trail
          </NavLink>
        </div>

        <div className="nav-section">
          <div className="section-title">DISTRIBUTED LAB</div>
          <NavLink to="/nodes" className={({isActive}) => isActive ? "nav-link active" : "nav-link"}>
            <Server size={18} /> Distributed Nodes
          </NavLink>
          <NavLink to="/clocks" className={({isActive}) => isActive ? "nav-link active" : "nav-link"}>
            <Clock size={18} /> Clocks (Exp 3)
          </NavLink>
          <NavLink to="/elections" className={({isActive}) => isActive ? "nav-link active" : "nav-link"}>
            <ShieldCheck size={18} /> Elections (Exp 4)
          </NavLink>
          <NavLink to="/replication" className={({isActive}) => isActive ? "nav-link active" : "nav-link"}>
            <Database size={18} /> Replication (Phase 9)
          </NavLink>
          <NavLink to="/failover" className={({isActive}) => isActive ? "nav-link active" : "nav-link"}>
            <Activity size={18} /> Failover & Faults (P10)
          </NavLink>
          <NavLink to="/load-balancer" className={({isActive}) => isActive ? "nav-link active" : "nav-link"}>
            <GitMerge size={18} /> Load Balancing (P12)
          </NavLink>
        </div>
      </nav>
    </aside>
  );
};

export default Sidebar;
