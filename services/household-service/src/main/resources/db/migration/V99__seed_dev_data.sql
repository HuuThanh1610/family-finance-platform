INSERT INTO item_catalog (id, family_id, sku, name, category, default_unit)
VALUES
    ('j1000000-0000-4000-8000-000000000001', 'a1000000-0000-4000-8000-000000000001', 'RICE-ST25-5KG', 'Gạo ST25 5kg', 'FOOD', 'bag'),
    ('j1000000-0000-4000-8000-000000000002', 'a1000000-0000-4000-8000-000000000001', 'MILK-FRESH-1L', 'Sữa tươi 1L', 'FOOD', 'box');

INSERT INTO inventory_items (id, family_id, catalog_item_id, location_code, quantity_on_hand, reorder_level)
VALUES
    ('j1000000-0000-4000-8000-000000000010', 'a1000000-0000-4000-8000-000000000001', 'j1000000-0000-4000-8000-000000000001', 'PANTRY', 1.000, 1.000),
    ('j1000000-0000-4000-8000-000000000011', 'a1000000-0000-4000-8000-000000000001', 'j1000000-0000-4000-8000-000000000002', 'FRIDGE', 4.000, 6.000);

INSERT INTO inventory_movements (inventory_item_id, movement_type, quantity_delta, reason, reference_id)
VALUES
    ('j1000000-0000-4000-8000-000000000010', 'INBOUND', 1.000, 'Mua siêu thị', 'expense-i1000000-co-op'),
    ('j1000000-0000-4000-8000-000000000011', 'OUTBOUND', -2.000, 'Uống hết', 'daily-use');

INSERT INTO shopping_lists (id, family_id, title, status, due_date, created_by_member_id)
VALUES (
    'j1000000-0000-4000-8000-000000000020',
    'a1000000-0000-4000-8000-000000000001',
    'Đi chợ tuần này',
    'OPEN',
    '2026-11-20',
    'c1000000-0000-4000-8000-000000000002'
);

INSERT INTO shopping_list_items (shopping_list_id, catalog_item_id, item_name, quantity, unit, estimated_price)
VALUES
    ('j1000000-0000-4000-8000-000000000020', 'j1000000-0000-4000-8000-000000000002', 'Sữa tươi 1L', 6, 'box', 180000),
    ('j1000000-0000-4000-8000-000000000020', NULL, 'Rau muống', 2, 'bunch', 20000);

INSERT INTO shopping_assignments (shopping_list_id, assignee_member_id, role)
VALUES (
    'j1000000-0000-4000-8000-000000000020',
    'c1000000-0000-4000-8000-000000000001',
    'SHOPPER'
);