import React, { useEffect, useState } from 'react';
import api from '../api/axios';
import { Briefcase, TrendingUp, PieChart as PieIcon, RefreshCw, AlertTriangle, DollarSign, Clock } from 'lucide-react';
import { ResponsiveContainer, PieChart, Pie, Cell, Tooltip } from 'recharts';

interface Holding {
  symbol: string;
  companyName: string;
  totalQuantity: number;
  sellableQuantity: number;
  reservedQuantity: number;
  averagePurchasePrice: number;
  currentPrice: number;
  previousClose: number;
  marketValue: number;
  costBasis: number;
  realizedPnl: number;
  unrealizedPnl: number;
  unrealizedPnlPct: number;
  dailyPnl: number;
  dailyPnlPct: number;
  allocationPct: number;
  isPriceStale: boolean;
}

interface Allocation {
  asset: string;
  value: number;
  percentage: number;
}

interface PortfolioSummary {
  totalPortfolioValue: number;
  totalMarketValue: number;
  totalCostBasis: number;
  availableCash: number;
  reservedCash: number;
  totalCash: number;
  totalRealizedPnl: number;
  totalUnrealizedPnl: number;
  totalUnrealizedPnlPct: number;
  totalDailyPnl: number;
  totalDailyPnlPct: number;
  costBasisMethod: string;
  asOf: string;
  holdings: Holding[];
  allocations: Allocation[];
}

const COLORS = ['#6366f1', '#10b981', '#f59e0b', '#ec4899', '#8b5cf6', '#06b6d4', '#64748b'];

