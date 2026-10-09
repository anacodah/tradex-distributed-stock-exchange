import React, { useState, useEffect, useRef } from 'react';
import api from '../api/axios';
import { Bell, Check, ExternalLink, X, RefreshCw, AlertCircle, Info, CheckCircle2, AlertTriangle } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export interface NotificationItem {
  id: number;
  title: string;
  message: string;
  type: string;
  read: boolean;
  orderId?: number;
  tradeId?: string;
  linkUrl?: string;
  createdAt: string;
}

interface NotificationCenterProps {
  onClose?: () => void;
}

export const NotificationCenter: React.FC<NotificationCenterProps> = ({ onClose }) => {
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [unreadCount, setUnreadCount] = useState<number>(0);
  const [connected, setConnected] = useState<boolean>(false);
  const wsRef = useRef<WebSocket | null>(null);
  const navigate = useNavigate();

  const fetchNotifications = async () => {
    try {
      setLoading(true);
      const res = await api.get('/notifications?page=0&size=30');
      setNotifications(res.data.content || []);
      const countRes = await api.get('/notifications/unread-count');
      setUnreadCount(countRes.data.unreadCount || 0);
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
    const wsUrl = `ws://localhost:8080/ws/notifications${token ? `?token=${token}` : ''}`;
    const ws = new WebSocket(wsUrl);
    wsRef.current = ws;

    ws.onopen = () => {
      setConnected(true);
    };

    ws.onmessage = (event) => {
      try {
        const notif: NotificationItem = JSON.parse(event.data);
        setNotifications((prev) => {
          // Deduplicate
          if (prev.some((n) => n.id === notif.id)) return prev;
          return [notif, ...prev];
        });
        setUnreadCount((c) => c + 1);
      } catch (e) {
        console.error('Failed to parse incoming notification:', e);
      }
    };

    ws.onclose = () => {
      setConnected(false);
    };

    return () => {
      if (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING) {
        ws.close();
      }
    };
  }, []);

  const handleMarkAsRead = async (id: number) => {
    try {
      await api.put(`/notifications/${id}/read`);
      setNotifications((prev) =>
        prev.map((n) => (n.id === id ? { ...n, read: true } : n))
      );
      setUnreadCount((c) => Math.max(0, c - 1));
    } catch (err) {
      console.error('Failed to mark notification as read:', err);
    }
  };

  const handleMarkAllAsRead = async () => {
    try {
      await api.put('/notifications/mark-all-read');
      setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
      setUnreadCount(0);
    } catch (err) {
      console.error('Failed to mark all as read:', err);
    }
  };

  const handleNavigate = (notif: NotificationItem) => {
    if (!notif.read) {
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
        return <CheckCircle2 size={16} className="text-success" />;
      case 'ORDER_REJECTED':
      case 'SECURITY_ALERT':
        return <AlertCircle size={16} className="text-danger" />;
      case 'STOP_LOSS_ACTIVATED':
      case 'ORDER_CANCELLED':
        return <AlertTriangle size={16} className="text-warning" />;
      default:
        return <Info size={16} className="text-info" />;
    }
  };

  return (
    <div className="notification-panel">
      <div className="notification-header">
        <div className="title-area">
          <Bell size={18} />
          <h3>Notifications</h3>
          <span className={`ws-badge ${connected ? 'connected' : 'disconnected'}`}>
            {connected ? 'LIVE' : 'POLLING'}
          </span>
          {unreadCount > 0 && <span className="unread-badge">{unreadCount}</span>}
        </div>
        <div className="actions-area">
          {unreadCount > 0 && (
            <button className="mark-all-btn" onClick={handleMarkAllAsRead} title="Mark all read">
              <Check size={14} /> Read all
            </button>
          )}
          <button className="refresh-btn" onClick={fetchNotifications} title="Refresh">
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
          notifications.map((n) => (
            <div
              key={n.id}
              className={`notification-item ${n.read ? 'read' : 'unread'}`}
              onClick={() => handleNavigate(n)}
            >
              <div className="notif-icon">{getIcon(n.type)}</div>
              <div className="notif-body">
                <div className="notif-title-row">
                  <span className="notif-title">{n.title}</span>
                  <span className="notif-time">{new Date(n.createdAt).toLocaleTimeString()}</span>
                </div>
                <div className="notif-message">{n.message}</div>
                <div className="notif-footer">
                  {n.orderId && <span className="order-tag">Order #{n.orderId}</span>}
                  {n.tradeId && <span className="trade-tag">Trade #{n.tradeId}</span>}
                  {n.linkUrl && (
                    <span className="link-tag">
                      View <ExternalLink size={11} />
                    </span>
                  )}
                </div>
              </div>
              {!n.read && (
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
          ))
        )}
      </div>
    </div>
  );
};

export default NotificationCenter;
