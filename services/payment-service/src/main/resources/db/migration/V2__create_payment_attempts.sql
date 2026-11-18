CREATE TABLE payment_attempts (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_order_id    UUID NOT NULL REFERENCES payment_orders (id) ON DELETE CASCADE,
    attempt_no          INT NOT NULL,
    provider_code       VARCHAR(64) NOT NULL,
    status              VARCHAR(32) NOT NULL DEFAULT 'INITIATED',
    failure_reason      VARCHAR(500),
    initiated_at        TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    completed_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT uq_payment_attempts_order_no UNIQUE (payment_order_id, attempt_no),
    CONSTRAINT chk_payment_attempts_status CHECK (
        status IN ('INITIATED', 'PENDING', 'SUCCEEDED', 'FAILED', 'ABANDONED')
    )
);

CREATE INDEX idx_payment_attempts_order_id ON payment_attempts (payment_order_id);