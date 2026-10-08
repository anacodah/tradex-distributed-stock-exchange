import React, { useEffect, useState } from 'react';
import api from '../api/axios';
import { ShoppingCart, RefreshCw, XCircle } from 'lucide-react';

interface Order {
  id: number;
  symbol: string;
  type: 'BUY' | 'SELL';
  quantity: number;
  price: number;
  status: 'PENDING' | 'FILLED' | 'CANCELLED';
  orderKind: 'MARKET' | 'LIMIT';
  createdAt: string;
}

interface Trade {
  id: number;
  orderId: number;
  symbol: string;
  quantity: number;
  executedPrice: number;
  timestamp: string;
}

const OrdersPage: React.FC = () => {
  const [orders, setOrders] = useState<Order[]>([]);
  const [trades, setTrades] = useState<Trade[]>([]);
  const [activeTab, setActiveTab] = useState<'orders' | 'trades'>('orders');
  const [loading, setLoading] = useState(true);

  const fetchData = async () => {
    try {
      const [ordersRes, tradesRes] = await Promise.all([
        api.get('/orders'),
        api.get('/trades')
      ]);
      setOrders(ordersRes.data);
      setTrades(tradesRes.data);
    } catch (err) {
      console.error('Failed to fetch orders/trades', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
    const interval = setInterval(fetchData, 5000);
    return () => clearInterval(interval);
  }, []);

  const handleCancelOrder = async (orderId: number) => {
    try {
      await api.post(`/orders/${orderId}/cancel`);
      fetchData();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to cancel order');
    }
  };

  if (loading) return <div className="loading-spinner">Loading Orders...</div>;

  return (
    <div className="orders-container">
      <div className="dashboard-header">
        <h1>Orders & Executed Trades</h1>
        <p>Monitor your active order book and completed trade executions</p>
      </div>

      <div style={{ display: 'flex', gap: '8px', marginBottom: '20px' }}>
        <button 
          type="button"
          className={`tab-btn ${activeTab === 'orders' ? 'active buy' : ''}`}
          style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px', maxWidth: '220px' }}
          onClick={() => setActiveTab('orders')}
        >
          <ShoppingCart size={16} /> My Orders ({orders.length})
        </button>
        <button 
          type="button"
          className={`tab-btn ${activeTab === 'trades' ? 'active buy' : ''}`}
          style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px', maxWidth: '220px' }}
          onClick={() => setActiveTab('trades')}
        >
          <RefreshCw size={16} /> Executed Trades ({trades.length})
        </button>
      </div>

      {activeTab === 'orders' ? (
        <div className="card full-width">
          <h3>Order History & Status</h3>
          {orders.length === 0 ? (
            <p style={{ color: 'var(--text-muted)', fontSize: '14px', padding: '16px 0' }}>
              No active or historical orders found.
            </p>
          ) : (
            <table className="data-table">
              <thead>
                <tr>
                  <th>Order ID</th>
                  <th>Symbol</th>
                  <th>Side</th>
                  <th>Type</th>
                  <th>Quantity</th>
                  <th>Price</th>
                  <th>Status</th>
                  <th>Time</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {orders.map((o: any) => (
                  <tr key={o.id}>
                    <td>#{o.id}</td>
                    <td><strong>{o.stock?.symbol || o.symbol}</strong></td>
                    <td>
                      <span className={`badge ${(o.side || o.type) === 'BUY' ? 'badge-buy' : 'badge-sell'}`}>
                        {o.side || o.type}
                      </span>
                    </td>
                    <td>{o.orderType || o.orderKind || 'MARKET'}</td>
                    <td>{o.quantity}</td>
                    <td>${Number(o.price || 0).toFixed(2)}</td>
                    <td>
                      <span className={`badge badge-status-${(o.status || 'PENDING').toLowerCase()}`}>
                        {o.status}
                      </span>
                    </td>
                    <td>{new Date(o.createdAt || Date.now()).toLocaleString()}</td>
                    <td>
                      {o.status === 'PENDING' && (
                        <button 
                          style={{ background: 'transparent', border: 'none', color: 'var(--red)', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '4px' }}
                          onClick={() => handleCancelOrder(o.id)}
                        >
                          <XCircle size={14} /> Cancel
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      ) : (
        <div className="card full-width">
          <h3>Executed Trades</h3>
          {trades.length === 0 ? (
            <p style={{ color: 'var(--text-muted)', fontSize: '14px', padding: '16px 0' }}>
              No trade executions recorded yet.
            </p>
          ) : (
            <table className="data-table">
              <thead>
                <tr>
                  <th>Trade ID</th>
                  <th>Order ID</th>
                  <th>Symbol</th>
                  <th>Quantity</th>
                  <th>Executed Price</th>
                  <th>Total Cost</th>
                  <th>Executed At</th>
                </tr>
              </thead>
              <tbody>
                {trades.map((t: any) => (
                  <tr key={t.id}>
                    <td>#{t.id}</td>
                    <td>#{t.order?.id || t.orderId}</td>
                    <td><strong>{t.stock?.symbol || t.symbol}</strong></td>
                    <td>{t.quantity}</td>
                    <td>${Number(t.price || t.executedPrice || 0).toFixed(2)}</td>
                    <td>${(Number(t.quantity) * Number(t.price || t.executedPrice || 0)).toFixed(2)}</td>
                    <td>{new Date(t.timestamp || Date.now()).toLocaleString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}
    </div>
  );
};

export default OrdersPage;
