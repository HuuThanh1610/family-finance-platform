-- Concurrent permanent-failure writers for the same expense must not create duplicate outcomes.
CREATE UNIQUE INDEX IF NOT EXISTS uq_wallet_outbox_expense_payment_failed
    ON outbox_events (event_type, aggregate_id)
    WHERE event_type = 'ExpensePaymentFailed';
