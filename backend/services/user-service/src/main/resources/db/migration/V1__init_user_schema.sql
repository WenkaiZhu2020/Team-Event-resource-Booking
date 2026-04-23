CREATE SCHEMA IF NOT EXISTS users;

CREATE TABLE IF NOT EXISTS users.user_profiles (
    user_id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    timezone VARCHAR(80) NOT NULL,
    role_summary VARCHAR(255) NOT NULL,
    account_status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_user_profiles_email_lower
    ON users.user_profiles (LOWER(email));

CREATE TABLE IF NOT EXISTS users.notification_preferences (
    user_id UUID PRIMARY KEY REFERENCES users.user_profiles(user_id) ON DELETE CASCADE,
    in_app_enabled BOOLEAN NOT NULL,
    email_enabled BOOLEAN NOT NULL,
    reminder_minutes_before INTEGER NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT
);
