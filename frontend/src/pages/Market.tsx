import React, { useEffect, useState, useRef } from 'react';
import api from '../api/axios';
import { AreaChart, Area, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts';

interface Stock {
  id: number;
  symbol: string;
  name: string;
  currentPrice: number;
  dayHigh: number;
  dayLow: number;
  openPrice: number;
  previousClose: number;
  volume: number;
  priceChange: number;
  dataSource?: string;
}

interface CompanyProfile {
  symbol: string;
  name: string;
  sector: string;
  industry: string;
  description: string;
  exchange: string;
}

interface MarketStats {
  totalTradedSecurities: number;
  advancers: number;
  decliners: number;
  unchanged: number;
  totalVolume: number;
}

interface Candle {
  bucketStart: string;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
}

interface BookLevel {
  price: number;
  quantity: number;
  orderCount: number;
}

interface OrderBookDepth {
  symbol: string;
  bids: BookLevel[];
  asks: BookLevel[];
}

const Market: React.FC = () => {
  const [stocks, setStocks] = useState<Stock[]>([]);
  const [selectedStock, setSelectedStock] = useState<Stock | null>(null);
  const [companyProfile, setCompanyProfile] = useState<CompanyProfile | null>(null);
  const [candles, setCandles] = useState<Candle[]>([]);
  const [timeframe, setTimeframe] = useState<'1M' | '1H' | '1D'>('1H');
  const [stats, setStats] = useState<MarketStats | null>(null);
  const [orderBook, setOrderBook] = useState<OrderBookDepth | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [loading, setLoading] = useState(true);

  // User wallet & holdings balance for trading ticket
  const [availableCash, setAvailableCash] = useState<number>(0);
  const [ownedShares, setOwnedShares] = useState<number>(0);

  // Buy / Sell Form state
  const [orderSide, setOrderSide] = useState<'BUY' | 'SELL'>('BUY');
  const [quantity, setQuantity] = useState<number>(1);
  const [orderType, setOrderType] = useState<'MARKET' | 'LIMIT' | 'STOP_LOSS'>('MARKET');
  const [limitPrice, setLimitPrice] = useState<string>('');
  const [stopPrice, setStopPrice] = useState<string>('');
  const [message, setMessage] = useState<{ text: string; type: 'success' | 'error' } | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const wsRef = useRef<WebSocket | null>(null);

  const fetchStocks = async () => {
    try {
      const url = searchQuery.trim() ? `/market/stocks?search=${encodeURIComponent(searchQuery)}` : '/market/stocks';
      const res = await api.get(url);
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

  const fetchStats = async () => {
    try {
      const res = await api.get('/market/stats');
      setStats(res.data);
    } catch (err) {
      console.debug('Failed to fetch market stats', err);
    }
  };

  const fetchBalances = async (symbol?: string) => {
    try {
      const [walletRes, holdingsRes] = await Promise.all([
        api.get('/wallet'),
        api.get('/portfolio/holdings')
      ]);
      if (walletRes.data) {
        setAvailableCash(Number(walletRes.data.availableBalance || 0));
      }
      if (holdingsRes.data && symbol) {
        const h = holdingsRes.data.find((item: any) => item.symbol === symbol || item.stock?.symbol === symbol);
        setOwnedShares(h ? Number(h.quantity || 0) : 0);
      }
    } catch (e) {
      // User might be unauthenticated or wallet not created
    }
  };

  const fetchCompanyProfile = async (symbol: string) => {
    try {
      const res = await api.get(`/market/companies/${symbol}`);
      setCompanyProfile(res.data);
    } catch (err) {
      setCompanyProfile(null);
    }
  };

  const fetchCandles = async (symbol: string, tf: string) => {
    try {
      const res = await api.get(`/market/candles/${symbol}?timeframe=${tf}`);
      if (res.data && res.data.length > 0) {
        setCandles(res.data);
      } else {
        setCandles([]);
      }
    } catch (err) {
      setCandles([]);
    }
  };

  const fetchOrderBook = async (symbol: string) => {
    try {
      const res = await api.get(`/orders/book/${symbol}?depth=5`);
      setOrderBook(res.data);
    } catch (err) {
      setOrderBook(null);
    }
  };

  useEffect(() => {
    fetchStocks();
    fetchStats();
    fetchBalances(selectedStock?.symbol);
    const interval = setInterval(() => {
      fetchStocks();
      fetchStats();
      if (selectedStock) {
        fetchOrderBook(selectedStock.symbol);
      }
    }, 5000);
    return () => clearInterval(interval);
  }, [searchQuery, selectedStock?.symbol]);

  useEffect(() => {
    if (selectedStock) {
      fetchCompanyProfile(selectedStock.symbol);
      fetchCandles(selectedStock.symbol, timeframe);
      fetchOrderBook(selectedStock.symbol);
      fetchBalances(selectedStock.symbol);
      setLimitPrice(selectedStock.currentPrice.toFixed(2));
      setStopPrice((selectedStock.currentPrice * 0.95).toFixed(2));
    }
  }, [selectedStock?.symbol, timeframe]);

  // WebSocket Live Updates
  useEffect(() => {
    const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsHost = window.location.hostname === 'localhost' ? 'localhost:8080' : window.location.host;
    const wsUrl = `${wsProtocol}//${wsHost}/ws/market`;

    try {
      const socket = new WebSocket(wsUrl);
      wsRef.current = socket;

      socket.onmessage = (event) => {
        try {
          const updatedStocks: Stock[] = JSON.parse(event.data);
          if (Array.isArray(updatedStocks)) {
            setStocks((prev) =>
              prev.map((s) => {
                const found = updatedStocks.find((u) => u.symbol === s.symbol);
                return found || s;
              })
            );
            if (selectedStock) {
              const updatedSelected = updatedStocks.find((u) => u.symbol === selectedStock.symbol);
              if (updatedSelected) {
                setSelectedStock((prev) => (prev ? { ...prev, ...updatedSelected } : updatedSelected));
              }
            }
          }
        } catch (e) {
          // Ignore parse errors
        }
      };

      return () => {
        socket.close();
      };
    } catch (e) {
      console.warn('WebSocket connection not supported, fallback to HTTP polling.');
    }
  }, [selectedStock?.symbol]);

  const handlePlaceOrder = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedStock) return;
    setSubmitting(true);
    setMessage(null);

    // Validate fields
    if (orderType === 'LIMIT' && (!limitPrice || Number(limitPrice) <= 0)) {
      setMessage({ text: 'Please enter a valid Limit Price.', type: 'error' });
      setSubmitting(false);
      return;
    }
    if (orderType === 'STOP_LOSS' && (!stopPrice || Number(stopPrice) <= 0)) {
      setMessage({ text: 'Please enter a valid Stop/Trigger Price.', type: 'error' });
      setSubmitting(false);
      return;
    }

    try {
      const payload: any = {
        symbol: selectedStock.symbol,
        side: orderSide,
        orderType: orderType,
        quantity: Number(quantity),
        price: orderType === 'LIMIT' ? Number(limitPrice) : null,
        stopPrice: orderType === 'STOP_LOSS' ? Number(stopPrice) : null,
      };

      // Idempotency key per unique submission
      const idempotencyKey = `ord-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`;

      const res = await api.post('/orders', payload, {
        headers: { 'Idempotency-Key': idempotencyKey }
      });

      if (res.data.status === 'REJECTED') {
        setMessage({
          text: `Order #${res.data.id} (${orderSide} ${quantity} ${selectedStock.symbol}) was REJECTED by matching engine (insufficient liquidity in order book).`,
          type: 'error',
        });
      } else {
        setMessage({
          text: `Order #${res.data.id} (${orderSide} ${quantity} ${selectedStock.symbol} [${orderType}]) submitted! Status: ${res.data.status}`,
          type: 'success',
        });
      }
      fetchStocks();
      fetchBalances(selectedStock.symbol);
      fetchOrderBook(selectedStock.symbol);
    } catch (err: any) {
      console.error('Order placement failed:', err);
      const errMsg =
        err.response?.data?.message ||
        err.response?.data?.error ||
        (typeof err.response?.data === 'string' ? err.response.data : null) ||
        err.message ||
        'Failed to place order. Check wallet balance or holdings.';
      setMessage({
        text: errMsg,
        type: 'error',
      });
    } finally {
      setSubmitting(false);
    }
  };

  const chartData = candles.length > 0
    ? candles.map((c) => ({
        time: new Date(c.bucketStart).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        price: Number(c.close),
      }))
    : [
        { time: 'Prev Close', price: selectedStock?.previousClose || 100 },
        { time: 'Open', price: selectedStock?.openPrice || selectedStock?.currentPrice || 100 },
        { time: 'Current', price: selectedStock?.currentPrice || 100 },
      ];

  const estimatedValue =
    (orderType === 'LIMIT'
      ? Number(limitPrice || 0)
      : orderType === 'STOP_LOSS'
      ? Number(stopPrice || selectedStock?.currentPrice || 0)
      : selectedStock?.currentPrice || 0) * quantity;

  if (loading) return <div className="loading-spinner">Loading Market Data...</div>;

  return (
    <div className="market-container">
      <div className="dashboard-header">
        <h1>TradeX Live Market & Order Desk</h1>
        <p>Authoritative matching engine, price-time order book, live chart & order execution</p>
      </div>

      {/* Market Statistics Banner */}
      {stats && (
        <div style={{ display: 'flex', gap: '16px', marginBottom: '20px', flexWrap: 'wrap' }}>
          <div className="card" style={{ flex: '1', minWidth: '150px', padding: '12px 16px' }}>
            <span style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>Advancers</span>
            <div style={{ fontSize: '18px', fontWeight: 'bold', color: '#10b981' }}>▲ {stats.advancers}</div>
          </div>
          <div className="card" style={{ flex: '1', minWidth: '150px', padding: '12px 16px' }}>
            <span style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>Decliners</span>
            <div style={{ fontSize: '18px', fontWeight: 'bold', color: '#ef4444' }}>▼ {stats.decliners}</div>
          </div>
          <div className="card" style={{ flex: '1', minWidth: '150px', padding: '12px 16px' }}>
            <span style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>Unchanged</span>
            <div style={{ fontSize: '18px', fontWeight: 'bold', color: 'var(--text-primary)' }}>— {stats.unchanged}</div>
          </div>
          <div className="card" style={{ flex: '1', minWidth: '180px', padding: '12px 16px' }}>
            <span style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>Total Volume</span>
            <div style={{ fontSize: '18px', fontWeight: 'bold', color: 'var(--text-primary)' }}>
              {stats.totalVolume ? stats.totalVolume.toLocaleString() : '0'} shares
            </div>
          </div>
        </div>
      )}

      <div className="market-grid">
        {/* Watchlist Panel */}
        <div className="card stock-list-card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
            <h3>Watchlist</h3>
          </div>
          <input
            type="text"
            placeholder="Search symbol or company..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="form-input"
            style={{ marginBottom: '12px', fontSize: '13px' }}
          />
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
                    setLimitPrice(stock.currentPrice.toFixed(2));
                  }}
                >
                  <div className="stock-info">
                    <span className="symbol">{stock.symbol}</span>
                    <span className="name">{stock.name}</span>
                  </div>
                  <div className="stock-price-info">
                    <span className="price">${stock.currentPrice.toFixed(2)}</span>
                    <span className={`change ${isPositive ? 'positive' : 'negative'}`}>
                      {isPositive ? '+' : ''}
                      {(stock.priceChange || 0).toFixed(2)}%
                    </span>
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Stock Detail, Chart & Order Book Panel */}
        <div className="card stock-detail-card">
          {selectedStock ? (
            <>
              <div
                className="detail-header"
                style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px' }}
              >
                <div>
                  <h2>
                    {selectedStock.name} ({selectedStock.symbol})
                  </h2>
                  <span
                    className="current-price"
                    style={{ fontSize: '24px', fontWeight: 700, color: 'var(--text-primary)' }}
                  >
                    ${selectedStock.currentPrice.toFixed(2)}
                  </span>
                  <span
                    style={{
                      marginLeft: '12px',
                      fontSize: '12px',
                      padding: '2px 8px',
                      borderRadius: '4px',
                      background: 'rgba(99, 102, 241, 0.15)',
                      color: '#818cf8',
                    }}
                  >
                    {selectedStock.dataSource || 'SIMULATED'}
                  </span>
                </div>
                <div
                  className="day-stats"
                  style={{ display: 'flex', gap: '16px', fontSize: '12px', color: 'var(--text-secondary)' }}
                >
                  <div>
                    <span>High:</span>{' '}
                    <strong style={{ color: 'var(--text-primary)' }}>
                      ${selectedStock.dayHigh?.toFixed(2) || selectedStock.currentPrice.toFixed(2)}
                    </strong>
                  </div>
                  <div>
                    <span>Low:</span>{' '}
                    <strong style={{ color: 'var(--text-primary)' }}>
                      ${selectedStock.dayLow?.toFixed(2) || selectedStock.currentPrice.toFixed(2)}
                    </strong>
                  </div>
                  <div>
                    <span>Vol:</span>{' '}
                    <strong style={{ color: 'var(--text-primary)' }}>
                      {selectedStock.volume?.toLocaleString() || 0}
                    </strong>
                  </div>
                </div>
              </div>

              {/* Timeframe selector */}
              <div style={{ display: 'flex', gap: '8px', marginTop: '16px' }}>
                {(['1M', '1H', '1D'] as const).map((tf) => (
                  <button
                    key={tf}
                    type="button"
                    onClick={() => setTimeframe(tf)}
                    style={{
                      padding: '4px 12px',
                      fontSize: '12px',
                      borderRadius: '4px',
                      border: '1px solid var(--border-color, #334155)',
                      background: timeframe === tf ? '#6366f1' : 'transparent',
                      color: timeframe === tf ? '#fff' : 'var(--text-secondary)',
                      cursor: 'pointer',
                    }}
                  >
                    {tf}
                  </button>
                ))}
              </div>

              {/* Price Chart */}
              <div className="chart-container" style={{ width: '100%', height: 240, marginTop: '12px' }}>
                <ResponsiveContainer>
                  <AreaChart data={chartData}>
                    <defs>
                      <linearGradient id="colorPrice" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="#6366f1" stopOpacity={0.4} />
                        <stop offset="95%" stopColor="#6366f1" stopOpacity={0} />
                      </linearGradient>
                    </defs>
                    <XAxis dataKey="time" stroke="#64748b" />
                    <YAxis domain={['auto', 'auto']} stroke="#64748b" />
                    <Tooltip
                      contentStyle={{ backgroundColor: '#18202d', borderColor: '#263347', color: '#f1f5f9' }}
                      formatter={(val: any) => [`$${Number(val || 0).toFixed(2)}`, 'Price']}
                    />
                    <Area
                      type="monotone"
                      dataKey="price"
                      stroke="#6366f1"
                      strokeWidth={2}
                      fillOpacity={1}
                      fill="url(#colorPrice)"
                    />
                  </AreaChart>
                </ResponsiveContainer>
              </div>

              {/* Order Book Depth L2 Component */}
              <div style={{ marginTop: '20px', padding: '12px', background: 'var(--bg-secondary, #1e293b)', borderRadius: '8px' }}>
                <h4 style={{ fontSize: '13px', marginBottom: '8px', color: 'var(--text-secondary)' }}>
                  Order Book (Price-Time Priority L2)
                </h4>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', fontSize: '12px' }}>
                  {/* Bids */}
                  <div>
                    <span style={{ color: '#10b981', fontWeight: 600 }}>Bids (Buy Orders)</span>
                    <table style={{ width: '100%', marginTop: '4px', textAlign: 'left' }}>
                      <thead>
                        <tr style={{ color: 'var(--text-secondary)' }}>
                          <th>Price</th>
                          <th>Qty</th>
                          <th>Count</th>
                        </tr>
                      </thead>
                      <tbody>
                        {orderBook?.bids && orderBook.bids.length > 0 ? (
                          orderBook.bids.map((b, i) => (
                            <tr key={i} style={{ color: '#10b981' }}>
                              <td>${Number(b.price).toFixed(2)}</td>
                              <td>{b.quantity}</td>
                              <td style={{ color: 'var(--text-secondary)' }}>{b.orderCount}</td>
                            </tr>
                          ))
                        ) : (
                          <tr><td colSpan={3} style={{ color: 'var(--text-secondary)' }}>No resting bids</td></tr>
                        )}
                      </tbody>
                    </table>
                  </div>

                  {/* Asks */}
                  <div>
                    <span style={{ color: '#ef4444', fontWeight: 600 }}>Asks (Sell Orders)</span>
                    <table style={{ width: '100%', marginTop: '4px', textAlign: 'left' }}>
                      <thead>
                        <tr style={{ color: 'var(--text-secondary)' }}>
                          <th>Price</th>
                          <th>Qty</th>
                          <th>Count</th>
                        </tr>
                      </thead>
                      <tbody>
                        {orderBook?.asks && orderBook.asks.length > 0 ? (
                          orderBook.asks.map((a, i) => (
                            <tr key={i} style={{ color: '#ef4444' }}>
                              <td>${Number(a.price).toFixed(2)}</td>
                              <td>{a.quantity}</td>
                              <td style={{ color: 'var(--text-secondary)' }}>{a.orderCount}</td>
                            </tr>
                          ))
                        ) : (
                          <tr><td colSpan={3} style={{ color: 'var(--text-secondary)' }}>No resting asks</td></tr>
                        )}
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>

              {/* Company Details */}
              {companyProfile && (
                <div
                  style={{
                    marginTop: '16px',
                    padding: '12px',
                    background: 'var(--bg-secondary, #1e293b)',
                    borderRadius: '8px',
                    fontSize: '12px',
                  }}
                >
                  <p style={{ color: 'var(--text-secondary)', margin: 0 }}>
                    <strong>{companyProfile.name}</strong> • {companyProfile.sector} • {companyProfile.exchange}
                  </p>
                </div>
              )}
            </>
          ) : (
            <p>Select a stock from the watchlist to view chart</p>
          )}
        </div>

        {/* Phase 4 Trading Ticket */}
        <div className="card order-panel-card">
          <h3>Trading Ticket</h3>
          {message && (
            <div className={`alert ${message.type === 'success' ? 'alert-success' : 'alert-error'}`}>
              {message.text}
            </div>
          )}

          {/* Available balance feedback */}
          <div style={{ marginBottom: '12px', padding: '8px 12px', background: 'var(--bg-secondary)', borderRadius: '6px', fontSize: '12px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span style={{ color: 'var(--text-secondary)' }}>Available Cash:</span>
              <strong style={{ color: '#10b981' }}>${availableCash.toFixed(2)}</strong>
            </div>
            {selectedStock && (
              <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '4px' }}>
                <span style={{ color: 'var(--text-secondary)' }}>Owned {selectedStock.symbol}:</span>
                <strong>{ownedShares} shares</strong>
              </div>
            )}
          </div>

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
                <option value="STOP_LOSS">Stop-Loss Order</option>
              </select>
            </div>

            <div className="form-group">
              <label>Quantity</label>
              <input
                type="number"
                min="1"
                value={quantity}
                onChange={(e) => setQuantity(Math.max(1, Number(e.target.value)))}
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

            {orderType === 'STOP_LOSS' && (
              <div className="form-group">
                <label>Stop / Trigger Price ($)</label>
                <input
                  type="number"
                  step="0.01"
                  value={stopPrice}
                  onChange={(e) => setStopPrice(e.target.value)}
                  className="form-input"
                  required
                />
                <small style={{ color: 'var(--text-secondary)', fontSize: '11px' }}>
                  {orderSide === 'SELL'
                    ? 'Executes market sell when price drops to or below stop price.'
                    : 'Executes market buy when price rises to or above stop price.'}
                </small>
              </div>
            )}

            <div
              className="order-summary"
              style={{
                margin: '16px 0',
                padding: '12px',
                background: 'var(--bg-secondary)',
                borderRadius: 'var(--radius)',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '13px' }}>
                <span style={{ color: 'var(--text-secondary)' }}>Estimated Value:</span>
                <strong style={{ color: 'var(--text-primary)' }}>
                  ${estimatedValue.toFixed(2)}
                </strong>
              </div>
              {orderSide === 'BUY' && estimatedValue > availableCash && (
                <div style={{ color: '#ef4444', fontSize: '11px', marginTop: '4px' }}>
                  ⚠️ Exceeds available cash (${availableCash.toFixed(2)})
                </div>
              )}
              {orderSide === 'SELL' && quantity > ownedShares && (
                <div style={{ color: '#ef4444', fontSize: '11px', marginTop: '4px' }}>
                  ⚠️ Exceeds owned shares ({ownedShares})
                </div>
              )}
            </div>

            <button
              type="submit"
              className={`submit-order-btn ${orderSide.toLowerCase()}`}
              disabled={submitting || !selectedStock}
            >
              {submitting ? 'Submitting to Engine...' : `${orderSide} ${selectedStock?.symbol || ''} [${orderType}]`}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
};

export default Market;
