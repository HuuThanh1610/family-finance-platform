CREATE TABLE payment_refunds (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_order_id    UUID NOT NULL REFERENCES payment_orders (id) ON DELETE RESTRICT,
    amount              BIGINT NOT NULL,
    reason              VARCHAR(500),
    status              VARCHAR(32) NOT NULL DEFAULT 'REQUESTED',
    provider_refund_id  VARCHAR(200),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT chk_payment_refunds_amount CHECK (amount > 0),
    CONSTRAINT chk_payment_refunds_status CHECK (
        status IN ('REQUESTED', 'PROCESSING', 'COMPLETED', 'FAILED', 'REJECTED')
    )
);

CREATE INDEX idx_payment_refunds_order_id ON payment_refunds (payment_order_id);

CREATE TABLE payment_reconciliations (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_code       VARCHAR(64) NOT NULL,
    reconciliation_date DATE NOT NULL,
    total_matched       BIGINT NOT NULL DEFAULT 0,
    total_unmatched       BIGINT NOT NULL DEFAULT 0,
    status              VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    notes               TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT uq_payment_reconciliations_provider_date UNIQUE (provider_code, reconciliation_date),
    CONSTRAINT chk_payment_reconciliations_status CHECK (status IN ('OPEN', 'CLOSED', 'DISPUTED'))
);