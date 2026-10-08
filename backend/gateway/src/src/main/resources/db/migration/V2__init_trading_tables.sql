-- =============================================
-- STOCKS
-- =============================================
CREATE TABLE stocks (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(10) UNIQUE NOT NULL,
    company_name VARCHAR(100) NOT NULL,
    current_price DECIMAL(15,4) NOT NULL,
    open_price DECIMAL(15,4) NOT NULL,
    high_price DECIMAL(15,4) NOT NULL,
    low_price DECIMAL(15,4) NOT NULL,
    previous_close DECIMAL(15,4) NOT NULL,
    volume BIGINT NOT NULL DEFAULT 0,
    market_cap DECIMAL(20,2),
    sector VARCHAR(50),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_stocks_symbol ON stocks(symbol);

-- =============================================
-- SEED STOCK DATA
-- =============================================
INSERT INTO stocks (symbol, company_name, current_price, open_price, high_price, low_price, previous_close, volume, market_cap, sector) VALUES
('AAPL',  'Apple Inc.',                 189.30, 188.50, 191.20, 187.80, 188.50,  52341200, 2940000000000.00, 'Technology'),
('MSFT',  'Microsoft Corporation',      415.20, 412.10, 417.80, 410.50, 412.10,  21876300, 3090000000000.00, 'Technology'),
('GOOGL', 'Alphabet Inc.',              178.45, 176.20, 180.10, 175.90, 176.20,  18932100, 2210000000000.00, 'Technology'),
('AMZN',  'Amazon.com Inc.',            195.80, 193.40, 197.50, 192.80, 193.40,  35421800, 2040000000000.00, 'Consumer Cyclical'),
('TSLA',  'Tesla Inc.',                 248.50, 245.20, 252.40, 244.10, 245.20,  89234500, 791000000000.00,  'Automotive'),
('NVDA',  'NVIDIA Corporation',         875.40, 868.20, 882.10, 865.30, 868.20,  42187600, 2160000000000.00, 'Technology'),
('META',  'Meta Platforms Inc.',        524.30, 520.10, 528.90, 518.40, 520.10,  15678900, 1330000000000.00, 'Technology'),
('NFLX',  'Netflix Inc.',               680.20, 675.40, 685.70, 672.30, 675.40,   5432100, 294000000000.00,  'Communication Services'),
('JPM',   'JPMorgan Chase & Co.',       224.50, 222.30, 226.80, 221.50, 222.30,  12345600, 649000000000.00,  'Financial Services'),
('V',     'Visa Inc.',                  275.80, 273.20, 278.40, 272.10, 273.20,   8976500, 570000000000.00,  'Financial Services');

-- =============================================
-- WALLETS
-- =============================================
CREATE TABLE wallets (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL,
    balance DECIMAL(15,4) NOT NULL DEFAULT 0.0000,
    available_balance DECIMAL(15,4) NOT NULL DEFAULT 0.0000,
    reserved_balance DECIMAL(15,4) NOT NULL DEFAULT 0.0000,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wallet_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_wallets_user_id ON wallets(user_id);

-- =============================================
-- WALLET TRANSACTIONS
-- =============================================
CREATE TABLE wallet_transactions (
    id BIGSERIAL PRIMARY KEY,
    wallet_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL, -- DEPOSIT, WITHDRAWAL, TRADE_DEBIT, TRADE_CREDIT
    amount DECIMAL(15,4) NOT NULL,
    balance_before DECIMAL(15,4) NOT NULL,
    balance_after DECIMAL(15,4) NOT NULL,
    description VARCHAR(255),
    reference_id BIGINT, -- order_id or null
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tx_wallet FOREIGN KEY (wallet_id) REFERENCES wallets (id) ON DELETE CASCADE
);

CREATE INDEX idx_wallet_tx_wallet_id ON wallet_transactions(wallet_id);
CREATE INDEX idx_wallet_tx_created_at ON wallet_transactions(created_at DESC);

-- =============================================
-- HOLDINGS (PORTFOLIO)
-- =============================================
CREATE TABLE holdings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    stock_id BIGINT NOT NULL,
    quantity DECIMAL(15,4) NOT NULL DEFAULT 0.0000,
    average_price DECIMAL(15,4) NOT NULL DEFAULT 0.0000,
    realized_pnl DECIMAL(15,4) NOT NULL DEFAULT 0.0000,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, stock_id),
    CONSTRAINT fk_holding_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_holding_stock FOREIGN KEY (stock_id) REFERENCES stocks (id)
);

CREATE INDEX idx_holdings_user_id ON holdings(user_id);

-- =============================================
-- ORDERS
-- =============================================
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    stock_id BIGINT NOT NULL,
    side VARCHAR(10) NOT NULL,        -- BUY, SELL
    order_type VARCHAR(15) NOT NULL,  -- MARKET, LIMIT, STOP_LOSS
    quantity DECIMAL(15,4) NOT NULL,
    price DECIMAL(15,4),              -- null for MARKET orders
    stop_price DECIMAL(15,4),         -- for STOP_LOSS
    execution_price DECIMAL(15,4),
    status VARCHAR(15) NOT NULL DEFAULT 'PENDING', -- PENDING, EXECUTED, CANCELLED, REJECTED
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_stock FOREIGN KEY (stock_id) REFERENCES stocks (id)
);

CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at DESC);

-- =============================================
-- TRADES
-- =============================================
CREATE TABLE trades (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    stock_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    side VARCHAR(10) NOT NULL,       -- BUY, SELL
    quantity DECIMAL(15,4) NOT NULL,
    execution_price DECIMAL(15,4) NOT NULL,
    total_value DECIMAL(15,4) NOT NULL,
    executed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_trade_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_trade_stock FOREIGN KEY (stock_id) REFERENCES stocks (id),
    CONSTRAINT fk_trade_order FOREIGN KEY (order_id) REFERENCES orders (id)
);

CREATE INDEX idx_trades_user_id ON trades(user_id);
CREATE INDEX idx_trades_executed_at ON trades(executed_at DESC);
