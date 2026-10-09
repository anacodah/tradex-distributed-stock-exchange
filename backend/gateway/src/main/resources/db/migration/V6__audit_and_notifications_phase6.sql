-- =============================================
-- TradeX Phase 6: Transaction, Audit & Notification Enhancements
-- =============================================

-- 1. Enhance audit_logs with correlation_id, actor, outcome, entity references
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS actor_username VARCHAR(100);
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS correlation_id VARCHAR(64);
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS outcome VARCHAR(30) DEFAULT 'SUCCESS';
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS entity_type VARCHAR(50);
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS entity_id VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_audit_logs_actor ON audit_logs(actor_username);
CREATE INDEX IF NOT EXISTS idx_audit_logs_correlation ON audit_logs(correlation_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_time ON audit_logs(created_at DESC);

-- 2. Enhance notifications with dedup_key, order_id, trade_id, and metadata
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS dedup_key VARCHAR(128) UNIQUE;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS order_id BIGINT;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS trade_id VARCHAR(64);
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS link_url VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_notifications_dedup ON notifications(dedup_key);
CREATE INDEX IF NOT EXISTS idx_notifications_user_time ON notifications(user_id, created_at DESC);

-- 3. Enhance wallet_transactions with correlation_id
ALTER TABLE wallet_transactions ADD COLUMN IF NOT EXISTS correlation_id VARCHAR(64);
CREATE INDEX IF NOT EXISTS idx_wallet_tx_corr ON wallet_transactions(correlation_id);
