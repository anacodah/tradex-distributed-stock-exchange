-- =============================================
-- TradeX Phase 3: Market Data Enhancement
-- =============================================

-- 1. Add data_source label to stocks table (simulated vs live)
ALTER TABLE stocks ADD COLUMN IF NOT EXISTS data_source VARCHAR(20) DEFAULT 'SIMULATED';
ALTER TABLE stocks ADD COLUMN IF NOT EXISTS exchange VARCHAR(20) DEFAULT 'TRADEX';
ALTER TABLE stocks ADD COLUMN IF NOT EXISTS currency VARCHAR(5) DEFAULT 'USD';
ALTER TABLE stocks ADD COLUMN IF NOT EXISTS price_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;

-- Update existing seeded stocks to explicitly mark as simulated
UPDATE stocks SET data_source = 'SIMULATED', exchange = 'TRADEX', currency = 'USD';

-- 2. Add data_source to market_prices
ALTER TABLE market_prices ADD COLUMN IF NOT EXISTS data_source VARCHAR(20) DEFAULT 'SIMULATED';

-- 3. Add data_source to market_candles
ALTER TABLE market_candles ADD COLUMN IF NOT EXISTS data_source VARCHAR(20) DEFAULT 'SIMULATED';

-- 4. Enrich companies table with more metadata
ALTER TABLE companies ADD COLUMN IF NOT EXISTS exchange VARCHAR(20) DEFAULT 'TRADEX';
ALTER TABLE companies ADD COLUMN IF NOT EXISTS currency VARCHAR(5) DEFAULT 'USD';
ALTER TABLE companies ADD COLUMN IF NOT EXISTS market_cap DECIMAL(22, 2);
ALTER TABLE companies ADD COLUMN IF NOT EXISTS employees INT;

-- Enrich existing company records with descriptions and industries
UPDATE companies SET
    description = 'Apple Inc. designs, manufactures, and markets smartphones, personal computers, tablets, wearables, and accessories worldwide.',
    industry = 'Consumer Electronics',
    website = 'https://www.apple.com',
    exchange = 'NASDAQ'
WHERE symbol = 'AAPL';

UPDATE companies SET
    description = 'Microsoft Corporation develops, licenses, and supports software, services, devices, and solutions worldwide.',
    industry = 'Software Infrastructure',
    website = 'https://www.microsoft.com',
    exchange = 'NASDAQ'
WHERE symbol = 'MSFT';

UPDATE companies SET
    description = 'Alphabet Inc. provides internet-related services and products globally including a variety of internet services and products.',
    industry = 'Internet Content & Information',
    website = 'https://www.abc.xyz',
    exchange = 'NASDAQ'
WHERE symbol = 'GOOGL';

UPDATE companies SET
    description = 'Amazon.com Inc. engages in the retail sale of consumer products and subscriptions in North America and internationally.',
    industry = 'Internet Retail',
    website = 'https://www.amazon.com',
    exchange = 'NASDAQ'
WHERE symbol = 'AMZN';

UPDATE companies SET
    description = 'Tesla Inc. designs, develops, manufactures, leases, and sells electric vehicles, energy generation and storage systems worldwide.',
    industry = 'Auto Manufacturers',
    website = 'https://www.tesla.com',
    exchange = 'NASDAQ'
WHERE symbol = 'TSLA';

UPDATE companies SET
    description = 'NVIDIA Corporation provides graphics, and compute and networking solutions worldwide.',
    industry = 'Semiconductors',
    website = 'https://www.nvidia.com',
    exchange = 'NASDAQ'
WHERE symbol = 'NVDA';

UPDATE companies SET
    description = 'Meta Platforms Inc. develops products that enable people to connect and share with friends and family through social networking.',
    industry = 'Internet Content & Information',
    website = 'https://www.meta.com',
    exchange = 'NASDAQ'
WHERE symbol = 'META';

UPDATE companies SET
    description = 'Netflix Inc. provides entertainment services, offering streaming services in multiple languages and countries.',
    industry = 'Entertainment',
    website = 'https://www.netflix.com',
    exchange = 'NASDAQ'
WHERE symbol = 'NFLX';

