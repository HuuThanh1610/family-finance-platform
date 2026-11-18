-- Forward-only: support multi-wallet journals and MEMBER_WALLET type.
-- Do not rewrite V1–V4.

ALTER TABLE wallet_accounts DROP CONSTRAINT IF EXISTS chk_wallet_accounts_type;
ALTER TABLE wallet_accounts ADD CONSTRAINT chk_wallet_accounts_type CHECK (
    wallet_type IN ('FAMILY_POOL', 'MEMBER_POCKET', 'MEMBER_WALLET', 'ALLOWANCE_JAR', 'SAVINGS')
);

ALTER TABLE ledger_transactions
    ALTER COLUMN wallet_account_id DROP NOT NULL;

ALTER TABLE ledger_transactions
    ADD COLUMN IF NOT EXISTS family_id UUID;

CREATE INDEX IF NOT EXISTS idx_ledger_transactions_family_id ON ledger_transactions (family_id);

ALTER TABLE ledger_accounts
    ALTER COLUMN wallet_account_id DROP NOT NULL;

ALTER TABLE ledger_accounts
    ADD COLUMN IF NOT EXISTS family_id UUID;

CREATE INDEX IF NOT EXISTS idx_ledger_accounts_family_id ON ledger_accounts (family_id);

-- Per-family PSP clearing (ASSET, normal DEBIT) for sandbox top-ups
CREATE TABLE IF NOT EXISTS system_ledger_accounts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id       UUID NOT NULL,
    account_code    VARCHAR(64) NOT NULL,
    name            VARCHAR(200) NOT NULL,
    account_kind    VARCHAR(32) NOT NULL,
    normal_balance  VARCHAR(6) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT uq_system_ledger_family_code UNIQUE (family_id, account_code),
    CONSTRAINT chk_system_ledger_kind CHECK (
        account_kind IN ('ASSET', 'LIABILITY', 'EQUITY', 'REVENUE', 'EXPENSE')
    ),
    CONSTRAINT chk_system_ledger_normal CHECK (normal_balance IN ('DEBIT', 'CREDIT'))
);

COMMENT ON TABLE system_ledger_accounts IS
'System accounts not owned by a member wallet (e.g. PSP_CLEARING). Linked via ledger_accounts.family_id when needed.';

-- Ensure each existing wallet has a LIABILITY projection account for new postings
INSERT INTO ledger_accounts (id, wallet_account_id, account_code, name, account_kind, normal_balance, family_id)
SELECT gen_random_uuid(),
       wa.id,
       'WALLET_LIABILITY',
       'Số dư ví (liability)',
       'LIABILITY',
       'CREDIT',
       wa.family_id
FROM wallet_accounts wa
WHERE NOT EXISTS (
    SELECT 1 FROM ledger_accounts la
    WHERE la.wallet_account_id = wa.id AND la.account_code = 'WALLET_LIABILITY'
);

INSERT INTO wallet_balances (ledger_account_id, balance_amount, version)
SELECT la.id, 0, 0
FROM ledger_accounts la
WHERE la.account_code = 'WALLET_LIABILITY'
  AND NOT EXISTS (
      SELECT 1 FROM wallet_balances wb WHERE wb.ledger_account_id = la.id
  );

-- Backfill liability balances from legacy ASSET cash balances where present
UPDATE wallet_balances wb
SET balance_amount = COALESCE(legacy.balance_amount, 0),
    version = COALESCE(legacy.version, 0) + 1,
    updated_at = NOW() AT TIME ZONE 'UTC'
FROM ledger_accounts liability
JOIN ledger_accounts cash
  ON cash.wallet_account_id = liability.wallet_account_id
 AND cash.account_kind = 'ASSET'
 AND cash.normal_balance = 'DEBIT'
JOIN wallet_balances legacy ON legacy.ledger_account_id = cash.id
WHERE liability.account_code = 'WALLET_LIABILITY'
  AND wb.ledger_account_id = liability.id;
