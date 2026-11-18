INSERT INTO expense_categories (id, family_id, code, display_name)
VALUES
    ('i1000000-0000-4000-8000-000000000001', 'a1000000-0000-4000-8000-000000000001', 'SUPERMARKET', 'Siêu thị'),
    ('i1000000-0000-4000-8000-000000000002', 'a1000000-0000-4000-8000-000000000001', 'MARKET', 'Chợ Bến Thành');

INSERT INTO expenses (id, family_id, paid_by_member_id, category_id, title, amount_total, expense_date, status)
VALUES (
    'i1000000-0000-4000-8000-000000000010',
    'a1000000-0000-4000-8000-000000000001',
    'c1000000-0000-4000-8000-000000000002',
    'i1000000-0000-4000-8000-000000000001',
    'Đi Co.opmart cuối tuần',
    485000,
    '2026-11-16',
    'RECORDED'
);

INSERT INTO expense_items (expense_id, description, quantity, unit_price, line_total)
VALUES
    ('i1000000-0000-4000-8000-000000000010', 'Gạo ST25 5kg', 1, 185000, 185000),
    ('i1000000-0000-4000-8000-000000000010', 'Rau củ tươi', 1, 120000, 120000),
    ('i1000000-0000-4000-8000-000000000010', 'Sữa tươi hộp', 6, 30000, 180000);