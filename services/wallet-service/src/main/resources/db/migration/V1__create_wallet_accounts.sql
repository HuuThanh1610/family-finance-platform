CREATE TABLE wallet_accounts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id       UUID NOT NULL,
    owner_member_id UUID,
    name            VARCHAR(200) NOT NULL,
    currency_code   CHAR(3) NOT NULL DEFAULT 'VND',
    wallet_type     VARCHAR(32) NOT NULL DEFAULT 'FAMILY_POOL',
    status          VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT chk_wallet_accounts_currency CHECK (currency_code = 'VND'),
    CONSTRAINT chk_wallet_accounts_type CHECK (
        wallet_type IN ('FAMILY_POOL', 'MEMBER_POCKET', 'ALLOWANCE_JAR', 'SAVINGS')
    ),
    CONSTRAINT chk_wallet_accounts_status CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED'))
);

CREATE INDEX idx_wallet_accounts_family_id ON wallet_accounts (family_id);
CREATE INDEX idx_wallet_accounts_owner_member_id ON wallet_accounts (owner_member_id);

COMMENT ON TABLE wallet_accounts IS 'Ví logic; số dư không lưu trực tiếp tại đây — chỉ qua ledger.';