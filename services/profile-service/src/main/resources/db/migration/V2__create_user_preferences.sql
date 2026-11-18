CREATE TABLE user_preferences (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_profile_id   UUID NOT NULL REFERENCES user_profiles (id) ON DELETE CASCADE,
    preference_key    VARCHAR(128) NOT NULL,
    preference_value  JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT uq_user_preferences_profile_key UNIQUE (user_profile_id, preference_key)
);

CREATE INDEX idx_user_preferences_profile_id ON user_preferences (user_profile_id);