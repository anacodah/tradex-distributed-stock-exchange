import React, { useState, useEffect } from 'react';
import api from '../api/axios';
import { 
  BarChart3, 
  TrendingUp, 
  TrendingDown, 
  DollarSign, 
  Activity, 
  PieChart, 
  CheckCircle, 
  XCircle, 
  RefreshCw,
  Layers,
  ArrowUpRight,
  ArrowDownRight
} from 'lucide-react';

interface AnalyticsData {
  totalTradingVolume: number;
  totalTradeCount: number;
  totalOrderCount: number;
  filledOrderCount: number;
  cancelledOrderCount: number;
  rejectedOrderCount: number;
  fillRatePercent: number;
  cancellationRatePercent: number;
  rejectionRatePercent: number;
  totalBuyVolume: number;
  totalSellVolume: number;
  buyRatioPercent: number;
  sellRatioPercent: number;
  mostActiveInstruments: Array<{ symbol: string; volume: number; tradeCount: number }>;
  topMarketGainers: Array<{ symbol: string; price: number; changePercent: number }>;
  topMarketLosers: Array<{ symbol: string; price: number; changePercent: number }>;
  timestamp: number;
}

const Analytics: React.FC = () => {
  const [data, setData] = useState<AnalyticsData | null>(null);
  const [loading, setLoading] = useState<boolean>(true);

  const fetchAnalytics = async () => {
    try {
      const res = await api.get('/admin/analytics');
      setData(res.data);
    } catch (err: any) {
      console.error('Failed fetching analytics:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAnalytics();
    const interval = setInterval(fetchAnalytics, 5000);
    return () => clearInterval(interval);
  }, []);

  if (loading && !data) {
    return <div style={{ padding: '24px', color: '#94a3b8' }}>Loading Real Trading Analytics...</div>;
  }

  return (
    <div className="analytics-page" style={{ padding: '24px', color: '#f8fafc' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '24px' }}>
        <div>
          <h1 style={{ fontSize: '28px', fontWeight: 800, margin: 0, display: 'flex', alignItems: 'center', gap: '10px' }}>
            <BarChart3 size={32} color="#3b82f6" />
            Trading Analytics & Market Intelligence
          </h1>
          <p style={{ color: '#94a3b8', margin: '6px 0 0 0', fontSize: '14px' }}>
            Phase 13: Computed entirely from persisted PostgreSQL orders, executed trades, and live market pricing.
          </p>
        </div>
        <button
          onClick={fetchAnalytics}
          style={{
            background: '#1e293b',
            border: '1px solid #334155',
            color: '#f8fafc',
            padding: '8px 16px',
            borderRadius: '8px',
            cursor: 'pointer',
            fontSize: '13px',
            display: 'flex',
            alignItems: 'center',
            gap: '6px'
          }}
        >
          <RefreshCw size={14} /> Refresh Analytics
        </button>
      </div>

      {/* METRIC RIBBON */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '16px', marginBottom: '24px' }}>
        <div style={{ background: '#1e293b', border: '1px solid #334155', borderRadius: '12px', padding: '18px' }}>
          <div style={{ fontSize: '12px', color: '#94a3b8', textTransform: 'uppercase' }}>Total Trading Volume</div>
          <div style={{ fontSize: '26px', fontWeight: 800, color: '#38bdf8', marginTop: '4px' }}>
            ${data?.totalTradingVolume ? Number(data.totalTradingVolume).toLocaleString(undefined, { minimumFractionDigits: 2 }) : '0.00'}
          </div>
          <div style={{ fontSize: '12px', color: '#64748b', marginTop: '4px' }}>
            Across {data?.totalTradeCount || 0} executed trades
          </div>
        </div>

        <div style={{ background: '#1e293b', border: '1px solid #334155', borderRadius: '12px', padding: '18px' }}>
          <div style={{ fontSize: '12px', color: '#94a3b8', textTransform: 'uppercase' }}>Order Fill Rate</div>
          <div style={{ fontSize: '26px', fontWeight: 800, color: '#34d399', marginTop: '4px' }}>
            {data?.fillRatePercent ? data.fillRatePercent.toFixed(1) : '0.0'}%
          </div>
          <div style={{ fontSize: '12px', color: '#64748b', marginTop: '4px' }}>
            {data?.filledOrderCount || 0} / {data?.totalOrderCount || 0} orders filled
          </div>
        </div>

        <div style={{ background: '#1e293b', border: '1px solid #334155', borderRadius: '12px', padding: '18px' }}>
          <div style={{ fontSize: '12px', color: '#94a3b8', textTransform: 'uppercase' }}>Buy / Sell Ratio</div>
          <div style={{ fontSize: '20px', fontWeight: 700, color: '#f8fafc', marginTop: '6px', display: 'flex', gap: '8px' }}>
            <span style={{ color: '#10b981' }}>{data?.buyRatioPercent?.toFixed(0)}% Buy</span>
            <span style={{ color: '#64748b' }}>|</span>
            <span style={{ color: '#ef4444' }}>{data?.sellRatioPercent?.toFixed(0)}% Sell</span>
          </div>
          <div style={{ fontSize: '12px', color: '#64748b', marginTop: '4px' }}>
            Volume distribution
          </div>
        </div>

        <div style={{ background: '#1e293b', border: '1px solid #334155', borderRadius: '12px', padding: '18px' }}>
          <div style={{ fontSize: '12px', color: '#94a3b8', textTransform: 'uppercase' }}>Order Rejection Rate</div>
          <div style={{ fontSize: '26px', fontWeight: 800, color: (data?.rejectionRatePercent || 0) > 0 ? '#fbbf24' : '#94a3b8', marginTop: '4px' }}>
            {data?.rejectionRatePercent ? data.rejectionRatePercent.toFixed(1) : '0.0'}%
          </div>
          <div style={{ fontSize: '12px', color: '#64748b', marginTop: '4px' }}>
            {data?.rejectedOrderCount || 0} rejected orders
          </div>
        </div>
      </div>

      {/* MOST ACTIVE INSTRUMENTS & MARKET MOVERS */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '20px', marginBottom: '24px' }}>
        {/* Most Active Instruments */}
        <div style={{ background: '#1e293b', border: '1px solid #334155', borderRadius: '12px', padding: '20px' }}>
          <h2 style={{ fontSize: '18px', fontWeight: 700, margin: '0 0 16px 0', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Layers size={20} color="#38bdf8" /> Most Active Instruments by Volume
          </h2>

          {data?.mostActiveInstruments && data.mostActiveInstruments.length > 0 ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              {data.mostActiveInstruments.map((item, idx) => (
                <div key={idx} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '12px 14px', background: '#0f172a', borderRadius: '8px' }}>
                  <div>
                    <span style={{ fontWeight: 800, fontSize: '16px', color: '#f8fafc' }}>{item.symbol}</span>
                    <div style={{ fontSize: '12px', color: '#94a3b8' }}>{item.tradeCount} trades executed</div>
                  </div>
                  <div style={{ textAlign: 'right' }}>
                    <span style={{ fontWeight: 700, fontSize: '15px', color: '#38bdf8' }}>${Number(item.volume).toLocaleString()}</span>
                    <div style={{ fontSize: '11px', color: '#64748b' }}>Traded Volume</div>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div style={{ color: '#64748b', fontSize: '14px' }}>No executed trades recorded yet. Place orders to populate real volume metrics.</div>
          )}
        </div>

        {/* Top Market Gainers */}
        <div style={{ background: '#1e293b', border: '1px solid #334155', borderRadius: '12px', padding: '20px' }}>
          <h2 style={{ fontSize: '18px', fontWeight: 700, margin: '0 0 16px 0', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <TrendingUp size={20} color="#10b981" /> Top Market Gainers
          </h2>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            {data?.topMarketGainers && data.topMarketGainers.length > 0 ? (
              data.topMarketGainers.map((stock, i) => (
                <div key={i} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px 14px', background: '#0f172a', borderRadius: '8px' }}>
                  <div>
                    <span style={{ fontWeight: 700, color: '#fff' }}>{stock.symbol}</span>
                    <div style={{ fontSize: '12px', color: '#94a3b8' }}>${Number(stock.price).toFixed(2)}</div>
                  </div>
                  <div style={{ color: '#10b981', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '2px' }}>
                    <ArrowUpRight size={16} /> +{stock.changePercent}%
                  </div>
                </div>
              ))
            ) : (
              <div style={{ color: '#64748b' }}>No market movers available.</div>
            )}
          </div>
        </div>

        {/* Top Market Losers */}
        <div style={{ background: '#1e293b', border: '1px solid #334155', borderRadius: '12px', padding: '20px' }}>
          <h2 style={{ fontSize: '18px', fontWeight: 700, margin: '0 0 16px 0', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <TrendingDown size={20} color="#ef4444" /> Top Market Losers
          </h2>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            {data?.topMarketLosers && data.topMarketLosers.length > 0 ? (
              data.topMarketLosers.map((stock, i) => (
                <div key={i} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px 14px', background: '#0f172a', borderRadius: '8px' }}>
                  <div>
                    <span style={{ fontWeight: 700, color: '#fff' }}>{stock.symbol}</span>
                    <div style={{ fontSize: '12px', color: '#94a3b8' }}>${Number(stock.price).toFixed(2)}</div>
                  </div>
                  <div style={{ color: '#ef4444', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '2px' }}>
                    <ArrowDownRight size={16} /> {stock.changePercent}%
                  </div>
                </div>
              ))
            ) : (
              <div style={{ color: '#64748b' }}>No market movers available.</div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default Analytics;
