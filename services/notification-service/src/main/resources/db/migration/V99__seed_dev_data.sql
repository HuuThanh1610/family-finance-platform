INSERT INTO notification_templates (template_key, channel, subject, body_template)
VALUES
    (
        'ALLOWANCE_APPROVED',
        'IN_APP',
        'Yêu cầu tiền tiêu vặt được duyệt',
        'Chào {{display_name}}, yêu cầu {{amount}} VND của bạn đã được duyệt.'
    ),
    (
        'BUDGET_ALERT',
        'EMAIL',
        'Cảnh báo ngân sách',
        'Gia đình {{family_name}} đã dùng {{percent}}% hạn mức danh mục {{category}}.'
    );

INSERT INTO notification_preferences (user_profile_id, channel, category, enabled)
VALUES
    ('b1000000-0000-4000-8000-000000000001', 'EMAIL', 'BUDGET', TRUE),
    ('b1000000-0000-4000-8000-000000000003', 'IN_APP', 'ALLOWANCE', TRUE);

INSERT INTO notifications (id, recipient_user_id, family_id, template_key, channel, title, body, status)
VALUES (
    'k1000000-0000-4000-8000-000000000001',
    'b1000000-0000-4000-8000-000000000003',
    'a1000000-0000-4000-8000-000000000001',
    'ALLOWANCE_APPROVED',
    'IN_APP',
    'Tiền tiêu vặt đã duyệt',
    'Mẹ Bình đã duyệt 150.000 VND mua sách ôn thi cho Minh.',
    'DELIVERED'
);

INSERT INTO notification_deliveries (notification_id, attempt_no, provider, status)
VALUES ('k1000000-0000-4000-8000-000000000001', 1, 'IN_APP_MOCK', 'DELIVERED');