import React, { useEffect, useState } from 'react';
import api from '../api/axios';
import { ShoppingCart, RefreshCw, XCircle, Edit3, Info } from 'lucide-react';

interface Order {
  id: number;
  symbol: string;
  side: 'BUY' | 'SELL';
  orderType: 'MARKET' | 'LIMIT' | 'STOP_LOSS';
  quantity: number;
  filledQuantity: number;
  price?: number;
  stopPrice?: number;
  executionPrice?: number;
  status: 'NEW' | 'OPEN' | 'PARTIALLY_FILLED' | 'FILLED' | 'CANCEL_PENDING' | 'CANCELLED' | 'REJECTED' | 'EXPIRED' | 'EXECUTED';
  sequenceNumber?: number;
  idempotencyKey?: string;
  createdAt: string;
  stock?: { symbol: string; companyName: string };
}

interface Trade {
  id: number;
  tradeId: string;
  orderId: number;
  symbol: string;
  side: string;
  quantity: number;
  price?: number;
  executionPrice?: number;
  totalValue: number;
  executedAt: string;
  timestamp?: string;
}

interface OrderDetail {
  order: Order;
  trades: Trade[];
}

const OrdersPage: React.FC = () => {
  const [orders, setOrders] = useState<Order[]>([]);
  const [trades, setTrades] = useState<Trade[]>([]);
  const [activeTab, setActiveTab] = useState<'ALL' | 'OPEN' | 'PARTIALLY_FILLED' | 'COMPLETED' | 'CANCELLED' | 'REJECTED' | 'TRADES'>('ALL');
  const [loading, setLoading] = useState(true);

  // Selected Order for Detail Modal
  const [selectedOrderDetail, setSelectedOrderDetail] = useState<OrderDetail | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);

  // Modification Modal state
  const [modifyingOrder, setModifyingOrder] = useState<Order | null>(null);
  const [newPrice, setNewPrice] = useState<string>('');
  const [newQuantity, setNewQuantity] = useState<string>('');
  const [modifying, setModifying] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

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
    const interval = setInterval(fetchData, 4000);
    return () => clearInterval(interval);
  }, []);

  const handleCancelOrder = async (orderId: number) => {
    if (!window.confirm(`Are you sure you want to cancel order #${orderId}?`)) return;
    try {
      await api.post(`/orders/${orderId}/cancel`);
      fetchData();
    } catch (err: any) {
      alert(err.response?.data?.message || err.response?.data?.error || 'Failed to cancel order');
    }
  };

  const handleOpenModify = (order: Order) => {
    setModifyingOrder(order);
    setNewPrice(order.price ? order.price.toString() : '');
    setNewQuantity(order.quantity.toString());
    setActionError(null);
  };

  const handleSaveModify = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!modifyingOrder) return;
    setModifying(true);
    setActionError(null);

    try {
      await api.put(`/orders/${modifyingOrder.id}`, {
        price: newPrice ? Number(newPrice) : null,
        quantity: Number(newQuantity)
      });
      setModifyingOrder(null);
      fetchData();
    } catch (err: any) {
      setActionError(err.response?.data?.error || err.response?.data?.message || 'Failed to modify order');
    } finally {
      setModifying(false);
    }
  };

  const handleViewDetail = async (orderId: number) => {
    setDetailLoading(true);
    try {
      const res = await api.get(`/orders/${orderId}`);
      setSelectedOrderDetail(res.data);
    } catch (err) {
      alert('Failed to load order details');
    } finally {
      setDetailLoading(false);
    }
  };

  const filteredOrders = orders.filter((o) => {
    if (activeTab === 'ALL') return true;
    if (activeTab === 'OPEN') return o.status === 'OPEN' || o.status === 'NEW';
    if (activeTab === 'PARTIALLY_FILLED') return o.status === 'PARTIALLY_FILLED';
    if (activeTab === 'COMPLETED') return o.status === 'FILLED' || o.status === 'EXECUTED';
    if (activeTab === 'CANCELLED') return o.status === 'CANCELLED';
    if (activeTab === 'REJECTED') return o.status === 'REJECTED';
    return true;
  });

  if (loading) return <div className="loading-spinner">Loading Orders & Trades...</div>;

  return (
    <div className="orders-container">
      <div className="dashboard-header">
        <h1>Orders & Matching Engine Ledger</h1>
        <p>Monitor paper-trading lifecycle, price-time priority executions & trade confirmations</p>
      </div>

      {/* Navigation Tabs */}
      <div style={{ display: 'flex', gap: '8px', marginBottom: '20px', flexWrap: 'wrap' }}>
        {(['ALL', 'OPEN', 'PARTIALLY_FILLED', 'COMPLETED', 'CANCELLED', 'REJECTED', 'TRADES'] as const).map((tab) => (
          <button
            key={tab}
            type="button"
            className={`tab-btn ${activeTab === tab ? 'active buy' : ''}`}
            style={{ fontSize: '12px', padding: '6px 14px', borderRadius: '4px' }}
            onClick={() => setActiveTab(tab)}
          >
            {tab === 'TRADES' ? `Executed Trades (${trades.length})` : `${tab} (${orders.filter(o => {
              if (tab === 'ALL') return true;
              if (tab === 'OPEN') return o.status === 'OPEN' || o.status === 'NEW';
              if (tab === 'PARTIALLY_FILLED') return o.status === 'PARTIALLY_FILLED';
              if (tab === 'COMPLETED') return o.status === 'FILLED' || o.status === 'EXECUTED';
              if (tab === 'CANCELLED') return o.status === 'CANCELLED';
              if (tab === 'REJECTED') return o.status === 'REJECTED';
              return false;
            }).length})`}
          </button>
        ))}
      </div>

      {activeTab !== 'TRADES' ? (
        <div className="card full-width">
          <h3>Order Book Positions ({activeTab})</h3>
          {filteredOrders.length === 0 ? (
            <p style={{ color: 'var(--text-muted)', fontSize: '14px', padding: '16px 0' }}>
              No orders found matching the filter criteria.
            </p>
          ) : (
            <div style={{ overflowX: 'auto' }}>
              <table className="data-table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Symbol</th>
                    <th>Side</th>
                    <th>Type</th>
                    <th>Quantity</th>
                    <th>Filled</th>
                    <th>Price</th>
                    <th>Status</th>
                    <th>Seq #</th>
                    <th>Created</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredOrders.map((o) => {
                    const isEligible = o.status === 'OPEN' || o.status === 'PARTIALLY_FILLED' || o.status === 'NEW';
                    return (
                      <tr key={o.id}>
                        <td>#{o.id}</td>
                        <td><strong>{o.stock?.symbol || o.symbol}</strong></td>
                        <td>
                          <span className={`badge ${o.side === 'BUY' ? 'badge-buy' : 'badge-sell'}`}>
                            {o.side}
                          </span>
                        </td>
                        <td>{o.orderType}</td>
                        <td>{o.quantity}</td>
                        <td>
                          <span style={{ color: Number(o.filledQuantity || 0) > 0 ? '#10b981' : 'inherit' }}>
                            {o.filledQuantity || 0}
                          </span>
                        </td>
                        <td>
                          {o.orderType === 'MARKET' ? (
                            'MKT'
                          ) : o.orderType === 'STOP_LOSS' ? (
                            `Stop: $${Number(o.stopPrice || 0).toFixed(2)}`
                          ) : (
                            `$${Number(o.price || 0).toFixed(2)}`
                          )}
                        </td>
                        <td>
                          <span className={`badge badge-status-${(o.status || 'PENDING').toLowerCase()}`}>
                            {o.status}
                          </span>
                        </td>
                        <td><small style={{ color: 'var(--text-secondary)' }}>{o.sequenceNumber || '-'}</small></td>
                        <td><small>{new Date(o.createdAt).toLocaleTimeString()}</small></td>
                        <td>
                          <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                            <button
                              title="View Details & Executions"
                              style={{ background: 'transparent', border: 'none', color: '#6366f1', cursor: 'pointer' }}
                              onClick={() => handleViewDetail(o.id)}
                            >
                              <Info size={16} />
                            </button>
                            {isEligible && (
                              <>
                                <button
                                  title="Modify Price/Quantity"
                                  style={{ background: 'transparent', border: 'none', color: '#f59e0b', cursor: 'pointer' }}
                                  onClick={() => handleOpenModify(o)}
                                >
                                  <Edit3 size={15} />
                                </button>
                                <button
                                  title="Cancel Order"
                                  style={{ background: 'transparent', border: 'none', color: 'var(--red, #ef4444)', cursor: 'pointer' }}
                                  onClick={() => handleCancelOrder(o.id)}
                                >
                                  <XCircle size={15} />
                                </button>
                              </>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      ) : (
        <div className="card full-width">
          <h3>Executed Trades (Unique Auditable Records)</h3>
          {trades.length === 0 ? (
            <p style={{ color: 'var(--text-muted)', fontSize: '14px', padding: '16px 0' }}>
              No trade executions recorded yet.
            </p>
          ) : (
            <div style={{ overflowX: 'auto' }}>
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Trade Ref ID</th>
                    <th>Order #</th>
                    <th>Symbol</th>
                    <th>Side</th>
                    <th>Shares</th>
                    <th>Price</th>
                    <th>Total Value</th>
                    <th>Timestamp</th>
                  </tr>
                </thead>
                <tbody>
                  {trades.map((t) => (
                    <tr key={t.id || t.tradeId}>
                      <td><code style={{ color: '#818cf8', fontSize: '11px' }}>{t.tradeId || `TRD-${t.id}`}</code></td>
                      <td>#{t.orderId}</td>
                      <td><strong>{t.symbol}</strong></td>
                      <td>
                        <span className={`badge ${t.side === 'BUY' ? 'badge-buy' : 'badge-sell'}`}>
                          {t.side || 'FILL'}
                        </span>
                      </td>
                      <td>{t.quantity}</td>
                      <td>${Number(t.price || t.executionPrice || 0).toFixed(2)}</td>
                      <td>${Number(t.totalValue || 0).toFixed(2)}</td>
                      <td>{new Date(t.executedAt || t.timestamp || Date.now()).toLocaleString()}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* Order Details Modal */}
      {selectedOrderDetail && (
        <div className="modal-backdrop" style={{
          position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
          background: 'rgba(0,0,0,0.7)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000
        }}>
          <div className="card" style={{ maxWidth: '600px', width: '90%', maxHeight: '80vh', overflowY: 'auto' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h3>Order Audit Details #{selectedOrderDetail.order.id}</h3>
              <button
                style={{ background: 'transparent', border: 'none', color: '#fff', fontSize: '18px', cursor: 'pointer' }}
                onClick={() => setSelectedOrderDetail(null)}
              >
                ✕
              </button>
            </div>
            <div style={{ fontSize: '13px', lineHeight: 1.6 }}>
              <p><strong>Instrument:</strong> {selectedOrderDetail.order.stock?.symbol} ({selectedOrderDetail.order.stock?.companyName})</p>
              <p><strong>Side / Type:</strong> {selectedOrderDetail.order.side} • {selectedOrderDetail.order.orderType}</p>
              <p><strong>Quantity:</strong> {selectedOrderDetail.order.quantity} | <strong>Filled:</strong> {selectedOrderDetail.order.filledQuantity}</p>
              <p><strong>Limit Price:</strong> ${Number(selectedOrderDetail.order.price || 0).toFixed(2)}</p>
              <p><strong>Status:</strong> <span className={`badge badge-status-${selectedOrderDetail.order.status.toLowerCase()}`}>{selectedOrderDetail.order.status}</span></p>
              <p><strong>Priority Sequence:</strong> {selectedOrderDetail.order.sequenceNumber || 'N/A'}</p>
              <p><strong>Created At:</strong> {new Date(selectedOrderDetail.order.createdAt).toLocaleString()}</p>
            </div>

            <h4 style={{ marginTop: '20px', marginBottom: '8px' }}>Trade Fills</h4>
            {selectedOrderDetail.trades.length === 0 ? (
              <p style={{ color: 'var(--text-secondary)', fontSize: '12px' }}>No execution fills for this order yet.</p>
            ) : (
              <table className="data-table" style={{ fontSize: '12px' }}>
                <thead>
                  <tr>
                    <th>Trade Ref</th>
                    <th>Qty</th>
                    <th>Price</th>
                    <th>Total</th>
                    <th>Time</th>
                  </tr>
                </thead>
                <tbody>
                  {selectedOrderDetail.trades.map((tr) => (
                    <tr key={tr.tradeId || tr.id}>
                      <td><code style={{ color: '#818cf8', fontSize: '10px' }}>{tr.tradeId}</code></td>
                      <td>{tr.quantity}</td>
                      <td>${Number(tr.price).toFixed(2)}</td>
                      <td>${Number(tr.totalValue).toFixed(2)}</td>
                      <td>{new Date(tr.executedAt).toLocaleTimeString()}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}

            <div style={{ marginTop: '20px', textAlign: 'right' }}>
              <button
                type="button"
                className="submit-order-btn"
                style={{ background: '#4b5563', padding: '6px 16px', fontSize: '13px' }}
                onClick={() => setSelectedOrderDetail(null)}
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modify Order Modal */}
      {modifyingOrder && (
        <div className="modal-backdrop" style={{
          position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
          background: 'rgba(0,0,0,0.7)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000
        }}>
          <div className="card" style={{ maxWidth: '450px', width: '90%' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h3>Modify Order #{modifyingOrder.id} ({modifyingOrder.stock?.symbol || modifyingOrder.symbol})</h3>
              <button
                style={{ background: 'transparent', border: 'none', color: '#fff', fontSize: '18px', cursor: 'pointer' }}
                onClick={() => setModifyingOrder(null)}
              >
                ✕
              </button>
            </div>

            {actionError && (
              <div className="alert alert-error" style={{ marginBottom: '12px' }}>
                {actionError}
              </div>
            )}

            <p style={{ fontSize: '12px', color: '#f59e0b', marginBottom: '16px' }}>
              ⚠️ Note: Changing price or increasing quantity resets your order's time-priority in the order book.
            </p>

            <form onSubmit={handleSaveModify}>
              {modifyingOrder.orderType === 'LIMIT' && (
                <div className="form-group">
                  <label>New Limit Price ($)</label>
                  <input
                    type="number"
                    step="0.01"
                    value={newPrice}
                    onChange={(e) => setNewPrice(e.target.value)}
                    className="form-input"
                    required
                  />
                </div>
              )}

              <div className="form-group">
                <label>New Total Quantity</label>
                <input
                  type="number"
                  min={Number(modifyingOrder.filledQuantity || 0) + 1}
                  value={newQuantity}
                  onChange={(e) => setNewQuantity(e.target.value)}
                  className="form-input"
                  required
                />
                <small style={{ color: 'var(--text-secondary)' }}>
                  Already filled: {modifyingOrder.filledQuantity || 0}
                </small>
              </div>

              <div style={{ display: 'flex', gap: '12px', marginTop: '20px' }}>
                <button
                  type="submit"
                  className="submit-order-btn"
                  style={{ flex: 1, background: '#6366f1' }}
                  disabled={modifying}
                >
                  {modifying ? 'Updating...' : 'Save Changes'}
                </button>
                <button
                  type="button"
                  className="submit-order-btn"
                  style={{ flex: 1, background: '#4b5563' }}
                  onClick={() => setModifyingOrder(null)}
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default OrdersPage;
