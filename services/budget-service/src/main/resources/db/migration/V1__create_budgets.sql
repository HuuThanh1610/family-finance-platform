CREATE TABLE budgets (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id       UUID NOT NULL,
    name            VARCHAR(200) NOT NULL,
    period_start    DATE NOT NULL,
    period_end      DATE NOT NULL,
    total_limit     BIGINT NOT NULL,
    currency_code   CHAR(3) NOT NULL DEFAULT 'VND',
    status          VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT chk_budgets_total_limit CHECK (total_limit > 0),
    CONSTRAINT chk_budgets_period CHECK (period_end >= period_start),
    CONSTRAINT chk_budgets_currency CHECK (currency_code = 'VND'),
    CONSTRAINT chk_budgets_status CHECK (status IN ('DRAFT', 'ACTIVE', 'CLOSED', 'ARCHIVED'))
);

CREATE INDEX idx_budgets_family_id ON budgets (family_id);
CREATE INDEX idx_budgets_period ON budgets (period_start, period_end);