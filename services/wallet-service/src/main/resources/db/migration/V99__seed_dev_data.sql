-- Ví chung Gia đình Nguyễn + bút toán kép cân bằng (nạp 10.000.000 VND)
INSERT INTO wallet_accounts (id, family_id, owner_member_id, name, wallet_type, status)
VALUES (
    'd1000000-0000-4000-8000-000000000001',
    'a1000000-0000-4000-8000-000000000001',
    'c1000000-0000-4000-8000-000000000001',
    'Quỹ chung Gia đình Nguyễn',
    'FAMILY_POOL',
    'ACTIVE'
);

INSERT INTO wallet_accounts (id, family_id, owner_member_id, name, wallet_type, status)
VALUES (
    'd1000000-0000-4000-8000-000000000002',
    'a1000000-0000-4000-8000-000000000001',
    'c1000000-0000-4000-8000-000000000003',
    'Tiền tiêu vặt Minh',
    'MEMBER_POCKET',
    'ACTIVE'
);

INSERT INTO ledger_accounts (id, wallet_account_id, account_code, name, account_kind, normal_balance)
VALUES
    ('e1000000-0000-4000-8000-000000000001', 'd1000000-0000-4000-8000-000000000001', 'CASH_MAIN', 'Tiền mặt quỹ chung', 'ASSET', 'DEBIT'),
    ('e1000000-0000-4000-8000-000000000002', 'd1000000-0000-4000-8000-000000000001', 'EQUITY_OPEN', 'Vốn gia đình', 'EQUITY', 'CREDIT'),
    ('e1000000-0000-4000-8000-000000000003', 'd1000000-0000-4000-8000-000000000002', 'CASH_POCKET', 'Tiền tiêu vặt Minh', 'ASSET', 'DEBIT'),
    ('e1000000-0000-4000-8000-000000000004', 'd1000000-0000-4000-8000-000000000002', 'EQUITY_OPEN', 'Vốn ví con', 'EQUITY', 'CREDIT');

INSERT INTO ledger_transactions (id, wallet_account_id, reference_id, idempotency_key, description, transaction_type, posted_at)
VALUES
    (
        'f1000000-0000-4000-8000-000000000001',
        'd1000000-0000-4000-8000-000000000001',
        'seed-open-family-pool-2026',
        'idempotency-seed-open-family-pool',
        'Vốn khởi tạo quỹ chung',
        'OPENING_BALANCE',
        NOW() AT TIME ZONE 'UTC'
    ),
    (
        'f1000000-0000-4000-8000-000000000002',
        'd1000000-0000-4000-8000-000000000002',
        'seed-open-minh-pocket-2026',
        'idempotency-seed-open-minh-pocket',
        'Cấp tiền tiêu vặt Minh',
        'ALLOWANCE_FUND',
        NOW() AT TIME ZONE 'UTC'
    );

-- Transaction 1: DEBIT 10_000_000 = CREDIT 10_000_000
INSERT INTO ledger_entries (transaction_id, ledger_account_id, entry_type, amount, memo)
VALUES
    ('f1000000-0000-4000-8000-000000000001', 'e1000000-0000-4000-8000-000000000001', 'DEBIT', 10000000, 'Tiền mặt quỹ'),
    ('f1000000-0000-4000-8000-000000000001', 'e1000000-0000-4000-8000-000000000002', 'CREDIT', 10000000, 'Vốn chủ');

-- Transaction 2: DEBIT 500_000 = CREDIT 500_000
INSERT INTO ledger_entries (transaction_id, ledger_account_id, entry_type, amount, memo)
VALUES
    ('f1000000-0000-4000-8000-000000000002', 'e1000000-0000-4000-8000-000000000003', 'DEBIT', 500000, 'Tiền tiêu vặt'),
    ('f1000000-0000-4000-8000-000000000002', 'e1000000-0000-4000-8000-000000000004', 'CREDIT', 500000, 'Vốn ví con');

INSERT INTO wallet_balances (ledger_account_id, balance_amount, version)
VALUES
    ('e1000000-0000-4000-8000-000000000001', 10000000, 1),
    ('e1000000-0000-4000-8000-000000000002', 10000000, 1),
    ('e1000000-0000-4000-8000-000000000003', 500000, 1),
    ('e1000000-0000-4000-8000-000000000004', 500000, 1);