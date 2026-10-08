import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import api from '../api/axios';
import { Activity, Server, UserCheck } from 'lucide-react';

interface SystemStatus {
  gateway?: string;
  node1?: string;
  node2?: string;
  node3?: string;
}

const Dashboard: React.FC = () => {
  const { user } = useAuth();
  const [status, setStatus] = useState<SystemStatus>({});
  const [loading, setLoading] = useState<boolean>(true);

  const fetchStatus = async () => {
    setLoading(true);
    try {
      const response = await api.get('/status');
      if (response.status === 200) {
        setStatus(response.data);
      } else {
        setStatus({ gateway: 'OFFLINE', node1: 'OFFLINE', node2: 'OFFLINE', node3: 'OFFLINE' });
      }
    } catch (err) {
      console.error(err);
      setStatus({ gateway: 'OFFLINE', node1: 'OFFLINE', node2: 'OFFLINE', node3: 'OFFLINE' });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchStatus();
    const interval = setInterval(fetchStatus, 5000);
    return () => clearInterval(interval);
  }, []);

  return (
    <div className="dashboard-content">
      <div className="dashboard-header">
        <h1>Dashboard</h1>
        <p>Welcome back, {user?.username}!</p>
      </div>

      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-icon"><UserCheck size={24} /></div>
          <div className="stat-info">
            <h4>Account Role</h4>
            <p>{user?.roles.join(', ')}</p>
          </div>
        </div>
        
        <div className="stat-card">
          <div className="stat-icon"><Activity size={24} /></div>
          <div className="stat-info">
            <h4>Market Status</h4>
            <p className="status-online">OPEN</p>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon"><Server size={24} /></div>
          <div className="stat-info">
            <h4>Connected Nodes</h4>
            <p>{Object.values(status).filter(s => s === 'ONLINE').length} / 4</p>
          </div>
        </div>
      </div>

      <div className="system-status-section">
        <h3>System Status</h3>
        <div className="status-list">
          <div className="status-item">
            <span>Gateway</span>
            <span className={status.gateway === 'ONLINE' ? 'status-online' : 'status-offline'}>
              {status.gateway || 'LOADING...'}
            </span>
          </div>
          <div className="status-item">
            <span>Node 1</span>
            <span className={status.node1 === 'ONLINE' ? 'status-online' : 'status-offline'}>
              {status.node1 || 'LOADING...'}
            </span>
          </div>
          <div className="status-item">
            <span>Node 2</span>
            <span className={status.node2 === 'ONLINE' ? 'status-online' : 'status-offline'}>
              {status.node2 || 'LOADING...'}
            </span>
          </div>
          <div className="status-item">
            <span>Node 3</span>
            <span className={status.node3 === 'ONLINE' ? 'status-online' : 'status-offline'}>
              {status.node3 || 'LOADING...'}
            </span>
          </div>
          <div className="status-item">
            <span>Database</span>
            <span className={(status.node1 === 'ONLINE' || status.node2 === 'ONLINE') ? 'status-online' : 'status-offline'}>
              {(status.node1 === 'ONLINE' || status.node2 === 'ONLINE') ? 'ONLINE' : 'UNKNOWN'}
            </span>
          </div>
        </div>
        <button onClick={fetchStatus} disabled={loading} className="refresh-btn">
          {loading ? 'Refreshing...' : 'Refresh Status'}
        </button>
      </div>
    </div>
  );
};

export default Dashboard;
