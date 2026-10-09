import React, { useState, useEffect } from 'react';
import api from '../api/axios';
import { 
  History, 
  ArrowUpRight, 
  ArrowDownLeft, 
  Lock, 
  Unlock, 
  CheckCircle2, 
  Clock, 
  Filter, 
  Search,
  RefreshCw,
  ExternalLink
} from 'lucide-react';

interface UnifiedTransaction {
  id: string;
  sourceType: 'ORDER' | 'TRADE' | 'WALLET' | 'DISTRIBUTED' | 'ADMIN';
  recordType: string;
  userId: number;
  username: string;
  symbol?: string;
  amount?: number;
  quantity?: number;
  price?: number;
  status: string;
  orderId?: number;
  tradeId?: string;
  correlationId?: string;
  details?: string;
  timestamp: string;
}

export const TransactionHistory: React.FC = () => {
  const [transactions, setTransactions] = useState<UnifiedTransaction[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [typeFilter, setTypeFilter] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState<string>('');

  const fetchTransactions = async () => {
    try {
      setLoading(true);
      const url = typeFilter === 'ALL' 
        ? '/transactions/unified?limit=100' 
        : `/transactions/unified?type=${typeFilter}&limit=100`;
      const res = await api.get(url);
      setTransactions(res.data || []);
    } catch (err) {
      console.error('Failed to load transaction history:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTransactions();
  }, [typeFilter]);

  const filteredTransactions = transactions.filter((tx) => {
    if (!searchQuery.trim()) return true;
    const q = searchQuery.toLowerCase();
    return (
      tx.id.toLowerCase().includes(q) ||
      (tx.symbol && tx.symbol.toLowerCase().includes(q)) ||
      (tx.details && tx.details.toLowerCase().includes(q)) ||
      (tx.correlationId && tx.correlationId.toLowerCase().includes(q)) ||
      (tx.orderId && tx.orderId.toString().includes(q)) ||
      (tx.tradeId && tx.tradeId.toLowerCase().includes(q))
    );
  });

  const getRecordIcon = (recordType: string, sourceType: string) => {
    switch (recordType) {
      case 'WALLET_CREDIT':
      case 'TRADE_CREDIT':
        return <ArrowDownLeft className="text-success" size={16} />;
      case 'WALLET_DEBIT':
      case 'TRADE_DEBIT':
        return <ArrowUpRight className="text-danger" size={16} />;
      case 'FUND_RESERVATION':
        return <Lock className="text-warning" size={16} />;
      case 'FUND_RELEASE':
        return <Unlock className="text-info" size={16} />;
      case 'TRADE_EXECUTION':
        return <CheckCircle2 className="text-primary" size={16} />;
      default:
        return <Clock className="text-secondary" size={16} />;
    }
  };

  return (
    <div className="transactions-container">
      <div className="page-header">
        <div>
          <h1>Unified Transaction History & Ledger</h1>
          <p className="page-subtitle">
            Auditable lifecycle trace across order submissions, fills, fund movements, and reservations.
          </p>
        </div>
        <button className="btn-secondary" onClick={fetchTransactions}>
          <RefreshCw size={15} className={loading ? 'spinning' : ''} /> Refresh
        </button>
      </div>

      <div className="controls-card">
        <div className="filter-group">
          <Filter size={16} />
          <span className="filter-label">Filter Type:</span>
          {['ALL', 'ORDER', 'TRADE', 'WALLET', 'ADMIN', 'DISTRIBUTED'].map((type) => (
            <button
              key={type}
              className={`filter-btn ${typeFilter === type ? 'active' : ''}`}
              onClick={() => setTypeFilter(type)}
            >
              {type}
            </button>
          ))}
        </div>

        <div className="search-bar">
          <Search size={16} />
          <input
            type="text"
            placeholder="Search by ID, Symbol, Order #, Correlation ID..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>
      </div>

      <div className="card table-card">
        <div className="table-responsive">
          <table className="tradex-table">
            <thead>
              <tr>
                <th>Type</th>
                <th>Transaction ID</th>
                <th>Timestamp</th>
                <th>Asset / Symbol</th>
                <th>Quantity / Price</th>
                <th>Financial Movement</th>
                <th>Status</th>
                <th>Trace / Link</th>
                <th>Description</th>
              </tr>
            </thead>
            <tbody>
              {loading && transactions.length === 0 ? (
                <tr>
                  <td colSpan={9} className="text-center py-4">Loading ledger transactions...</td>
                </tr>
              ) : filteredTransactions.length === 0 ? (
                <tr>
                  <td colSpan={9} className="text-center py-4">No records found matching current criteria.</td>
                </tr>
              ) : (
                filteredTransactions.map((tx) => (
                  <tr key={tx.id}>
                    <td>
                      <span className={`badge-pill badge-${tx.sourceType.toLowerCase()}`}>
                        {getRecordIcon(tx.recordType, tx.sourceType)}
                        <span className="ml-1">{tx.recordType}</span>
                      </span>
                    </td>
                    <td className="font-mono text-xs">{tx.id}</td>
                    <td className="text-xs text-muted">
                      {new Date(tx.timestamp).toLocaleString()}
                    </td>
                    <td className="font-bold">{tx.symbol || 'USD (Cash)'}</td>
                    <td>
                      {tx.quantity ? (
                        <span>
                          {tx.quantity} {tx.price ? `@ $${tx.price.toFixed(2)}` : ''}
                        </span>
                      ) : (
                        '-'
                      )}
                    </td>
                    <td>
                      {tx.amount !== undefined && tx.amount !== null ? (
                        <span className={tx.amount >= 0 ? 'text-success font-semibold' : 'text-danger font-semibold'}>
                          {tx.amount >= 0 ? `+$${tx.amount.toFixed(2)}` : `-$${Math.abs(tx.amount).toFixed(2)}`}
                        </span>
                      ) : (
                        '-'
                      )}
                    </td>
                    <td>
                      <span className={`status-tag status-${tx.status.toLowerCase()}`}>
                        {tx.status}
                      </span>
                    </td>
                    <td className="text-xs font-mono">
                      {tx.orderId && <div>Order: #{tx.orderId}</div>}
                      {tx.tradeId && <div>Trade: {tx.tradeId.substring(0, 8)}...</div>}
                      {tx.correlationId && <div className="text-muted">Corr: {tx.correlationId}</div>}
                    </td>
                    <td className="text-xs">{tx.details || '-'}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

export default TransactionHistory;
