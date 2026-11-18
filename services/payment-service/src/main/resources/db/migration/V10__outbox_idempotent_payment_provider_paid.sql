-- At most one PaymentProviderPaid outbox row per payment order (prevents duplicate wallet credit).
CREATE UNIQUE INDEX IF NOT EXISTS uq_outbox_events_aggregate_event_type
    ON outbox_events (aggregate_id, event_type);