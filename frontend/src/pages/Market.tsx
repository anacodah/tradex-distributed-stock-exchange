import React, { useEffect, useState } from 'react';
import api from '../api/axios';
import { AreaChart, Area, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts';

interface Stock {
  id: number;
  symbol: string;
  name: string;
  currentPrice: number;
  dayHigh: number;
  dayLow: number;
  volume: number;
  priceChange: number;
}

const mockChartData = [
  { time: '09:30', price: 175.2 },
  { time: '10:00', price: 177.5 },
  { time: '10:30', price: 176.8 },
  { time: '11:00', price: 179.1 },
  { time: '11:30', price: 181.4 },
  { time: '12:00', price: 180.2 },
  { time: '12:30', price: 182.9 },
  { time: '13:00', price: 185.0 },
];

const Market: React.FC = () => {
  const [stocks, setStocks] = useState<Stock[]>([]);
  const [selectedStock, setSelectedStock] = useState<Stock | null>(null);
  const [loading, setLoading] = useState(true);
  
  // Buy / Sell Form state
  const [orderSide, setOrderSide] = useState<'BUY' | 'SELL'>('BUY');
  const [quantity, setQuantity] = useState<number>(1);
  const [orderType, setOrderType] = useState<'MARKET' | 'LIMIT'>('MARKET');
  const [limitPrice, setLimitPrice] = useState<string>('');
  const [message, setMessage] = useState<{ text: string; type: 'success' | 'error' } | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const fetchStocks = async () => {
    try {
      const res = await api.get('/market/stocks');
      setStocks(res.data);
      if (res.data.length > 0 && !selectedStock) {
        setSelectedStock(res.data[0]);
        setLimitPrice(res.data[0].currentPrice.toString());
      }
    } catch (err) {
      console.error('Failed to fetch stocks', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchStocks();
    const interval = setInterval(fetchStocks, 5000);
    return () => clearInterval(interval);
  }, []);

  const handlePlaceOrder = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedStock) return;
    setSubmitting(true);
    setMessage(null);

    try {
      const payload = {
        symbol: selectedStock.symbol,
        side: orderSide,
        orderType: orderType,
        quantity: Number(quantity),
        price: orderType === 'LIMIT' ? Number(limitPrice) : selectedStock.currentPrice
      };

      const res = await api.post('/orders', payload);
      setMessage({ 
        text: `Order #${res.data.id} (${orderSide} ${quantity} ${selectedStock.symbol}) placed successfully! Status: ${res.data.status}`, 
        type: 'success' 
      });
      fetchStocks();
    } catch (err: any) {
      setMessage({ 
        text: err.response?.data?.message || err.response?.data?.error || 'Failed to place order. Check wallet balance or holdings.', 
        type: 'error' 
      });
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <div className="loading-spinner">Loading Market Data...</div>;

  return (
    <div className="market-container">
      <div className="dashboard-header">
        <h1>TradeX Live Market</h1>
        <p>Real-time stock prices, interactive charts & instant order execution</p>
      </div>

      <div className="market-grid">
        {/* Stock List Panel */}
        <div className="card stock-list-card">
          <h3>Watchlist</h3>
          <div className="stock-list">
            {stocks.map((stock) => {
              const isPositive = (stock.priceChange || 0) >= 0;
              const isSelected = selectedStock?.symbol === stock.symbol;
              return (
                <div 
                  key={stock.symbol} 
                  className={`stock-item ${isSelected ? 'selected' : ''}`}
                  onClick={() => {
                    setSelectedStock(stock);
                    setLimitPrice(stock.currentPrice.toString());
                  }}
                >
                  <div className="stock-info">
                    <span className="symbol">{stock.symbol}</span>
                    <span className="name">{stock.name}</span>
                  </div>
                  <div className="stock-price-info">
                    <span className="price">${stock.currentPrice.toFixed(2)}</span>
                    <span className={`change ${isPositive ? 'positive' : 'negative'}`}>
                      {isPositive ? '+' : ''}{(stock.priceChange || 0).toFixed(2)}%
                    </span>
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Stock Detail & Chart Panel */}
        <div className="card stock-detail-card">
          {selectedStock ? (
            <>
              <div className="detail-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                  <h2>{selectedStock.name} ({selectedStock.symbol})</h2>
                  <span className="current-price" style={{ fontSize: '24px', fontWeight: 700, color: 'var(--text-primary)' }}>
                    ${selectedStock.currentPrice.toFixed(2)}
                  </span>
                </div>
                <div className="day-stats" style={{ display: 'flex', gap: '16px', fontSize: '12px', color: 'var(--text-secondary)' }}>
                  <div><span>24h High:</span> <strong style={{ color: 'var(--text-primary)' }}>${selectedStock.dayHigh?.toFixed(2) || selectedStock.currentPrice}</strong></div>
                  <div><span>24h Low:</span> <strong style={{ color: 'var(--text-primary)' }}>${selectedStock.dayLow?.toFixed(2) || selectedStock.currentPrice}</strong></div>
                </div>
              </div>

              <div className="chart-container" style={{ width: '100%', height: 280, marginTop: '20px' }}>
                <ResponsiveContainer>
                  <AreaChart data={mockChartData}>
                    <defs>
                      <linearGradient id="colorPrice" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="#6366f1" stopOpacity={0.4}/>
                        <stop offset="95%" stopColor="#6366f1" stopOpacity={0}/>
                      </linearGradient>
                    </defs>
                    <XAxis dataKey="time" stroke="#64748b" />
                    <YAxis domain={['auto', 'auto']} stroke="#64748b" />
                    <Tooltip 
                      contentStyle={{ backgroundColor: '#18202d', borderColor: '#263347', color: '#f1f5f9' }}
                      formatter={(val: any) => [`$${Number(val || 0).toFixed(2)}`, 'Price']}
                    />
                    <Area type="monotone" dataKey="price" stroke="#6366f1" strokeWidth={2} fillOpacity={1} fill="url(#colorPrice)" />
                  </AreaChart>
                </ResponsiveContainer>
              </div>
            </>
          ) : (
            <p>Select a stock from the watchlist to view chart</p>
          )}
        </div>

        {/* Order Execution Panel */}
        <div className="card order-panel-card">
          <h3>Order Entry</h3>
          {message && (
            <div className={`alert ${message.type === 'success' ? 'alert-success' : 'alert-error'}`}>
              {message.text}
            </div>
          )}

          <form onSubmit={handlePlaceOrder}>
            <div className="order-type-tabs">
              <button 
                type="button" 
                className={`tab-btn buy ${orderSide === 'BUY' ? 'active' : ''}`}
                onClick={() => setOrderSide('BUY')}
              >
                BUY
              </button>
              <button 
                type="button" 
                className={`tab-btn sell ${orderSide === 'SELL' ? 'active' : ''}`}
                onClick={() => setOrderSide('SELL')}
              >
                SELL
              </button>
            </div>

            <div className="form-group">
              <label>Order Type</label>
              <select 
                value={orderType} 
                onChange={(e) => setOrderType(e.target.value as any)}
                className="form-input"
              >
                <option value="MARKET">Market Order</option>
                <option value="LIMIT">Limit Order</option>
              </select>
            </div>

            <div className="form-group">
              <label>Quantity</label>
              <input 
                type="number" 
                min="1" 
                value={quantity} 
                onChange={(e) => setQuantity(Number(e.target.value))}
                className="form-input"
                required 
              />
            </div>

            {orderType === 'LIMIT' && (
              <div className="form-group">
                <label>Limit Price ($)</label>
                <input 
                  type="number" 
                  step="0.01"
                  value={limitPrice} 
                  onChange={(e) => setLimitPrice(e.target.value)}
                  className="form-input"
                  required 
                />
              </div>
            )}

            <div className="order-summary" style={{ margin: '16px 0', padding: '12px', background: 'var(--bg-secondary)', borderRadius: 'var(--radius)' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '13px' }}>
                <span style={{ color: 'var(--text-secondary)' }}>Estimated Value:</span>
                <strong style={{ color: 'var(--text-primary)' }}>
                  ${((orderType === 'LIMIT' ? Number(limitPrice || 0) : (selectedStock?.currentPrice || 0)) * quantity).toFixed(2)}
                </strong>
              </div>
            </div>

            <button 
              type="submit" 
              className={`submit-order-btn ${orderSide.toLowerCase()}`}
              disabled={submitting || !selectedStock}
            >
              {submitting ? 'Executing...' : `${orderSide} ${selectedStock?.symbol || ''}`}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
};

export default Market;
