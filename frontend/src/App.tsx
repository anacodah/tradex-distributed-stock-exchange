import { useState, useEffect } from 'react';
import './App.css';

interface SystemStatus {
  gateway?: string;
  node1?: string;
  node2?: string;
  node3?: string;
}

function App() {
  const [status, setStatus] = useState<SystemStatus>({});
  const [loading, setLoading] = useState<boolean>(true);

  const fetchStatus = async () => {
    setLoading(true);
    try {
      // Proxy or direct call depending on setup. Assuming frontend is served via a gateway or directly calls it.
      // With Docker, the frontend runs on port 3000, Gateway on 8080.
      const response = await fetch('http://localhost:8080/api/status');
      if (response.ok) {
        const data = await response.json();
        setStatus(data);
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
    <div className="App">
      <header className="App-header">
        <h1>TRADEX</h1>
        <h2>Distributed Stock Exchange</h2>
      </header>
      <main>
        <section className="status-board">
          <h3>System Status</h3>
          <div className="status-item">
            <span>Frontend:</span> 
            <span className="status-online">ONLINE</span>
          </div>
          <div className="status-item">
            <span>Gateway:</span> 
            <span className={status.gateway === 'ONLINE' ? 'status-online' : 'status-offline'}>{status.gateway || 'LOADING...'}</span>
          </div>
          <div className="status-item">
            <span>Node 1:</span> 
            <span className={status.node1 === 'ONLINE' ? 'status-online' : 'status-offline'}>{status.node1 || 'LOADING...'}</span>
          </div>
          <div className="status-item">
            <span>Node 2:</span> 
            <span className={status.node2 === 'ONLINE' ? 'status-online' : 'status-offline'}>{status.node2 || 'LOADING...'}</span>
          </div>
          <div className="status-item">
            <span>Node 3:</span> 
            <span className={status.node3 === 'ONLINE' ? 'status-online' : 'status-offline'}>{status.node3 || 'LOADING...'}</span>
          </div>
          {/* Note: we assume Database is running if node status are ONLINE since nodes depend on it */}
          <div className="status-item">
            <span>Database:</span> 
            <span className={(status.node1 === 'ONLINE' || status.node2 === 'ONLINE') ? 'status-online' : 'status-offline'}>{(status.node1 === 'ONLINE' || status.node2 === 'ONLINE') ? 'ONLINE' : 'UNKNOWN'}</span>
          </div>
        </section>
        <button onClick={fetchStatus} disabled={loading}>
          {loading ? 'Refreshing...' : 'Refresh Status'}
        </button>
      </main>
    </div>
  );
}

export default App;
