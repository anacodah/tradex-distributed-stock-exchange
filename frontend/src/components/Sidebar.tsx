import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
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
  LogOut
} from 'lucide-react';

const Sidebar: React.FC = () => {
  const { user, logout } = useAuth();

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
          <button onClick={logout} className="icon-logout-btn" title="Logout">
            <LogOut size={16} />
          </button>
        </div>
      </div>

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
          <div className="nav-link disabled" title="Coming in Phase 5">
            <Database size={18} /> Replication (Phase 5)
          </div>
          <div className="nav-link disabled" title="Coming in Phase 5">
            <GitMerge size={18} /> Load Balancing (Phase 5)
          </div>
          <div className="nav-link disabled" title="Coming in Phase 5">
            <Settings size={18} /> Fault Tolerance (Phase 5)
          </div>
        </div>
      </nav>
    </aside>
  );
};

export default Sidebar;