UPDATE companies SET
    description = 'JPMorgan Chase & Co. operates as a financial services company worldwide providing banking, asset management, and financial services.',
    industry = 'Banks—Diversified',
    website = 'https://www.jpmorganchase.com',
    exchange = 'NYSE'
WHERE symbol = 'JPM';

UPDATE companies SET
    description = 'Visa Inc. operates as a payments technology company worldwide facilitating digital payments among consumers, merchants, financial institutions, and government entities.',
    industry = 'Credit Services',
    website = 'https://www.visa.com',
    exchange = 'NYSE'
WHERE symbol = 'V';

-- 5. Seed initial daily candles for all stocks (today's session so far)
-- This gives the frontend real historical data from day 1
INSERT INTO market_candles (stock_id, symbol, timeframe, open, high, low, close, volume, bucket_start, data_source)
SELECT
    s.id,
    s.symbol,
    '1D',
    s.open_price,
    s.high_price,
    s.low_price,
    s.current_price,
    s.volume,
    DATE_TRUNC('day', NOW() AT TIME ZONE 'America/New_York'),
    'SIMULATED'
FROM stocks s
ON CONFLICT (stock_id, timeframe, bucket_start) DO NOTHING;

-- 6. Seed initial 1H candles for today (simulate several intraday hours)
-- AAPL
INSERT INTO market_candles (stock_id, symbol, timeframe, open, high, low, close, volume, bucket_start, data_source)
SELECT s.id, s.symbol, '1H',
    s.open_price * 1.000,
    s.open_price * 1.003,
    s.open_price * 0.998,
    s.open_price * 1.001,
    s.volume / 8,
    DATE_TRUNC('day', NOW() AT TIME ZONE 'America/New_York') + INTERVAL '9 hours 30 minutes',
    'SIMULATED'
FROM stocks s
ON CONFLICT (stock_id, timeframe, bucket_start) DO NOTHING;

INSERT INTO market_candles (stock_id, symbol, timeframe, open, high, low, close, volume, bucket_start, data_source)
SELECT s.id, s.symbol, '1H',
    s.open_price * 1.001,
    s.open_price * 1.005,
    s.open_price * 0.999,
    s.open_price * 1.003,
    s.volume / 7,
    DATE_TRUNC('day', NOW() AT TIME ZONE 'America/New_York') + INTERVAL '10 hours 30 minutes',
    'SIMULATED'
FROM stocks s
ON CONFLICT (stock_id, timeframe, bucket_start) DO NOTHING;

INSERT INTO market_candles (stock_id, symbol, timeframe, open, high, low, close, volume, bucket_start, data_source)
SELECT s.id, s.symbol, '1H',
    s.open_price * 1.003,
    s.open_price * 1.008,
    s.open_price * 1.001,
    s.open_price * 1.006,
    s.volume / 6,
    DATE_TRUNC('day', NOW() AT TIME ZONE 'America/New_York') + INTERVAL '11 hours 30 minutes',
    'SIMULATED'
FROM stocks s
ON CONFLICT (stock_id, timeframe, bucket_start) DO NOTHING;

INSERT INTO market_candles (stock_id, symbol, timeframe, open, high, low, close, volume, bucket_start, data_source)
SELECT s.id, s.symbol, '1H',
    s.open_price * 1.006,
    s.current_price * 1.002,
    s.open_price * 1.004,
    s.current_price,
    s.volume / 5,
    DATE_TRUNC('day', NOW() AT TIME ZONE 'America/New_York') + INTERVAL '12 hours 30 minutes',
    'SIMULATED'
FROM stocks s
ON CONFLICT (stock_id, timeframe, bucket_start) DO NOTHING;

-- 7. Seed initial price snapshot (latest)
INSERT INTO market_prices (stock_id, symbol, price, change_amount, change_percent, day_high, day_low, volume, timestamp, data_source)
SELECT
    s.id,
    s.symbol,
    s.current_price,
    s.current_price - s.previous_close,
    ROUND(((s.current_price - s.previous_close) / s.previous_close) * 100, 4),
    s.high_price,
    s.low_price,
    s.volume,
    NOW(),
    'SIMULATED'
FROM stocks s;
