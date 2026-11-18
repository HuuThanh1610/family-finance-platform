CREATE TABLE IF NOT EXISTS outbox_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type      VARCHAR(128) NOT NULL,
    aggregate_type  VARCHAR(64) NOT NULL,
    aggregate_id    UUID NOT NULL,
    payload         JSONB NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    published_at    TIMESTAMPTZ,
    status          VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    CONSTRAINT chk_household_outbox_status CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_household_outbox_status ON outbox_events (status) WHERE status = 'PENDING';

-- Dedup key for low-stock alerts: one pending/published InventoryLow per inventory item until restocked above threshold
ALTER TABLE inventory_items
    ADD COLUMN IF NOT EXISTS low_stock_alerted BOOLEAN NOT NULL DEFAULT FALSE;
