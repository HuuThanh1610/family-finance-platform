CREATE TABLE shopping_lists (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id       UUID NOT NULL,
    title           VARCHAR(200) NOT NULL,
    status          VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    due_date        DATE,
    created_by_member_id UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT chk_shopping_lists_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'DONE', 'CANCELLED'))
);

CREATE INDEX idx_shopping_lists_family_id ON shopping_lists (family_id);

CREATE TABLE shopping_list_items (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shopping_list_id    UUID NOT NULL REFERENCES shopping_lists (id) ON DELETE CASCADE,
    catalog_item_id     UUID REFERENCES item_catalog (id) ON DELETE SET NULL,
    item_name           VARCHAR(300) NOT NULL,
    quantity            NUMERIC(14, 3) NOT NULL DEFAULT 1,
    unit                VARCHAR(32) NOT NULL DEFAULT 'pcs',
    estimated_price     BIGINT,
    is_checked          BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT chk_shopping_list_items_qty CHECK (quantity > 0)
);

CREATE INDEX idx_shopping_list_items_list_id ON shopping_list_items (shopping_list_id);

CREATE TABLE shopping_assignments (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shopping_list_id    UUID NOT NULL REFERENCES shopping_lists (id) ON DELETE CASCADE,
    assignee_member_id  UUID NOT NULL,
    role                VARCHAR(32) NOT NULL DEFAULT 'SHOPPER',
    assigned_at         TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT uq_shopping_assignments_list_member UNIQUE (shopping_list_id, assignee_member_id),
    CONSTRAINT chk_shopping_assignments_role CHECK (role IN ('SHOPPER', 'REVIEWER'))
);

CREATE INDEX idx_shopping_assignments_list_id ON shopping_assignments (shopping_list_id);