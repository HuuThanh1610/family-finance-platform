CREATE TABLE provider_transactions (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_attempt_id      UUID NOT NULL REFERENCES payment_attempts (id) ON DELETE CASCADE,
    provider_txn_id         VARCHAR(200) NOT NULL,
    provider_code           VARCHAR(64) NOT NULL,
    amount                  BIGINT NOT NULL,
    status                  VARCHAR(32) NOT NULL,
    raw_response            JSONB NOT NULL DEFAULT '{}'::jsonb,
    recorded_at             TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT uq_provider_transactions_provider_txn UNIQUE (provider_code, provider_txn_id),
    CONSTRAINT chk_provider_transactions_amount CHECK (amount > 0)
);

CREATE INDEX idx_provider_transactions_attempt_id ON provider_transactions (payment_attempt_id);