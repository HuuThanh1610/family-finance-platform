CREATE TABLE notification_templates (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_key    VARCHAR(128) NOT NULL,
    channel         VARCHAR(32) NOT NULL,
    locale          VARCHAR(16) NOT NULL DEFAULT 'vi-VN',
    subject         VARCHAR(300),
    body_template   TEXT NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT uq_notification_templates_key_locale_channel UNIQUE (template_key, locale, channel),
    CONSTRAINT chk_notification_templates_channel CHECK (
        channel IN ('EMAIL', 'PUSH', 'SMS', 'IN_APP')
    )
);