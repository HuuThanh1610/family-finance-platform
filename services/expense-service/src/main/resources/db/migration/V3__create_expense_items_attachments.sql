CREATE TABLE expense_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    expense_id      UUID NOT NULL REFERENCES expenses (id) ON DELETE CASCADE,
    description     VARCHAR(300) NOT NULL,
    quantity        NUMERIC(12, 3) NOT NULL DEFAULT 1,
    unit_price      BIGINT NOT NULL,
    line_total      BIGINT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT chk_expense_items_quantity CHECK (quantity > 0),
    CONSTRAINT chk_expense_items_unit_price CHECK (unit_price >= 0),
    CONSTRAINT chk_expense_items_line_total CHECK (line_total > 0)
);

CREATE INDEX idx_expense_items_expense_id ON expense_items (expense_id);

CREATE TABLE expense_attachments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    expense_id      UUID NOT NULL REFERENCES expenses (id) ON DELETE CASCADE,
    file_name       VARCHAR(255) NOT NULL,
    content_type    VARCHAR(128) NOT NULL,
    storage_key     VARCHAR(512) NOT NULL,
    byte_size       BIGINT NOT NULL,
    uploaded_at     TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT chk_expense_attachments_byte_size CHECK (byte_size > 0)
);

CREATE INDEX idx_expense_attachments_expense_id ON expense_attachments (expense_id);