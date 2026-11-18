INSERT INTO families (id, name, slug, description, status)
VALUES (
    'a1000000-0000-4000-8000-000000000001',
    'Gia đình Nguyễn',
    'gia-dinh-nguyen',
    'Hộ gia đình demo tại Quận 7, TP.HCM',
    'ACTIVE'
);

INSERT INTO family_members (id, family_id, user_profile_id, role_code, nickname, status)
VALUES
    ('c1000000-0000-4000-8000-000000000001', 'a1000000-0000-4000-8000-000000000001', 'b1000000-0000-4000-8000-000000000001', 'OWNER', 'Bố An', 'ACTIVE'),
    ('c1000000-0000-4000-8000-000000000002', 'a1000000-0000-4000-8000-000000000001', 'b1000000-0000-4000-8000-000000000002', 'PARENT', 'Mẹ Bình', 'ACTIVE'),
    ('c1000000-0000-4000-8000-000000000003', 'a1000000-0000-4000-8000-000000000001', 'b1000000-0000-4000-8000-000000000003', 'CHILD', 'Minh', 'ACTIVE'),
    ('c1000000-0000-4000-8000-000000000004', 'a1000000-0000-4000-8000-000000000001', 'b1000000-0000-4000-8000-000000000004', 'CHILD', 'Lan', 'ACTIVE');

INSERT INTO family_invitations (family_id, inviter_member_id, invitee_email, role_code, invite_token, status, expires_at)
VALUES (
    'a1000000-0000-4000-8000-000000000001',
    'c1000000-0000-4000-8000-000000000001',
    'ongngoai@demo.local',
    'VIEWER',
  'demo-invite-token-ongngoai-2026',
    'PENDING',
    (NOW() AT TIME ZONE 'UTC') + INTERVAL '7 days'
);

INSERT INTO family_audit_logs (family_id, actor_user_id, action, entity_type, entity_id, metadata)
VALUES (
    'a1000000-0000-4000-8000-000000000001',
    'b1000000-0000-4000-8000-000000000001',
    'FAMILY_CREATED',
    'FAMILY',
    'a1000000-0000-4000-8000-000000000001',
    '{"name": "Gia đình Nguyễn"}'::jsonb
);