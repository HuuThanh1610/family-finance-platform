CREATE TABLE inbox_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id        UUID NOT NULL,
    event_type      VARCHAR(128) NOT NULL,
    aggregate_type  VARCHAR(64) NOT NULL,
    aggregate_id    UUID NOT NULL,
    payload         JSONB NOT NULL,
    received_at     TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    processed_at    TIMESTAMPTZ,
    status          VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    CONSTRAINT uq_expense_inbox_events_event_id UNIQUE (event_id),
    CONSTRAINT chk_expense_inbox_events_status CHECK (status IN ('PENDING', 'PROCESSED', 'FAILED', 'SKIPPED'))
);

CREATE INDEX idx_expense_inbox_events_status ON inbox_events (status) WHERE status = 'PENDING';
