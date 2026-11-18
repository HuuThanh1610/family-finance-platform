INSERT INTO budgets (id, family_id, name, period_start, period_end, total_limit, status)
VALUES (
    '12000000-0000-4000-8000-000000000001',
    'a1000000-0000-4000-8000-000000000001',
    'Ngân sách tháng 11/2026 — Gia đình Nguyễn',
    '2026-11-01',
    '2026-11-30',
    15000000,
    'ACTIVE'
);

INSERT INTO budget_categories (budget_id, category_code, display_name, allocated_amount, spent_amount)
VALUES
    ('12000000-0000-4000-8000-000000000001', 'FOOD', 'Ăn uống', 6000000, 1200000),
    ('12000000-0000-4000-8000-000000000001', 'UTILITIES', 'Điện nước internet', 2000000, 450000),
    ('12000000-0000-4000-8000-000000000001', 'EDUCATION', 'Học tập', 3000000, 0);

INSERT INTO allowance_policies (family_id, member_id, amount, cadence, day_of_week, effective_from, status)
VALUES (
    'a1000000-0000-4000-8000-000000000001',
    'c1000000-0000-4000-8000-000000000003',
    500000,
    'WEEKLY',
    6,
    '2026-11-01',
    'ACTIVE'
);

INSERT INTO allowance_requests (id, family_id, requester_member_id, amount, reason, status)
VALUES (
    '12000000-0000-4000-8000-000000000002',
    'a1000000-0000-4000-8000-000000000001',
    'c1000000-0000-4000-8000-000000000003',
    150000,
    'Mua sách ôn thi',
    'APPROVED'
);

INSERT INTO approval_history (allowance_request_id, approver_member_id, decision, comment)
VALUES (
    '12000000-0000-4000-8000-000000000002',
    'c1000000-0000-4000-8000-000000000002',
    'APPROVED',
    'Đồng ý, mua sách Toán 9'
);