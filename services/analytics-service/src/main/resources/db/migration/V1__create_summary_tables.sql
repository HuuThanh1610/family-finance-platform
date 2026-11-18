CREATE TABLE family_monthly_summary (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id           UUID NOT NULL,
    summary_month       DATE NOT NULL,
    total_income        BIGINT NOT NULL DEFAULT 0,
    total_expense       BIGINT NOT NULL DEFAULT 0,
    net_cashflow        BIGINT NOT NULL DEFAULT 0,
    currency_code       CHAR(3) NOT NULL DEFAULT 'VND',
    generated_at        TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT uq_family_monthly_summary UNIQUE (family_id, summary_month),
    CONSTRAINT chk_family_monthly_currency CHECK (currency_code = 'VND')
);

CREATE INDEX idx_family_monthly_summary_family ON family_monthly_summary (family_id);

CREATE TABLE member_spending_summary (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id           UUID NOT NULL,
    member_id           UUID NOT NULL,
    summary_month       DATE NOT NULL,
    total_spent         BIGINT NOT NULL DEFAULT 0,
    transaction_count   INT NOT NULL DEFAULT 0,
    currency_code       CHAR(3) NOT NULL DEFAULT 'VND',
    generated_at        TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT uq_member_spending_summary UNIQUE (family_id, member_id, summary_month),
    CONSTRAINT chk_member_spending_currency CHECK (currency_code = 'VND')
);

CREATE INDEX idx_member_spending_summary_family ON member_spending_summary (family_id);

CREATE TABLE category_spending_summary (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id           UUID NOT NULL,
    category_code       VARCHAR(64) NOT NULL,
    summary_month       DATE NOT NULL,
    total_spent         BIGINT NOT NULL DEFAULT 0,
    currency_code       CHAR(3) NOT NULL DEFAULT 'VND',
    generated_at        TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT uq_category_spending_summary UNIQUE (family_id, category_code, summary_month),
    CONSTRAINT chk_category_spending_currency CHECK (currency_code = 'VND')
);

CREATE INDEX idx_category_spending_summary_family ON category_spending_summary (family_id);

CREATE TABLE inventory_summary (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id           UUID NOT NULL,
    snapshot_date       DATE NOT NULL,
    total_sku_count     INT NOT NULL DEFAULT 0,
    low_stock_count     INT NOT NULL DEFAULT 0,
    generated_at        TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT uq_inventory_summary UNIQUE (family_id, snapshot_date)
);

CREATE INDEX idx_inventory_summary_family ON inventory_summary (family_id);