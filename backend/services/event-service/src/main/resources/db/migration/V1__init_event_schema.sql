CREATE SCHEMA IF NOT EXISTS events;

CREATE TABLE IF NOT EXISTS events.events (
    event_id UUID PRIMARY KEY,
    organizer_id UUID NOT NULL,
    title VARCHAR(160) NOT NULL,
    description TEXT,
    category VARCHAR(40) NOT NULL,
    location VARCHAR(160) NOT NULL,
    capacity INTEGER NOT NULL,
    registration_open_at TIMESTAMPTZ,
    registration_close_at TIMESTAMPTZ,
    start_at TIMESTAMPTZ NOT NULL,
    end_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT
);

CREATE INDEX IF NOT EXISTS idx_events_organizer_status ON events.events(organizer_id, status);
CREATE INDEX IF NOT EXISTS idx_events_status_start ON events.events(status, start_at);
