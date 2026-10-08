import React, { useEffect, useState } from 'react';
import api from '../api/axios';
import { Wallet as WalletIcon, ArrowUpRight, ArrowDownLeft } from 'lucide-react';

interface WalletData {
  id: number;
  balance: number;
  currency: string;
}

interface WalletTransaction {
  id: number;
  type: string;
  amount: number;
  timestamp: string;
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
        text: err.response?.data?.message || `Failed to ${actionType}`,
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
        <h1>TradeX Wallet</h1>
        <p>Manage your cash balance, deposits, withdrawals, and ledger</p>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px', marginBottom: '28px' }}>
        {/* Wallet Balance Card */}
        <div className="card balance-card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
            <h3 style={{ margin: 0 }}>Total Cash Balance</h3>
            <WalletIcon size={24} style={{ color: 'var(--yellow)' }} />
          </div>
          <div style={{ fontSize: '32px', fontWeight: 700, color: 'var(--text-primary)', marginBottom: '16px' }}>
            ${wallet?.balance !== undefined ? Number(wallet.balance).toFixed(2) : '0.00'}
            <span style={{ fontSize: '14px', color: 'var(--text-muted)', marginLeft: '8px' }}>{wallet?.currency || 'USD'}</span>
          </div>
          <div style={{ display: 'flex', gap: '10px' }}>
            <button 
              type="button"
              className={`tab-btn ${actionType === 'deposit' ? 'active buy' : ''}`}
              style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px' }}
              onClick={() => setActionType('deposit')}
            >
              <ArrowDownLeft size={16} /> Deposit Funds
            </button>
            <button 
              type="button"
              className={`tab-btn ${actionType === 'withdraw' ? 'active buy' : ''}`}
              style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px' }}
              onClick={() => setActionType('withdraw')}
            >
              <ArrowUpRight size={16} /> Withdraw Funds
            </button>
          </div>
        </div>

        {/* Form Card */}
        <div className="card form-card">
          <h3>{actionType === 'deposit' ? 'Deposit Funds' : 'Withdraw Funds'}</h3>
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
                placeholder="0.00"
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
      </div>

      {/* Transaction History Table */}
      <div className="card full-width">
        <h3>Transaction History</h3>
        {transactions.length === 0 ? (
          <p style={{ color: 'var(--text-muted)', fontSize: '14px', padding: '16px 0' }}>No transactions recorded yet.</p>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Type</th>
                <th>Amount</th>
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
                    <td>{tx.description || '-'}</td>
                    <td>{new Date(tx.timestamp || Date.now()).toLocaleString()}</td>
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
