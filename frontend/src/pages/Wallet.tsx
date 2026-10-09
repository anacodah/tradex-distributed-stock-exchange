import React, { useEffect, useState } from 'react';
import api from '../api/axios';
import { Wallet as WalletIcon, ArrowUpRight, ArrowDownLeft, Lock, DollarSign } from 'lucide-react';

interface WalletData {
  id: number;
  balance: number;
  availableBalance: number;
  reservedBalance: number;
  currency?: string;
}

interface WalletTransaction {
  id: number;
  type: string;
  amount: number;
  balanceBefore?: number;
  balanceAfter?: number;
  createdAt?: string;
  timestamp?: string;
  description: string;
}

const WalletPage: React.FC = () => {
  const [wallet, setWallet] = useState<WalletData | null>(null);
  const [transactions, setTransactions] = useState<WalletTransaction[]>([]);
  const [amount, setAmount] = useState<string>('');
  const [actionType, setActionType] = useState<'deposit' | 'withdraw'>('deposit');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState<{ text: string; type: 'success' | 'error' } | null>(null);

  const fetchWallet = async () => {
    try {
      const [walletRes, txRes] = await Promise.all([
        api.get('/wallet'),
        api.get('/wallet/transactions')
      ]);
      setWallet(walletRes.data);
      setTransactions(txRes.data);
    } catch (err) {
      console.error('Failed to fetch wallet info', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchWallet();
  }, []);

  const handleTransaction = async (e: React.FormEvent) => {
    e.preventDefault();
    const numAmount = parseFloat(amount);
    if (!numAmount || numAmount <= 0) return;

    setSubmitting(true);
    setMessage(null);

    try {
      const endpoint = actionType === 'deposit' ? '/wallet/deposit' : '/wallet/withdraw';
      const res = await api.post(endpoint, { amount: numAmount });
      setWallet(res.data);
      setMessage({
        text: `Successfully ${actionType === 'deposit' ? 'deposited' : 'withdrawn'} $${numAmount.toFixed(2)}`,
        type: 'success'
      });
      setAmount('');
      fetchWallet();
    } catch (err: any) {
      setMessage({
        text: err.response?.data?.message || err.response?.data?.error || `Failed to ${actionType}`,
        type: 'error'
      });
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <div className="loading-spinner">Loading Wallet...</div>;

  return (
    <div className="wallet-container">
      <div className="dashboard-header">
        <h1>TradeX Virtual Wallet</h1>
        <p>Manage paper-trading funds, available capital, and immutable ledger entries</p>
      </div>

      {/* 3-Column Metrics Row */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '16px', marginBottom: '24px' }}>
        <div className="card" style={{ background: 'var(--bg-card)', border: '1px solid var(--border-color)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
            <span style={{ fontSize: '13px', color: 'var(--text-muted)' }}>Total Balance</span>
            <WalletIcon size={18} style={{ color: 'var(--yellow)' }} />
          </div>
          <div style={{ fontSize: '26px', fontWeight: 700 }}>
            ${wallet?.balance !== undefined ? Number(wallet.balance).toFixed(2) : '0.00'}
          </div>
          <div style={{ fontSize: '12px', color: 'var(--text-muted)', marginTop: '4px' }}>Total equity in virtual wallet</div>
        </div>

        <div className="card" style={{ background: 'var(--bg-card)', border: '1px solid var(--border-color)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
            <span style={{ fontSize: '13px', color: 'var(--text-muted)' }}>Available Balance</span>
            <DollarSign size={18} style={{ color: 'var(--green)' }} />
          </div>
          <div style={{ fontSize: '26px', fontWeight: 700, color: 'var(--green)' }}>
            ${wallet?.availableBalance !== undefined ? Number(wallet.availableBalance).toFixed(2) : '0.00'}
          </div>
          <div style={{ fontSize: '12px', color: 'var(--text-muted)', marginTop: '4px' }}>Available to buy stocks</div>
        </div>

        <div className="card" style={{ background: 'var(--bg-card)', border: '1px solid var(--border-color)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
            <span style={{ fontSize: '13px', color: 'var(--text-muted)' }}>Reserved Balance</span>
            <Lock size={18} style={{ color: '#f59e0b' }} />
          </div>
          <div style={{ fontSize: '26px', fontWeight: 700, color: '#f59e0b' }}>
            ${wallet?.reservedBalance !== undefined ? Number(wallet.reservedBalance).toFixed(2) : '0.00'}
          </div>
          <div style={{ fontSize: '12px', color: 'var(--text-muted)', marginTop: '4px' }}>Locked in pending limit orders</div>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px', marginBottom: '28px' }}>
        {/* Deposit/Withdraw Form */}
        <div className="card form-card">
          <div style={{ display: 'flex', gap: '10px', marginBottom: '16px' }}>
            <button 
              type="button"
              className={`tab-btn ${actionType === 'deposit' ? 'active buy' : ''}`}
              style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px' }}
              onClick={() => { setActionType('deposit'); setMessage(null); }}
            >
              <ArrowDownLeft size={16} /> Deposit
            </button>
            <button 
              type="button"
              className={`tab-btn ${actionType === 'withdraw' ? 'active buy' : ''}`}
              style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px' }}
              onClick={() => { setActionType('withdraw'); setMessage(null); }}
            >
              <ArrowUpRight size={16} /> Withdraw
            </button>
          </div>

          <h3>{actionType === 'deposit' ? 'Deposit Virtual Cash' : 'Withdraw Virtual Cash'}</h3>
          {message && (
            <div className={`alert ${message.type === 'success' ? 'alert-success' : 'alert-error'}`}>
              {message.text}
            </div>
          )}

          <form onSubmit={handleTransaction}>
            <div className="form-group">
              <label>Amount ($)</label>
              <input 
                type="number" 
                step="0.01" 
                min="1" 
                placeholder="1000.00"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                className="form-input"
                required 
              />
            </div>

            <button type="submit" className="refresh-btn" style={{ width: '100%', marginTop: '8px' }} disabled={submitting}>
              {submitting ? 'Processing...' : `Confirm ${actionType === 'deposit' ? 'Deposit' : 'Withdrawal'}`}
            </button>
          </form>
        </div>

        {/* Informational Panel */}
        <div className="card" style={{ background: 'var(--bg-card)', border: '1px solid var(--border-color)', display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
          <h3 style={{ marginTop: 0 }}>Financial Integrity Notice</h3>
          <ul style={{ paddingLeft: '20px', color: 'var(--text-muted)', fontSize: '13px', lineHeight: '1.7' }}>
            <li>Paper trading uses virtual simulator funds only. No real payment processing is performed.</li>
            <li>All cash movements write append-only transaction entries into the database ledger.</li>
            <li>Pending buy orders lock funds in <code>Reserved Balance</code> to prevent overselling or negative balances under concurrent orders.</li>
            <li>Every request is authorized server-side for the logged-in user context.</li>
          </ul>
        </div>
      </div>

      {/* Transaction History Table */}
      <div className="card full-width">
        <h3>Wallet Transaction History</h3>
        {transactions.length === 0 ? (
          <p style={{ color: 'var(--text-muted)', fontSize: '14px', padding: '16px 0' }}>No transactions recorded yet.</p>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Type</th>
                <th>Amount</th>
                <th>Balance Before</th>
                <th>Balance After</th>
                <th>Description</th>
                <th>Date & Time</th>
              </tr>
            </thead>
            <tbody>
              {transactions.map((tx: any) => {
                const txType = (tx.type || '').toUpperCase();
                const isCredit = txType === 'DEPOSIT' || txType === 'SELL' || txType === 'TRADE_CREDIT';
                return (
                  <tr key={tx.id}>
                    <td>#{tx.id}</td>
                    <td>
                      <span className={`badge ${isCredit ? 'badge-deposit' : 'badge-withdraw'}`}>
                        {txType}
                      </span>
                    </td>
                    <td className={isCredit ? 'text-green' : 'text-red'}>
                      {isCredit ? '+' : '-'}${Number(tx.amount || 0).toFixed(2)}
                    </td>
                    <td>${tx.balanceBefore !== undefined ? Number(tx.balanceBefore).toFixed(2) : '-'}</td>
                    <td>${tx.balanceAfter !== undefined ? Number(tx.balanceAfter).toFixed(2) : '-'}</td>
                    <td>{tx.description || '-'}</td>
                    <td>{new Date(tx.createdAt || tx.timestamp || Date.now()).toLocaleString()}</td>
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

export default WalletPage;
