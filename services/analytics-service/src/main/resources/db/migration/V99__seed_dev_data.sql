INSERT INTO family_monthly_summary (family_id, summary_month, total_income, total_expense, net_cashflow)
VALUES (
    'a1000000-0000-4000-8000-000000000001',
    '2026-11-01',
    12000000,
    485000,
    11515000
);

INSERT INTO member_spending_summary (family_id, member_id, summary_month, total_spent, transaction_count)
VALUES
    ('a1000000-0000-4000-8000-000000000001', 'c1000000-0000-4000-8000-000000000002', '2026-11-01', 485000, 1),
    ('a1000000-0000-4000-8000-000000000001', 'c1000000-0000-4000-8000-000000000003', '2026-11-01', 0, 0);

INSERT INTO category_spending_summary (family_id, category_code, summary_month, total_spent)
VALUES
    ('a1000000-0000-4000-8000-000000000001', 'FOOD', '2026-11-01', 485000),
    ('a1000000-0000-4000-8000-000000000001', 'SUPERMARKET', '2026-11-01', 485000);

INSERT INTO inventory_summary (family_id, snapshot_date, total_sku_count, low_stock_count)
VALUES (
    'a1000000-0000-4000-8000-000000000001',
    '2026-11-18',
    2,
    1
);