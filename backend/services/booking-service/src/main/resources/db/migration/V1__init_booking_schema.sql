CREATE SCHEMA IF NOT EXISTS bookings;

CREATE TABLE IF NOT EXISTS bookings.booking_locks (
    resource_id UUID PRIMARY KEY,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS bookings.bookings (
    booking_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    linked_event_id UUID,
    resource_id UUID NOT NULL,
    resource_name VARCHAR(160) NOT NULL,
    resource_manager_id UUID NOT NULL,
    resource_type VARCHAR(32) NOT NULL,
    start_at TIMESTAMPTZ NOT NULL,
    end_at TIMESTAMPTZ NOT NULL,
    purpose VARCHAR(240) NOT NULL,
    status VARCHAR(32) NOT NULL,
    approval_mode VARCHAR(32) NOT NULL,
    waitlist_position INTEGER,
    approval_requested_at TIMESTAMPTZ,
    decided_at TIMESTAMPTZ,
    decision_note VARCHAR(400),
    cancelled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT
);

CREATE TABLE IF NOT EXISTS bookings.idempotency_records (
    id BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(120) NOT NULL,
    user_id UUID NOT NULL,
    booking_id UUID NOT NULL REFERENCES bookings.bookings(booking_id),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_booking_idempotency UNIQUE (idempotency_key, user_id)
);

CREATE TABLE IF NOT EXISTS bookings.outbox_messages (
    message_id UUID PRIMARY KEY,
    aggregate_type VARCHAR(60) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_bookings_user_created
    ON bookings.bookings(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_bookings_resource_window
    ON bookings.bookings(resource_id, start_at, end_at);
CREATE INDEX IF NOT EXISTS idx_bookings_status_manager
    ON bookings.bookings(status, resource_manager_id);
CREATE INDEX IF NOT EXISTS idx_bookings_waitlist
    ON bookings.bookings(resource_id, status, waitlist_position, created_at);
CREATE INDEX IF NOT EXISTS idx_outbox_created
    ON bookings.outbox_messages(created_at);
