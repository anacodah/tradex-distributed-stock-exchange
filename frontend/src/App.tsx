import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import Sidebar from './components/Sidebar';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import Market from './pages/Market';
import Wallet from './pages/Wallet';
import Portfolio from './pages/Portfolio';
import Orders from './pages/Orders';
import Clocks from './pages/Clocks';
import Elections from './pages/Elections';
import TransactionHistory from './pages/TransactionHistory';
import AuditTrail from './pages/AuditTrail';
import ClusterNodes from './pages/ClusterNodes';
import ReplicationDashboard from './pages/ReplicationDashboard';
import './App.css';

const AppLayout: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  return (
    <div className="app-layout">
      <Sidebar />
      <main className="main-content">
        {children}
      </main>
    </div>
  );
};

function App() {
  return (
    <AuthProvider>
      <Router>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          
          <Route element={<ProtectedRoute />}>
            <Route path="/dashboard" element={<AppLayout><Dashboard /></AppLayout>} />
            <Route path="/market" element={<AppLayout><Market /></AppLayout>} />
            <Route path="/wallet" element={<AppLayout><Wallet /></AppLayout>} />
            <Route path="/portfolio" element={<AppLayout><Portfolio /></AppLayout>} />
            <Route path="/orders" element={<AppLayout><Orders /></AppLayout>} />
            <Route path="/transactions" element={<AppLayout><TransactionHistory /></AppLayout>} />
            <Route path="/audit" element={<AppLayout><AuditTrail /></AppLayout>} />
            <Route path="/clocks" element={<AppLayout><Clocks /></AppLayout>} />
            <Route path="/elections" element={<AppLayout><Elections /></AppLayout>} />
            <Route path="/nodes" element={<AppLayout><ClusterNodes /></AppLayout>} />
            <Route path="/replication" element={<AppLayout><ReplicationDashboard /></AppLayout>} />
            
            {/* Redirect root to dashboard */}
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
          </Route>
          
          {/* Catch-all */}
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </Router>
    </AuthProvider>
  );
}

export default App;
