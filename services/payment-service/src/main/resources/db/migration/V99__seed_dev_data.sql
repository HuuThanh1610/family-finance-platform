INSERT INTO payment_orders (id, family_id, payer_member_id, amount, purpose, status)
VALUES (
    'g1000000-0000-4000-8000-000000000001',
    'a1000000-0000-4000-8000-000000000001',
    'c1000000-0000-4000-8000-000000000001',
    2500000,
    'GROCERY_TRANSFER',
    'CAPTURED'
);

INSERT INTO payment_attempts (id, payment_order_id, attempt_no, provider_code, status, completed_at)
VALUES (
    'g1000000-0000-4000-8000-000000000002',
    'g1000000-0000-4000-8000-000000000001',
    1,
    'MOCK_BANK',
    'SUCCEEDED',
    NOW() AT TIME ZONE 'UTC'
);

INSERT INTO provider_transactions (payment_attempt_id, provider_txn_id, provider_code, amount, status)
VALUES (
    'g1000000-0000-4000-8000-000000000002',
    'MOCK-TXN-2026-00042',
    'MOCK_BANK',
    2500000,
    'SETTLED'
);