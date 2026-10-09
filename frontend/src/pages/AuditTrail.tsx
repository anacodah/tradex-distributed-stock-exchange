import React, { useState, useEffect } from 'react';
import api from '../api/axios';
import { 
  ShieldAlert, 
  Search, 
  RefreshCw, 
  Lock, 
  User, 
  Tag, 
  Activity, 
  Calendar,
  AlertTriangle,
  CheckCircle,
  FileText
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';

interface AuditLog {
  id: number;
  actorId: number;
  actorUsername: string;
  action: string;
  entityType?: string;
  entityId?: string;
  correlationId?: string;
  outcome?: string;
  details?: string;
  ipAddress?: string;
  timestamp: string;
}

export const AuditTrail: React.FC = () => {
  const { user } = useAuth();
  const isAdmin = user?.roles?.includes('ADMIN') || user?.roles?.includes('ROLE_ADMIN');

  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [actionFilter, setActionFilter] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [viewScope, setViewScope] = useState<'MY' | 'ALL'>(isAdmin ? 'ALL' : 'MY');

  // Compensating transaction modal state
  const [showCompModal, setShowCompModal] = useState<boolean>(false);
  const [compUserId, setCompUserId] = useState<string>('');
  const [compAmount, setCompAmount] = useState<string>('');
  const [compIsCredit, setCompIsCredit] = useState<boolean>(true);
  const [compReason, setCompReason] = useState<string>('');
  const [compOrigTxId, setCompOrigTxId] = useState<string>('');
  const [compSubmitting, setCompSubmitting] = useState<boolean>(false);
  const [compSuccess, setCompSuccess] = useState<string | null>(null);

  const fetchAuditLogs = async () => {
    try {
      setLoading(true);
      setError(null);
      const url = viewScope === 'ALL' && isAdmin ? '/audit' : '/audit/my';
      const res = await api.get(url);
      setAuditLogs(res.data.content || []);
    } catch (err: any) {
      console.error('Failed to load audit logs:', err);
      if (err.response?.status === 403) {
        setError('Access denied. Administrator privileges required to view system-wide audit logs.');
      } else {
        setError('Failed to fetch audit log trail.');
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAuditLogs();
  }, [viewScope]);

  const handlePostCompensation = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setCompSubmitting(true);
      setCompSuccess(null);
      await api.post('/transactions/compensate', {
        userId: Number(compUserId),
        amount: Number(compAmount),
        isCredit: compIsCredit,
        reason: compReason,
        originalTxId: compOrigTxId || undefined,
      });
      setCompSuccess('Compensating ledger entry created successfully.');
      setShowCompModal(false);
      fetchAuditLogs();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Failed to submit compensating transaction');
    } finally {
      setCompSubmitting(false);
    }
  };

  const filteredLogs = auditLogs.filter((log) => {
    const matchesAction = actionFilter === 'ALL' || log.action.includes(actionFilter);
    if (!matchesAction) return false;
    if (!searchQuery.trim()) return true;
    const q = searchQuery.toLowerCase();
    return (
      log.action.toLowerCase().includes(q) ||
      (log.actorUsername && log.actorUsername.toLowerCase().includes(q)) ||
      (log.details && log.details.toLowerCase().includes(q)) ||
      (log.correlationId && log.correlationId.toLowerCase().includes(q)) ||
      (log.entityType && log.entityType.toLowerCase().includes(q))
    );
  });

  return (
    <div className="audit-container">
      <div className="page-header">
        <div>
          <h1>System Audit Trail & Security Logs</h1>
          <p className="page-subtitle">
            Immutable trace of security actions, orders, trades, wallet updates, and admin compensating entries.
          </p>
        </div>
        <div className="header-actions">
          {isAdmin && (
            <button className="btn-primary" onClick={() => setShowCompModal(true)}>
              <FileText size={15} /> Post Compensating Entry
            </button>
          )}
          <button className="btn-secondary" onClick={fetchAuditLogs}>
            <RefreshCw size={15} className={loading ? 'spinning' : ''} /> Refresh
          </button>
        </div>
      </div>

      {compSuccess && (
        <div className="alert alert-success">
          <CheckCircle size={16} /> {compSuccess}
        </div>
      )}

      {error && (
        <div className="alert alert-danger">
          <AlertTriangle size={16} /> {error}
        </div>
      )}

      <div className="controls-card">
        {isAdmin && (
          <div className="scope-toggle">
            <span className="filter-label">Scope:</span>
            <button
              className={`filter-btn ${viewScope === 'ALL' ? 'active' : ''}`}
              onClick={() => setViewScope('ALL')}
            >
              System Wide (Admin)
            </button>
            <button
              className={`filter-btn ${viewScope === 'MY' ? 'active' : ''}`}
              onClick={() => setViewScope('MY')}
            >
              My Activity
            </button>
          </div>
        )}

        <div className="search-bar">
          <Search size={16} />
          <input
            type="text"
            placeholder="Search action, actor, entity, correlation ID..."
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
                <th>ID</th>
                <th>Timestamp</th>
                <th>Actor</th>
                <th>Action</th>
                <th>Entity Target</th>
                <th>Outcome</th>
                <th>Correlation ID</th>
                <th>Details / Redaction</th>
              </tr>
            </thead>
            <tbody>
              {loading && auditLogs.length === 0 ? (
                <tr>
                  <td colSpan={8} className="text-center py-4">Loading audit logs...</td>
                </tr>
              ) : filteredLogs.length === 0 ? (
                <tr>
                  <td colSpan={8} className="text-center py-4">No audit records match the current view.</td>
                </tr>
              ) : (
                filteredLogs.map((log) => (
                  <tr key={log.id}>
                    <td className="font-mono text-xs">#{log.id}</td>
                    <td className="text-xs text-muted">
                      {new Date(log.timestamp).toLocaleString()}
                    </td>
                    <td className="font-semibold">
                      <User size={13} className="inline mr-1" />
                      {log.actorUsername || `User #${log.actorId}`}
                    </td>
                    <td>
                      <span className="badge-pill badge-primary font-mono text-xs">
                        {log.action}
                      </span>
                    </td>
                    <td className="text-xs">
                      {log.entityType ? (
                        <span>
                          {log.entityType} {log.entityId ? `(#${log.entityId})` : ''}
                        </span>
                      ) : (
                        '-'
                      )}
                    </td>
                    <td>
                      <span className={`status-tag status-${log.outcome?.toLowerCase() || 'success'}`}>
                        {log.outcome || 'SUCCESS'}
                      </span>
                    </td>
                    <td className="font-mono text-xs text-muted">
                      {log.correlationId || '-'}
                    </td>
                    <td className="text-xs text-secondary max-w-xs truncate" title={log.details}>
                      {log.details || '-'}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {showCompModal && (
        <div className="modal-backdrop">
          <div className="modal-dialog">
            <div className="modal-header">
              <h3>Post Compensating Financial Ledger Entry</h3>
              <button className="close-btn" onClick={() => setShowCompModal(false)}>✕</button>
            </div>
            <form onSubmit={handlePostCompensation} className="modal-body">
              <p className="text-sm text-muted mb-3">
                Financial history cannot be erased or modified. Create an explicit, auditable compensating adjustment.
              </p>
              <div className="form-group">
                <label>User ID</label>
                <input
                  type="number"
                  required
                  value={compUserId}
                  onChange={(e) => setCompUserId(e.target.value)}
                  placeholder="e.g. 1"
                />
              </div>
              <div className="form-group">
                <label>Adjustment Type</label>
                <select
                  value={compIsCredit ? 'CREDIT' : 'DEBIT'}
                  onChange={(e) => setCompIsCredit(e.target.value === 'CREDIT')}
                >
                  <option value="CREDIT">Compensating Credit (+)</option>
                  <option value="DEBIT">Compensating Debit (-)</option>
                </select>
              </div>
              <div className="form-group">
                <label>Amount ($)</label>
                <input
                  type="number"
                  step="0.01"
                  required
                  value={compAmount}
                  onChange={(e) => setCompAmount(e.target.value)}
                  placeholder="e.g. 150.00"
                />
              </div>
              <div className="form-group">
                <label>Original Transaction ID (Optional)</label>
                <input
                  type="text"
                  value={compOrigTxId}
                  onChange={(e) => setCompOrigTxId(e.target.value)}
                  placeholder="e.g. WTX-123 or ORD-45"
                />
              </div>
              <div className="form-group">
                <label>Audit Justification / Reason</label>
                <textarea
                  required
                  value={compReason}
                  onChange={(e) => setCompReason(e.target.value)}
                  placeholder="State the audit finding or reconciliation reason..."
                  rows={3}
                />
              </div>
              <div className="modal-footer">
                <button type="button" className="btn-secondary" onClick={() => setShowCompModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn-primary" disabled={compSubmitting}>
                  {compSubmitting ? 'Recording...' : 'Commit Compensating Entry'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default AuditTrail;
