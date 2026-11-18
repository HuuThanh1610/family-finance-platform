CREATE TABLE wallet_balances (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ledger_account_id   UUID NOT NULL REFERENCES ledger_accounts (id) ON DELETE RESTRICT,
    balance_amount      BIGINT NOT NULL DEFAULT 0,
    version             INT NOT NULL DEFAULT 0,
    recalculated_at     TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT uq_wallet_balances_ledger_account UNIQUE (ledger_account_id)
);

CREATE INDEX idx_wallet_balances_ledger_account_id ON wallet_balances (ledger_account_id);

COMMENT ON TABLE wallet_balances IS
'Snapshot số dư theo ledger_account; cập nhật qua domain service (optimistic lock version). Không dùng CRUD trực tiếp để chỉnh số dư.';

CREATE TABLE idempotency_records (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_account_id   UUID NOT NULL REFERENCES wallet_accounts (id) ON DELETE RESTRICT,
    idempotency_key     VARCHAR(128) NOT NULL,
    request_hash        VARCHAR(128),
    response_payload    JSONB,
    status              VARCHAR(32) NOT NULL DEFAULT 'COMPLETED',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    expires_at          TIMESTAMPTZ,
    CONSTRAINT uq_idempotency_records_key UNIQUE (idempotency_key),
    CONSTRAINT chk_idempotency_records_status CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'FAILED'))
);

CREATE INDEX idx_idempotency_records_wallet_id ON idempotency_records (wallet_account_id);