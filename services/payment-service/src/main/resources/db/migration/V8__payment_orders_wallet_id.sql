ALTER TABLE payment_orders
    ADD COLUMN IF NOT EXISTS wallet_id UUID;

CREATE INDEX IF NOT EXISTS idx_payment_orders_wallet_id ON payment_orders (wallet_id);

COMMENT ON COLUMN payment_orders.wallet_id IS 'Target wallet for top-up credit after provider success';
