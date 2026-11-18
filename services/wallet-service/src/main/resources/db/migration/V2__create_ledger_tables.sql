CREATE TABLE ledger_accounts (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_account_id UUID NOT NULL REFERENCES wallet_accounts (id) ON DELETE RESTRICT,
    account_code      VARCHAR(64) NOT NULL,
    name              VARCHAR(200) NOT NULL,
    account_kind      VARCHAR(32) NOT NULL,
    normal_balance    VARCHAR(6) NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT uq_ledger_accounts_wallet_code UNIQUE (wallet_account_id, account_code),
    CONSTRAINT chk_ledger_accounts_kind CHECK (
        account_kind IN ('ASSET', 'LIABILITY', 'EQUITY', 'REVENUE', 'EXPENSE')
    ),
    CONSTRAINT chk_ledger_accounts_normal CHECK (normal_balance IN ('DEBIT', 'CREDIT'))
);

CREATE INDEX idx_ledger_accounts_wallet_id ON ledger_accounts (wallet_account_id);

CREATE TABLE ledger_transactions (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_account_id UUID NOT NULL REFERENCES wallet_accounts (id) ON DELETE RESTRICT,
    reference_id      VARCHAR(128) NOT NULL,
    idempotency_key   VARCHAR(128) NOT NULL,
    description       VARCHAR(500),
    transaction_type  VARCHAR(64) NOT NULL,
    posted_at         TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT uq_ledger_transactions_reference UNIQUE (reference_id),
    CONSTRAINT uq_ledger_transactions_idempotency UNIQUE (idempotency_key)
);

CREATE INDEX idx_ledger_transactions_wallet_id ON ledger_transactions (wallet_account_id);
CREATE INDEX idx_ledger_transactions_reference_id ON ledger_transactions (reference_id);
CREATE INDEX idx_ledger_transactions_posted_at ON ledger_transactions (posted_at DESC);

CREATE TABLE ledger_entries (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id      UUID NOT NULL REFERENCES ledger_transactions (id) ON DELETE RESTRICT,
    ledger_account_id   UUID NOT NULL REFERENCES ledger_accounts (id) ON DELETE RESTRICT,
    entry_type          VARCHAR(6) NOT NULL,
    amount              BIGINT NOT NULL,
    memo                VARCHAR(255),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT chk_ledger_entries_type CHECK (entry_type IN ('DEBIT', 'CREDIT')),
    CONSTRAINT chk_ledger_entries_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_ledger_entries_transaction_id ON ledger_entries (transaction_id);
CREATE INDEX idx_ledger_entries_ledger_account_id ON ledger_entries (ledger_account_id);

COMMENT ON TABLE ledger_entries IS
'Bút toán bất biến (không có updated_at). Mỗi ledger_transactions phải có tổng DEBIT = tổng CREDIT (enforce tại application layer).';
COMMENT ON COLUMN ledger_entries.amount IS 'Luôn là số nguyên dương (VND); hướng ghi sổ xác định bởi entry_type.';