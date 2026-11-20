-- Business-level idempotency for realized income/expense projections.
-- One ledger transaction contributes at most once, even across distinct Kafka event IDs.
CREATE TABLE realized_financial_postings (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id               UUID NOT NULL,
    ledger_transaction_id   UUID NOT NULL,
    reference_id            VARCHAR(128),
    posting_kind            VARCHAR(32) NOT NULL,
    amount                  BIGINT NOT NULL,
    currency_code           VARCHAR(3) NOT NULL DEFAULT 'VND',
    event_id                UUID NOT NULL,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT uq_realized_financial_postings_txn UNIQUE (ledger_transaction_id),
    CONSTRAINT chk_realized_financial_postings_kind CHECK (posting_kind IN ('INCOME', 'EXPENSE')),
    CONSTRAINT chk_realized_financial_postings_amount CHECK (amount > 0)
);

CREATE INDEX idx_realized_financial_postings_family
    ON realized_financial_postings (family_id, created_at DESC);
