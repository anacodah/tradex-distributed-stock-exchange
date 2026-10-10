-- =============================================
-- TradeX Phase 7 / V7: Schema Fix & Seed Central Limit Order Book Liquidity
-- =============================================

-- 1. Fix distributed_events table column compatibility
ALTER TABLE distributed_events ADD COLUMN IF NOT EXISTS destination_node VARCHAR(50);
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'distributed_events' AND column_name = 'dest_node'
    ) THEN
        UPDATE distributed_events SET destination_node = dest_node WHERE destination_node IS NULL;
    END IF;
END $$;

-- 2. Create Market Maker system user for central limit order book liquidity
INSERT INTO users (username, email, password_hash, enabled)
VALUES ('marketmaker', 'marketmaker@tradex.com', '$2a$10$wNnQk2lR.Zc18v5nJd8W/.aPBy4vP8V6i8a22QW/c10H1sS9aO1Hq', true)
ON CONFLICT (username) DO NOTHING;

-- Assign ROLE_USER to marketmaker
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'marketmaker' AND r.name = 'ROLE_USER'
ON CONFLICT DO NOTHING;

-- 3. Seed Market Maker wallet with ample capital
INSERT INTO wallets (user_id, balance, available_balance, reserved_balance)
SELECT id, 10000000.0000, 10000000.0000, 0.0000 FROM users WHERE username = 'marketmaker'
ON CONFLICT (user_id) DO UPDATE SET 
    balance = GREATEST(wallets.balance, 10000000.0000),
    available_balance = GREATEST(wallets.available_balance, 10000000.0000);

-- 4. Seed Market Maker holdings with 10,000 shares for each stock
INSERT INTO holdings (user_id, stock_id, quantity, average_price, realized_pnl)
SELECT u.id, s.id, 10000.0000, s.current_price, 0.0000
FROM users u
CROSS JOIN stocks s
WHERE u.username = 'marketmaker'
ON CONFLICT (user_id, stock_id) DO UPDATE SET quantity = GREATEST(holdings.quantity, 10000.0000);

-- 5. Seed initial holdings for student admin accounts (so they can also test selling immediately)
INSERT INTO holdings (user_id, stock_id, quantity, average_price, realized_pnl)
SELECT u.id, s.id, 25.0000, s.current_price, 0.0000
FROM users u
CROSS JOIN stocks s
WHERE u.username IN ('bhargavee.save', 'anoushka.redekar', 'anushree.sawant', 'admin')
ON CONFLICT (user_id, stock_id) DO NOTHING;

-- 6. Seed resting LIMIT SELL (Asks) and LIMIT BUY (Bids) orders in the orders table
-- These provide authentic exchange depth in the central limit order book

-- Asks (Sells) slightly above market price
INSERT INTO orders (user_id, stock_id, side, order_type, quantity, price, status, reserved_shares, reserved_amount, sequence_number, client_order_id)
SELECT 
    u.id, 
    s.id, 
    'SELL', 
    'LIMIT', 
    50.0000, 
    ROUND(s.current_price * 1.002, 2), 
    'OPEN', 
    50.0000, 
    0.0000, 
    nextval('order_seq_num_seq'), 
    'MM-ASK1-' || s.symbol
FROM users u
CROSS JOIN stocks s
WHERE u.username = 'marketmaker'
AND NOT EXISTS (
    SELECT 1 FROM orders o WHERE o.user_id = u.id AND o.stock_id = s.id AND o.client_order_id = 'MM-ASK1-' || s.symbol
);

INSERT INTO orders (user_id, stock_id, side, order_type, quantity, price, status, reserved_shares, reserved_amount, sequence_number, client_order_id)
SELECT 
    u.id, 
    s.id, 
    'SELL', 
    'LIMIT', 
    100.0000, 
    ROUND(s.current_price * 1.006, 2), 
    'OPEN', 
    100.0000, 
    0.0000, 
    nextval('order_seq_num_seq'), 
    'MM-ASK2-' || s.symbol
FROM users u
CROSS JOIN stocks s
WHERE u.username = 'marketmaker'
AND NOT EXISTS (
    SELECT 1 FROM orders o WHERE o.user_id = u.id AND o.stock_id = s.id AND o.client_order_id = 'MM-ASK2-' || s.symbol
);

-- Bids (Buys) slightly below market price
INSERT INTO orders (user_id, stock_id, side, order_type, quantity, price, status, reserved_shares, reserved_amount, sequence_number, client_order_id)
SELECT 
    u.id, 
    s.id, 
    'BUY', 
    'LIMIT', 
    50.0000, 
    ROUND(s.current_price * 0.998, 2), 
    'OPEN', 
    0.0000, 
    ROUND(s.current_price * 0.998 * 50, 4), 
    nextval('order_seq_num_seq'), 
    'MM-BID1-' || s.symbol
FROM users u
CROSS JOIN stocks s
WHERE u.username = 'marketmaker'
AND NOT EXISTS (
    SELECT 1 FROM orders o WHERE o.user_id = u.id AND o.stock_id = s.id AND o.client_order_id = 'MM-BID1-' || s.symbol
);

INSERT INTO orders (user_id, stock_id, side, order_type, quantity, price, status, reserved_shares, reserved_amount, sequence_number, client_order_id)
SELECT 
    u.id, 
    s.id, 
    'BUY', 
    'LIMIT', 
    100.0000, 
    ROUND(s.current_price * 0.994, 2), 
    'OPEN', 
    0.0000, 
    ROUND(s.current_price * 0.994 * 100, 4), 
    nextval('order_seq_num_seq'), 
    'MM-BID2-' || s.symbol
FROM users u
CROSS JOIN stocks s
WHERE u.username = 'marketmaker'
AND NOT EXISTS (
    SELECT 1 FROM orders o WHERE o.user_id = u.id AND o.stock_id = s.id AND o.client_order_id = 'MM-BID2-' || s.symbol
);
