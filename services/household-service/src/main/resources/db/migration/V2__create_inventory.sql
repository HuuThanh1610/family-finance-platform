CREATE TABLE inventory_items (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id           UUID NOT NULL,
    catalog_item_id     UUID NOT NULL REFERENCES item_catalog (id) ON DELETE RESTRICT,
    location_code       VARCHAR(64) NOT NULL DEFAULT 'PANTRY',
    quantity_on_hand    NUMERIC(14, 3) NOT NULL DEFAULT 0,
    reorder_level       NUMERIC(14, 3),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT uq_inventory_items_family_catalog_location UNIQUE (family_id, catalog_item_id, location_code),
    CONSTRAINT chk_inventory_items_qty CHECK (quantity_on_hand >= 0)
);

CREATE INDEX idx_inventory_items_family_id ON inventory_items (family_id);
CREATE INDEX idx_inventory_items_catalog_id ON inventory_items (catalog_item_id);

CREATE TABLE inventory_movements (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inventory_item_id   UUID NOT NULL REFERENCES inventory_items (id) ON DELETE RESTRICT,
    movement_type       VARCHAR(32) NOT NULL,
    quantity_delta      NUMERIC(14, 3) NOT NULL,
    reason              VARCHAR(200),
    reference_id        VARCHAR(128),
    moved_at            TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT chk_inventory_movements_type CHECK (
        movement_type IN ('INBOUND', 'OUTBOUND', 'ADJUSTMENT', 'WASTE', 'TRANSFER')
    ),
    CONSTRAINT chk_inventory_movements_delta CHECK (quantity_delta <> 0)
);

CREATE INDEX idx_inventory_movements_item_id ON inventory_movements (inventory_item_id);
CREATE INDEX idx_inventory_movements_reference ON inventory_movements (reference_id);