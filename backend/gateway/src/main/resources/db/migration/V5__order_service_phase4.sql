-- =============================================
-- TradeX Phase 4: Order Service & Matching Engine
-- =============================================

-- 1. Drop existing check constraint on orders status to expand with Phase 4 states
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_orders_status') THEN
        ALTER TABLE orders DROP CONSTRAINT chk_orders_status;
    END IF;
END $$;

-- 2. Add expanded status check constraint
ALTER TABLE orders ADD CONSTRAINT chk_orders_status 
CHECK (status IN ('NEW', 'OPEN', 'PARTIALLY_FILLED', 'FILLED', 'EXECUTED', 'CANCEL_PENDING', 'CANCELLED', 'REJECTED', 'EXPIRED'));

-- 3. Add order lifecycle & priority columns to orders
ALTER TABLE orders ADD COLUMN IF NOT EXISTS filled_quantity DECIMAL(15,4) NOT NULL DEFAULT 0.0000;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS reserved_amount DECIMAL(15,4) NOT NULL DEFAULT 0.0000;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS reserved_shares DECIMAL(15,4) NOT NULL DEFAULT 0.0000;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS sequence_number BIGINT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS client_order_id VARCHAR(64);

-- Ensure sequence_number has a sequence if not set
CREATE SEQUENCE IF NOT EXISTS order_seq_num_seq START WITH 1 INCREMENT BY 1;
UPDATE orders SET sequence_number = nextval('order_seq_num_seq') WHERE sequence_number IS NULL;

-- 4. Enhance trades table with trade_id, maker/taker, buyer/seller references
ALTER TABLE trades ADD COLUMN IF NOT EXISTS trade_id VARCHAR(64) UNIQUE;
ALTER TABLE trades ADD COLUMN IF NOT EXISTS maker_order_id BIGINT REFERENCES orders(id) ON DELETE SET NULL;
ALTER TABLE trades ADD COLUMN IF NOT EXISTS taker_order_id BIGINT REFERENCES orders(id) ON DELETE SET NULL;
ALTER TABLE trades ADD COLUMN IF NOT EXISTS buyer_id BIGINT REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE trades ADD COLUMN IF NOT EXISTS seller_id BIGINT REFERENCES users(id) ON DELETE SET NULL;

-- Backfill existing trades with a trade_id
UPDATE trades SET trade_id = 'TRD-' || id || '-' || EXTRACT(EPOCH FROM executed_at)::BIGINT WHERE trade_id IS NULL;

-- 5. Additional indices for matching engine and query performance
CREATE INDEX IF NOT EXISTS idx_orders_stock_status ON orders(stock_id, status);
CREATE INDEX IF NOT EXISTS idx_orders_user_status ON orders(user_id, status);
CREATE INDEX IF NOT EXISTS idx_orders_sequence ON orders(sequence_number);
CREATE INDEX IF NOT EXISTS idx_trades_trade_id ON trades(trade_id);
CREATE INDEX IF NOT EXISTS idx_trades_stock_id ON trades(stock_id);
