CREATE TABLE budget_categories (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    budget_id       UUID NOT NULL REFERENCES budgets (id) ON DELETE CASCADE,
    category_code   VARCHAR(64) NOT NULL,
    display_name    VARCHAR(200) NOT NULL,
    allocated_amount BIGINT NOT NULL,
    spent_amount    BIGINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT uq_budget_categories_budget_code UNIQUE (budget_id, category_code),
    CONSTRAINT chk_budget_categories_allocated CHECK (allocated_amount >= 0),
    CONSTRAINT chk_budget_categories_spent CHECK (spent_amount >= 0)
);

CREATE INDEX idx_budget_categories_budget_id ON budget_categories (budget_id);