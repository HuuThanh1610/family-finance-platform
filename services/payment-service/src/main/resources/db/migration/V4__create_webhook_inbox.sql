CREATE TABLE webhook_inbox (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_code       VARCHAR(64) NOT NULL,
    idempotency_key     VARCHAR(200) NOT NULL,
    event_type          VARCHAR(128) NOT NULL,
    payload             JSONB NOT NULL,
    received_at         TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    processed_at        TIMESTAMPTZ,
    status              VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    CONSTRAINT uq_webhook_inbox_idempotency UNIQUE (provider_code, idempotency_key),
    CONSTRAINT chk_webhook_inbox_status CHECK (status IN ('PENDING', 'PROCESSED', 'FAILED', 'SKIPPED'))
);

CREATE INDEX idx_webhook_inbox_status ON webhook_inbox (status) WHERE status = 'PENDING';