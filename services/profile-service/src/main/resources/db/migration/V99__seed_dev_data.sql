-- Dữ liệu demo: Gia đình Nguyễn (fictional)
INSERT INTO user_profiles (id, keycloak_sub, email, display_name, phone, status)
VALUES
    ('b1000000-0000-4000-8000-000000000001', 'kc-sub-nguyen-van-an', 'an.nguyen@demo.local', 'Nguyễn Văn An', '0901000001', 'ACTIVE'),
    ('b1000000-0000-4000-8000-000000000002', 'kc-sub-tran-thi-binh', 'binh.tran@demo.local', 'Trần Thị Bình', '0901000002', 'ACTIVE'),
    ('b1000000-0000-4000-8000-000000000003', 'kc-sub-nguyen-minh', 'minh.nguyen@demo.local', 'Nguyễn Minh', NULL, 'ACTIVE'),
    ('b1000000-0000-4000-8000-000000000004', 'kc-sub-nguyen-lan', 'lan.nguyen@demo.local', 'Nguyễn Lan', NULL, 'ACTIVE');

INSERT INTO user_preferences (user_profile_id, preference_key, preference_value)
VALUES
    ('b1000000-0000-4000-8000-000000000001', 'notifications.email', '{"enabled": true}'::jsonb),
    ('b1000000-0000-4000-8000-000000000001', 'currency.display', '{"code": "VND"}'::jsonb),
    ('b1000000-0000-4000-8000-000000000003', 'ui.theme', '{"mode": "light"}'::jsonb);

INSERT INTO user_consents (user_profile_id, consent_type, policy_version, granted, granted_at)
VALUES
    ('b1000000-0000-4000-8000-000000000001', 'TERMS_OF_SERVICE', '2026-01', TRUE, NOW() AT TIME ZONE 'UTC'),
    ('b1000000-0000-4000-8000-000000000001', 'PRIVACY_POLICY', '2026-01', TRUE, NOW() AT TIME ZONE 'UTC'),
    ('b1000000-0000-4000-8000-000000000002', 'PRIVACY_POLICY', '2026-01', TRUE, NOW() AT TIME ZONE 'UTC');