const Portfolio: React.FC = () => {
  const [portfolio, setPortfolio] = useState<PortfolioSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [reconciling, setReconciling] = useState(false);
  const [reconcileMessage, setReconcileMessage] = useState<string | null>(null);

  const fetchPortfolio = async () => {
    try {
      const res = await api.get('/portfolio');
      setPortfolio(res.data);
    } catch (err) {
      console.error('Failed to fetch portfolio', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPortfolio();
    const interval = setInterval(fetchPortfolio, 6000);
    return () => clearInterval(interval);
  }, []);

  const handleReconcile = async () => {
    setReconciling(true);
    setReconcileMessage(null);
    try {
      const res = await api.post('/portfolio/reconcile');
      if (res.data.reconciled) {
        setReconcileMessage(`Positions verified against ${res.data.positionsChecked} trade position(s). All records in sync!`);
      } else {
        setReconcileMessage(`Reconciliation completed: Repaired ${res.data.positionsRepaired} position projection(s).`);
      }
      fetchPortfolio();
    } catch (err: any) {
      setReconcileMessage('Failed to reconcile positions.');
    } finally {
      setReconciling(false);
    }
  };

  if (loading) return <div className="loading-spinner">Loading Portfolio Analytics...</div>;

  const isDailyPositive = (portfolio?.totalDailyPnl || 0) >= 0;
  const isUnrealizedPositive = (portfolio?.totalUnrealizedPnl || 0) >= 0;
  const isRealizedPositive = (portfolio?.totalRealizedPnl || 0) >= 0;

  return (
    <div className="portfolio-container">
      <div className="dashboard-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h1>Portfolio & Performance Analytics</h1>
          <p>
            Authoritative positions computed via <strong>Weighted-Average Cost (WAC)</strong> & live exchange benchmarks
          </p>
        </div>
        <button
          type="button"
          className="tab-btn"
          style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '8px 16px', background: '#334155', borderRadius: '6px' }}
          disabled={reconciling}
          onClick={handleReconcile}
        >
          <RefreshCw size={15} className={reconciling ? 'spin' : ''} />
          {reconciling ? 'Reconciling Trades...' : 'Reconcile Positions'}
        </button>
      </div>

      {reconcileMessage && (
        <div className="alert alert-success" style={{ marginBottom: '16px' }}>
          {reconcileMessage}
        </div>
      )}

      {/* Primary KPI Stats Grid */}
      <div className="stats-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '16px', marginBottom: '24px' }}>
        <div className="stat-card">
          <div className="stat-icon"><Briefcase size={22} /></div>
          <div className="stat-info">
            <h4>Total Portfolio Value</h4>
            <p className="highlight-val">${Number(portfolio?.totalPortfolioValue || 0).toFixed(2)}</p>
            <small style={{ color: 'var(--text-secondary)' }}>Cash + Equities</small>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon"><TrendingUp size={22} /></div>
          <div className="stat-info">
            <h4>Daily P&L (Today)</h4>
            <p style={{ fontSize: '18px', fontWeight: 'bold', color: isDailyPositive ? '#10b981' : '#ef4444' }}>
              {isDailyPositive ? '+' : ''}${Number(portfolio?.totalDailyPnl || 0).toFixed(2)} ({isDailyPositive ? '+' : ''}{Number(portfolio?.totalDailyPnlPct || 0).toFixed(2)}%)
            </p>
            <small style={{ color: 'var(--text-secondary)' }}>vs. Previous Close</small>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon"><DollarSign size={22} /></div>
          <div className="stat-info">
            <h4>Total Unrealized P&L</h4>
            <p style={{ fontSize: '18px', fontWeight: 'bold', color: isUnrealizedPositive ? '#10b981' : '#ef4444' }}>
              {isUnrealizedPositive ? '+' : ''}${Number(portfolio?.totalUnrealizedPnl || 0).toFixed(2)} ({isUnrealizedPositive ? '+' : ''}{Number(portfolio?.totalUnrealizedPnlPct || 0).toFixed(2)}%)
            </p>
            <small style={{ color: 'var(--text-secondary)' }}>Cost Basis: ${Number(portfolio?.totalCostBasis || 0).toFixed(2)}</small>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon"><PieIcon size={22} /></div>
          <div className="stat-info">
            <h4>Realized P&L (Closed)</h4>
            <p style={{ fontSize: '18px', fontWeight: 'bold', color: isRealizedPositive ? '#10b981' : '#ef4444' }}>
              {isRealizedPositive ? '+' : ''}${Number(portfolio?.totalRealizedPnl || 0).toFixed(2)}
            </p>
            <small style={{ color: 'var(--text-secondary)' }}>Cumulative Sales Gain</small>
          </div>
        </div>
      </div>

      {/* Cash Breakdown Banner */}
      <div className="card" style={{ marginBottom: '24px', padding: '14px 20px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <span style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>Available Trading Cash: </span>
          <strong style={{ color: '#10b981', fontSize: '16px' }}>${Number(portfolio?.availableCash || 0).toFixed(2)}</strong>
        </div>
        <div>
          <span style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>Reserved for Orders: </span>
          <strong style={{ color: '#f59e0b', fontSize: '16px' }}>${Number(portfolio?.reservedCash || 0).toFixed(2)}</strong>
        </div>
        <div>
          <span style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>Equities Market Value: </span>
          <strong style={{ color: '#6366f1', fontSize: '16px' }}>${Number(portfolio?.totalMarketValue || 0).toFixed(2)}</strong>
        </div>
      </div>

      {/* Asset Allocation Chart & Details */}
      {portfolio?.allocations && portfolio.allocations.length > 0 && (
        <div className="card" style={{ marginBottom: '24px' }}>
          <h3>Asset Allocation Breakdown</h3>
          <div style={{ display: 'grid', gridTemplateColumns: 'minmax(250px, 320px) 1fr', gap: '24px', alignItems: 'center' }}>
            <div style={{ width: '100%', height: 200 }}>
              <ResponsiveContainer>
                <PieChart>
                  <Pie
                    data={portfolio.allocations}
                    dataKey="value"
                    nameKey="asset"
                    cx="50%"
                    cy="50%"
                    outerRadius={75}
                    innerRadius={45}
                    paddingAngle={3}
                  >
                    {portfolio.allocations.map((_, index) => (
                      <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip formatter={(value: any) => [`$${Number(value || 0).toFixed(2)}`, 'Value']} />
                </PieChart>
              </ResponsiveContainer>
            </div>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '12px' }}>
              {portfolio.allocations.map((a, i) => (
                <div key={a.asset} style={{ display: 'flex', alignItems: 'center', gap: '8px', minWidth: '130px', padding: '6px 12px', background: 'var(--bg-secondary)', borderRadius: '6px' }}>
                  <div style={{ width: '12px', height: '12px', borderRadius: '3px', background: COLORS[i % COLORS.length] }} />
                  <div>
                    <strong style={{ fontSize: '13px' }}>{a.asset}</strong>
                    <div style={{ fontSize: '11px', color: 'var(--text-secondary)' }}>
                      {Number(a.percentage || 0).toFixed(1)}% (${Number(a.value || 0).toFixed(2)})
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Detailed Holdings Table */}
      <div className="card holdings-card full-width">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          <h3>Stock Holdings & Performance Breakdown</h3>
          <small style={{ color: 'var(--text-secondary)' }}>Method: {portfolio?.costBasisMethod || 'Weighted-Average Cost'}</small>
        </div>

        {!portfolio?.holdings || portfolio.holdings.length === 0 ? (
          <p className="empty-text">
            You currently do not own any stocks. Head over to the Market tab to place your first buy order!
          </p>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table className="data-table">
              <thead>
                <tr>
                  <th>Symbol</th>
                  <th>Quantity</th>
                  <th>Sellable</th>
                  <th>Reserved</th>
                  <th>Avg Cost</th>
                  <th>Current Price</th>
                  <th>Cost Basis</th>
                  <th>Market Value</th>
                  <th>Daily P&L</th>
                  <th>Unrealized P&L</th>
                  <th>Weight</th>
                </tr>
              </thead>
              <tbody>
                {portfolio.holdings.map((h) => {
                  const isHoldingDailyPos = Number(h.dailyPnl || 0) >= 0;
                  const isHoldingUnrealizedPos = Number(h.unrealizedPnl || 0) >= 0;

                  return (
                    <tr key={h.symbol}>
                      <td>
                        <strong>{h.symbol}</strong>
                        <div style={{ fontSize: '11px', color: 'var(--text-secondary)' }}>{h.companyName}</div>
                      </td>
                      <td><strong>{h.totalQuantity}</strong></td>
                      <td>{h.sellableQuantity}</td>
                      <td>
                        {Number(h.reservedQuantity) > 0 ? (
                          <span style={{ color: '#f59e0b' }}>{h.reservedQuantity}</span>
                        ) : (
                          '0'
                        )}
                      </td>
                      <td>${Number(h.averagePurchasePrice).toFixed(2)}</td>
                      <td>
                        ${Number(h.currentPrice).toFixed(2)}
                        {h.isPriceStale && (
                          <span title="Market price update delayed" style={{ marginLeft: '4px', color: '#f59e0b' }}>
                            <Clock size={12} />
                          </span>
                        )}
                      </td>
                      <td>${Number(h.costBasis).toFixed(2)}</td>
                      <td><strong>${Number(h.marketValue).toFixed(2)}</strong></td>
                      <td style={{ color: isHoldingDailyPos ? '#10b981' : '#ef4444' }}>
                        {isHoldingDailyPos ? '+' : ''}${Number(h.dailyPnl).toFixed(2)}
                        <div style={{ fontSize: '11px' }}>
                          ({isHoldingDailyPos ? '+' : ''}{Number(h.dailyPnlPct).toFixed(2)}%)
                        </div>
                      </td>
                      <td style={{ color: isHoldingUnrealizedPos ? '#10b981' : '#ef4444' }}>
                        {isHoldingUnrealizedPos ? '+' : ''}${Number(h.unrealizedPnl).toFixed(2)}
                        <div style={{ fontSize: '11px' }}>
                          ({isHoldingUnrealizedPos ? '+' : ''}{Number(h.unrealizedPnlPct).toFixed(2)}%)
                        </div>
                      </td>
                      <td>
                        <span className="badge" style={{ background: 'rgba(99, 102, 241, 0.15)', color: '#818cf8' }}>
                          {Number(h.allocationPct).toFixed(1)}%
                        </span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

export default Portfolio;
