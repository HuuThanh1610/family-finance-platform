CREATE TABLE expense_categories (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id       UUID NOT NULL,
    code            VARCHAR(64) NOT NULL,
    display_name    VARCHAR(200) NOT NULL,
    parent_id       UUID REFERENCES expense_categories (id) ON DELETE SET NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT uq_expense_categories_family_code UNIQUE (family_id, code)
);

CREATE INDEX idx_expense_categories_family_id ON expense_categories (family_id);