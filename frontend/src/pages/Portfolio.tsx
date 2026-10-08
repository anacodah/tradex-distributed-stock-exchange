import React, { useEffect, useState } from 'react';
import api from '../api/axios';
import { Briefcase, TrendingUp, PieChart as PieIcon } from 'lucide-react';

interface Holding {
  id: number;
  symbol: string;
  quantity: number;
  averageBuyPrice: number;
  currentPrice?: number;
}

interface PortfolioSummary {
  totalValue: number;
  cashBalance: number;
  holdingsValue: number;
  holdings: Holding[];
}

const Portfolio: React.FC = () => {
  const [portfolio, setPortfolio] = useState<PortfolioSummary | null>(null);
  const [loading, setLoading] = useState(true);

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
  }, []);

  if (loading) return <div className="loading-spinner">Loading Portfolio...</div>;

  return (
    <div className="portfolio-container">
      <div className="dashboard-header">
        <h1>TradeX Portfolio</h1>
        <p>Overview of your current stock holdings & asset distribution</p>
      </div>

      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-icon"><Briefcase size={24} /></div>
          <div className="stat-info">
            <h4>Total Portfolio Value</h4>
            <p className="highlight-val">${portfolio?.totalValue?.toFixed(2) || '0.00'}</p>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon"><TrendingUp size={24} /></div>
          <div className="stat-info">
            <h4>Holdings Value</h4>
            <p>${portfolio?.holdingsValue?.toFixed(2) || '0.00'}</p>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon"><PieIcon size={24} /></div>
          <div className="stat-info">
            <h4>Available Cash</h4>
            <p>${portfolio?.cashBalance?.toFixed(2) || '0.00'}</p>
          </div>
        </div>
      </div>

      <div className="card holdings-card full-width">
        <h3>Stock Holdings</h3>
        {!portfolio?.holdings || portfolio.holdings.length === 0 ? (
          <p className="empty-text">You currently do not own any stocks. Head over to the Market tab to place your first buy order!</p>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>Symbol</th>
                <th>Quantity</th>
                <th>Avg Buy Price</th>
                <th>Current Price</th>
                <th>Market Value</th>
                <th>Unrealized P/L</th>
              </tr>
            </thead>
            <tbody>
              {portfolio.holdings.map((h) => {
                const currentPrice = h.currentPrice || h.averageBuyPrice;
                const marketVal = h.quantity * currentPrice;
                const costBasis = h.quantity * h.averageBuyPrice;
                const pnl = marketVal - costBasis;
                const pnlPercent = costBasis > 0 ? (pnl / costBasis) * 100 : 0;
                const isPositive = pnl >= 0;

                return (
                  <tr key={h.id}>
                    <td><strong>{h.symbol}</strong></td>
                    <td>{h.quantity}</td>
                    <td>${h.averageBuyPrice.toFixed(2)}</td>
                    <td>${currentPrice.toFixed(2)}</td>
                    <td>${marketVal.toFixed(2)}</td>
                    <td className={isPositive ? 'text-green' : 'text-red'}>
                      {isPositive ? '+' : ''}${pnl.toFixed(2)} ({isPositive ? '+' : ''}{pnlPercent.toFixed(2)}%)
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
};

export default Portfolio;
