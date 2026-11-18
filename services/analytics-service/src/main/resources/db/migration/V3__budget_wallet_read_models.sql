CREATE TABLE budget_category_actual (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id           UUID NOT NULL,
    summary_month       DATE NOT NULL,
    category_code       VARCHAR(64) NOT NULL,
    category_name       VARCHAR(128) NOT NULL,
    allocated_amount    BIGINT NOT NULL DEFAULT 0,
    actual_amount       BIGINT NOT NULL DEFAULT 0,
    currency_code       CHAR(3) NOT NULL DEFAULT 'VND',
    generated_at        TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT uq_budget_category_actual UNIQUE (family_id, category_code, summary_month),
    CONSTRAINT chk_budget_category_actual_currency CHECK (currency_code = 'VND')
);

CREATE INDEX idx_budget_category_actual_family ON budget_category_actual (family_id);

CREATE TABLE wallet_balance_summary (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id           UUID NOT NULL,
    wallet_id           UUID NOT NULL,
    balance_amount      BIGINT NOT NULL DEFAULT 0,
    currency_code       CHAR(3) NOT NULL DEFAULT 'VND',
    snapshot_at         TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT uq_wallet_balance_summary UNIQUE (family_id, wallet_id),
    CONSTRAINT chk_wallet_balance_currency CHECK (currency_code = 'VND')
);

CREATE INDEX idx_wallet_balance_summary_family ON wallet_balance_summary (family_id);