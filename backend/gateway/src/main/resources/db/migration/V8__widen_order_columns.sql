-- =============================================
-- TradeX Phase 8 / V8: Widen order status column for PARTIALLY_FILLED (16 chars)
-- =============================================

ALTER TABLE orders ALTER COLUMN status TYPE VARCHAR(32);
ALTER TABLE orders ALTER COLUMN order_type TYPE VARCHAR(32);
ALTER TABLE orders ALTER COLUMN side TYPE VARCHAR(20);
