-- =============================================
-- TradeX Phase 1: Database Foundation Migration
-- =============================================

-- 1. USER PROFILES
CREATE TABLE IF NOT EXISTS user_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone_number VARCHAR(30),
    address TEXT,
    timezone VARCHAR(50) DEFAULT 'UTC',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_profile_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_user_profiles_user_id ON user_profiles(user_id);

-- Seed profiles for initial users
INSERT INTO user_profiles (user_id, first_name, last_name)
SELECT id, 'Admin', 'System' FROM users WHERE username = 'admin'
ON CONFLICT (user_id) DO NOTHING;

-- 2. COMPANIES
CREATE TABLE IF NOT EXISTS companies (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    sector VARCHAR(50),
    industry VARCHAR(100),
    website VARCHAR(255),
    country VARCHAR(50) DEFAULT 'USA',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_companies_symbol ON companies(symbol);

-- Populate companies from stocks table
INSERT INTO companies (symbol, name, sector)
SELECT symbol, company_name, sector FROM stocks
ON CONFLICT (symbol) DO NOTHING;

-- 3. INSTRUMENTS
CREATE TABLE IF NOT EXISTS instruments (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(150) NOT NULL,
    type VARCHAR(20) DEFAULT 'EQUITY',
    exchange VARCHAR(50) DEFAULT 'TRADEX',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_instruments_symbol ON instruments(symbol);

INSERT INTO instruments (symbol, name)
SELECT symbol, company_name FROM stocks
ON CONFLICT (symbol) DO NOTHING;

-- 4. MARKET PRICES HISTORY
CREATE TABLE IF NOT EXISTS market_prices (
    id BIGSERIAL PRIMARY KEY,
    stock_id BIGINT NOT NULL REFERENCES stocks(id) ON DELETE CASCADE,
    symbol VARCHAR(20) NOT NULL,
    price DECIMAL(15,4) NOT NULL,
    change_amount DECIMAL(15,4),
    change_percent DECIMAL(10,4),
    day_high DECIMAL(15,4),
    day_low DECIMAL(15,4),
    volume BIGINT DEFAULT 0,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_market_prices_sym_time ON market_prices(symbol, timestamp DESC);

-- 5. MARKET CANDLES (OHLCV)
CREATE TABLE IF NOT EXISTS market_candles (
    id BIGSERIAL PRIMARY KEY,
    stock_id BIGINT NOT NULL REFERENCES stocks(id) ON DELETE CASCADE,
    symbol VARCHAR(20) NOT NULL,
    timeframe VARCHAR(10) NOT NULL, -- 1M, 5M, 1H, 1D
    open DECIMAL(15,4) NOT NULL,
    high DECIMAL(15,4) NOT NULL,
    low DECIMAL(15,4) NOT NULL,
    close DECIMAL(15,4) NOT NULL,
    volume BIGINT NOT NULL,
    bucket_start TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_candle_bucket UNIQUE (stock_id, timeframe, bucket_start)
);

CREATE INDEX IF NOT EXISTS idx_candles_timeframe ON market_candles(symbol, timeframe, bucket_start DESC);

-- 6. ORDERS TABLE ENHANCEMENTS
ALTER TABLE orders ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(64) UNIQUE;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS vector_clock VARCHAR(128);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS node_id VARCHAR(32);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS leader_epoch BIGINT DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS filled_quantity DECIMAL(15,4) DEFAULT 0.0000;

-- Add check constraint for orders status if not present
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_orders_status') THEN
        ALTER TABLE orders ADD CONSTRAINT chk_orders_status 
        CHECK (status IN ('PENDING', 'PARTIALLY_FILLED', 'EXECUTED', 'CANCELLED', 'REJECTED'));
    END IF;
END $$;

-- 7. ORDER EVENTS
CREATE TABLE IF NOT EXISTS order_events (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    event_type VARCHAR(30) NOT NULL, -- CREATED, MATCHED, FILLED, CANCELLED, REJECTED
    payload TEXT,
    leader_epoch BIGINT,
    node_id VARCHAR(32),
    lamport_timestamp BIGINT,
    vector_clock VARCHAR(128),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_order_events_order_id ON order_events(order_id);

-- 8. TRADES ENHANCEMENTS
ALTER TABLE trades ADD COLUMN IF NOT EXISTS buyer_id BIGINT REFERENCES users(id);
ALTER TABLE trades ADD COLUMN IF NOT EXISTS seller_id BIGINT REFERENCES users(id);
ALTER TABLE trades ADD COLUMN IF NOT EXISTS leader_epoch BIGINT DEFAULT 0;
ALTER TABLE trades ADD COLUMN IF NOT EXISTS execution_node_id VARCHAR(32);

-- 9. WALLETS CONSTRAINTS
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_wallet_available') THEN
        ALTER TABLE wallets ADD CONSTRAINT chk_wallet_available CHECK (available_balance >= 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_wallet_reserved') THEN
        ALTER TABLE wallets ADD CONSTRAINT chk_wallet_reserved CHECK (reserved_balance >= 0);
    END IF;
END $$;

-- 10. WALLET TRANSACTIONS ENHANCEMENTS
ALTER TABLE wallet_transactions ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(64) UNIQUE;
ALTER TABLE wallet_transactions ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'COMPLETED';

-- 11. NOTIFICATIONS
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(100) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(30) DEFAULT 'INFO',
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_read ON notifications(user_id, is_read);

-- 12. AUDIT LOGS
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    action VARCHAR(100) NOT NULL,
    resource VARCHAR(100),
    details TEXT,
    ip_address VARCHAR(45),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_user_id ON audit_logs(user_id);

-- 13. DISTRIBUTED NODES
CREATE TABLE IF NOT EXISTS distributed_nodes (
    id BIGSERIAL PRIMARY KEY,
    node_name VARCHAR(50) UNIQUE NOT NULL,
    host VARCHAR(100) NOT NULL,
    port INT NOT NULL,
    rmi_port INT,
    priority INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_heartbeat TIMESTAMP WITH TIME ZONE,
    is_leader BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Seed default cluster nodes
INSERT INTO distributed_nodes (node_name, host, port, rmi_port, priority) VALUES
('node1', 'node1', 8080, 1099, 1),
('node2', 'node2', 8080, 1100, 2),
('node3', 'node3', 8080, 1101, 3)
ON CONFLICT (node_name) DO NOTHING;

-- 14. DISTRIBUTED EVENTS
CREATE TABLE IF NOT EXISTS distributed_events (
    id BIGSERIAL PRIMARY KEY,
    node_id VARCHAR(50) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    lamport_timestamp BIGINT NOT NULL,
    vector_clock VARCHAR(128),
    source_node VARCHAR(50),
    dest_node VARCHAR(50),
    received_timestamp BIGINT,
    description TEXT,
    wall_clock_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_dist_events_time ON distributed_events(wall_clock_time DESC);

-- 15. REPLICATION LOG
CREATE TABLE IF NOT EXISTS replication_log (
    id BIGSERIAL PRIMARY KEY,
    leader_node VARCHAR(50) NOT NULL,
    leader_epoch BIGINT NOT NULL,
    sequence_number BIGINT NOT NULL,
    entry_type VARCHAR(50) NOT NULL,
    payload TEXT NOT NULL,
    term BIGINT NOT NULL,
    acknowledged_nodes TEXT,
    committed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_replication_seq UNIQUE(leader_epoch, sequence_number)
);

-- 16. IDEMPOTENCY RECORDS
CREATE TABLE IF NOT EXISTS idempotency_records (
    id BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(100) UNIQUE NOT NULL,
    request_path VARCHAR(255) NOT NULL,
    response_body TEXT,
    status_code INT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_idempotency_key ON idempotency_records(idempotency_key);

-- 17. LEADER LEASE & EPOCH FENCING
CREATE TABLE IF NOT EXISTS leader_lease (
    id INT PRIMARY KEY DEFAULT 1,
    leader_name VARCHAR(50) NOT NULL,
    epoch BIGINT NOT NULL DEFAULT 1,
    lease_expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_single_lease CHECK (id = 1)
);

INSERT INTO leader_lease (id, leader_name, epoch, lease_expires_at)
VALUES (1, 'node3', 1, CURRENT_TIMESTAMP + INTERVAL '1 hour')
ON CONFLICT (id) DO NOTHING;
