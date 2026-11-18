CREATE TABLE item_catalog (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id       UUID NOT NULL,
    sku             VARCHAR(64) NOT NULL,
    name            VARCHAR(300) NOT NULL,
    category        VARCHAR(64),
    default_unit    VARCHAR(32) NOT NULL DEFAULT 'pcs',
    barcode         VARCHAR(64),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT uq_item_catalog_family_sku UNIQUE (family_id, sku)
);

CREATE INDEX idx_item_catalog_family_id ON item_catalog (family_id);