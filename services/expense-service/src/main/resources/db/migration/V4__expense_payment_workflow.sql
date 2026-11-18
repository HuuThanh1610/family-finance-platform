ALTER TABLE expenses DROP CONSTRAINT chk_expenses_status;
ALTER TABLE expenses ADD CONSTRAINT chk_expenses_status CHECK (
    status IN (
        'DRAFT',
        'RECORDED',
        'SUBMITTED_FOR_PAYMENT',
        'PENDING_WALLET',
        'PAID',
        'FAILED',
        'VOID'
    )
);

ALTER TABLE expenses ADD COLUMN IF NOT EXISTS wallet_id UUID;
ALTER TABLE expenses ADD COLUMN IF NOT EXISTS payment_reference VARCHAR(128);

CREATE TABLE IF NOT EXISTS outbox_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type      VARCHAR(128) NOT NULL,
    aggregate_type  VARCHAR(64) NOT NULL,
    aggregate_id    UUID NOT NULL,
    payload         JSONB NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    published_at    TIMESTAMPTZ,
    status          VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    CONSTRAINT chk_expense_outbox_status CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_expense_outbox_status ON outbox_events (status) WHERE status = 'PENDING';
