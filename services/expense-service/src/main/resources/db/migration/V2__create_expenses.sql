CREATE TABLE expenses (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id           UUID NOT NULL,
    paid_by_member_id   UUID,
    category_id         UUID REFERENCES expense_categories (id) ON DELETE SET NULL,
    title               VARCHAR(300) NOT NULL,
    amount_total        BIGINT NOT NULL,
    expense_date        DATE NOT NULL,
    notes               TEXT,
    status              VARCHAR(32) NOT NULL DEFAULT 'RECORDED',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT chk_expenses_amount CHECK (amount_total > 0),
    CONSTRAINT chk_expenses_status CHECK (status IN ('DRAFT', 'RECORDED', 'VOID'))
);

CREATE INDEX idx_expenses_family_id ON expenses (family_id);
CREATE INDEX idx_expenses_expense_date ON expenses (expense_date DESC);
CREATE INDEX idx_expenses_category_id ON expenses (category_id);