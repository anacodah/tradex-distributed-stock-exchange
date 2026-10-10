import React, { useState, useEffect, useRef } from 'react';
import api from '../api/axios';
import { Bell, Check, ExternalLink, X, RefreshCw, AlertCircle, Info, CheckCircle2, AlertTriangle } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export interface NotificationItem {
  id: number;
  userId?: number;
  title: string;
  message: string;
  type: string;
  read?: boolean;
  isRead?: boolean;
  orderId?: number;
  tradeId?: string;
  linkUrl?: string;
  createdAt: string;
}

interface NotificationCenterProps {
  onClose?: () => void;
  onUnreadCountChange?: (count: number) => void;
}

export const NotificationCenter: React.FC<NotificationCenterProps> = ({ onClose, onUnreadCountChange }) => {
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [unreadCount, setUnreadCount] = useState<number>(0);
  const [connected, setConnected] = useState<boolean>(false);
  const wsRef = useRef<WebSocket | null>(null);
  const navigate = useNavigate();

  const fetchNotifications = async () => {
    try {
      setLoading(true);
      const res = await api.get('/notifications?page=0&size=50');
      const list: NotificationItem[] = Array.isArray(res.data) 
        ? res.data 
        : (res.data?.content || []);
      setNotifications(list);

      const countRes = await api.get('/notifications/unread-count');
      const count = countRes.data?.unreadCount ?? 0;
      setUnreadCount(count);
      if (onUnreadCountChange) onUnreadCountChange(count);
    } catch (err) {
      console.error('Failed to load notifications:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchNotifications();

    // Setup WebSocket push
    const token = localStorage.getItem('token');
    const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsHost = window.location.hostname === 'localhost' ? 'localhost:8080' : window.location.host;
    const wsUrl = `${wsProtocol}//${wsHost}/ws/notifications${token ? `?token=${token}` : ''}`;

    try {
      const ws = new WebSocket(wsUrl);
      wsRef.current = ws;

      ws.onopen = () => {
        setConnected(true);
      };

      ws.onmessage = (event) => {
        try {
          const notif: NotificationItem = JSON.parse(event.data);
          setNotifications((prev) => {
            if (prev.some((n) => n.id === notif.id)) return prev;
            return [notif, ...prev];
          });
          setUnreadCount((c) => {
            const next = c + 1;
            if (onUnreadCountChange) onUnreadCountChange(next);
            return next;
          });
        } catch (e) {
          console.error('Failed to parse incoming notification:', e);
        }
      };

      ws.onclose = () => {
        setConnected(false);
      };
    } catch (e) {
      console.warn('WebSocket notifications unavailable, fallback to HTTP polling.');
    }

    return () => {
      if (wsRef.current && (wsRef.current.readyState === WebSocket.OPEN || wsRef.current.readyState === WebSocket.CONNECTING)) {
        wsRef.current.close();
      }
    };
  }, []);

  const handleMarkAsRead = async (id: number) => {
    try {
      await api.put(`/notifications/${id}/read`);
      setNotifications((prev) =>
        prev.map((n) => (n.id === id ? { ...n, read: true, isRead: true } : n))
      );
      setUnreadCount((c) => {
        const next = Math.max(0, c - 1);
        if (onUnreadCountChange) onUnreadCountChange(next);
        return next;
      });
    } catch (err) {
      console.error('Failed to mark notification as read:', err);
    }
  };

  const handleMarkAllAsRead = async () => {
    try {
      await api.put('/notifications/read-all');
      setNotifications((prev) => prev.map((n) => ({ ...n, read: true, isRead: true })));
      setUnreadCount(0);
      if (onUnreadCountChange) onUnreadCountChange(0);
    } catch (err) {
      console.error('Failed to mark all as read:', err);
    }
  };

  const handleNavigate = (notif: NotificationItem) => {
    const isRead = notif.isRead !== undefined ? notif.isRead : (notif.read ?? false);
    if (!isRead) {
      handleMarkAsRead(notif.id);
    }
    if (notif.linkUrl) {
      navigate(notif.linkUrl);
      if (onClose) onClose();
    }
  };

  const getIcon = (type: string) => {
    switch (type) {
      case 'ORDER_FILLED':
      case 'TRADE_EXECUTED':
        return <CheckCircle2 size={16} color="#34d399" />;
      case 'ORDER_REJECTED':
      case 'SECURITY_ALERT':
        return <AlertCircle size={16} color="#f87171" />;
      case 'STOP_LOSS_ACTIVATED':
      case 'ORDER_CANCELLED':
        return <AlertTriangle size={16} color="#fbbf24" />;
      default:
        return <Info size={16} color="#60a5fa" />;
    }
  };

  const formatTime = (isoString: string) => {
    try {
      const date = new Date(isoString);
      const now = new Date();
      const diffMs = now.getTime() - date.getTime();
      const diffMins = Math.floor(diffMs / 60000);
      if (diffMins < 1) return 'Just now';
      if (diffMins < 60) return `${diffMins}m ago`;
      const diffHours = Math.floor(diffMins / 60);
      if (diffHours < 24) return `${diffHours}h ago`;
      return date.toLocaleDateString([], { month: 'short', day: 'numeric' });
    } catch (e) {
      return '';
    }
  };

  return (
    <div className="notification-panel">
      <div className="notification-header">
        <div className="title-area">
          <Bell size={18} color="#38bdf8" />
          <h3>Notifications</h3>
          <span className={`ws-badge ${connected ? 'connected' : 'disconnected'}`}>
            {connected ? '● LIVE' : '○ POLLING'}
          </span>
          {unreadCount > 0 && <span className="unread-badge">{unreadCount}</span>}
        </div>
        <div className="actions-area">
          {unreadCount > 0 && (
            <button className="mark-all-btn" onClick={handleMarkAllAsRead} title="Mark all as read">
              <Check size={14} /> Read all
            </button>
          )}
          <button className="refresh-btn" onClick={fetchNotifications} title="Refresh notifications">
            <RefreshCw size={14} className={loading ? 'spinning' : ''} />
          </button>
          {onClose && (
            <button className="close-btn" onClick={onClose} title="Close">
              <X size={16} />
            </button>
          )}
        </div>
      </div>

      <div className="notification-list">
        {loading && notifications.length === 0 ? (
          <div className="loading-state">Loading notifications...</div>
        ) : notifications.length === 0 ? (
          <div className="empty-state">No notifications yet.</div>
        ) : (
          notifications.map((n) => {
            const isRead = n.isRead !== undefined ? n.isRead : (n.read ?? false);
            return (
              <div
                key={n.id}
                className={`notification-item ${isRead ? 'read' : 'unread'}`}
                onClick={() => handleNavigate(n)}
              >
                <div className="notif-icon">{getIcon(n.type)}</div>
                <div className="notif-body">
                  <div className="notif-title-row">
                    <span className="notif-title">{n.title}</span>
                    <span className="notif-time">{formatTime(n.createdAt)}</span>
                  </div>
                  <div className="notif-message">{n.message}</div>
                  <div className="notif-footer">
                    {n.orderId && <span className="order-tag">Order #{n.orderId}</span>}
                    {n.tradeId && <span className="trade-tag">Trade #{n.tradeId.substring(0, 12)}</span>}
                    {n.linkUrl && (
                      <span className="link-tag">
                        View <ExternalLink size={11} />
                      </span>
                    )}
                  </div>
                </div>
                {!isRead && (
                  <button
                    className="read-dot-btn"
                    onClick={(e) => {
                      e.stopPropagation();
                      handleMarkAsRead(n.id);
                    }}
                    title="Mark as read"
                  >
                    <span className="unread-dot" />
                  </button>
                )}
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};

export default NotificationCenter;
