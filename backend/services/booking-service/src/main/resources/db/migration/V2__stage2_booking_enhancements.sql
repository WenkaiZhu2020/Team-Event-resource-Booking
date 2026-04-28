ALTER TABLE bookings.bookings
    ADD COLUMN IF NOT EXISTS approval_status VARCHAR(40),
    ADD COLUMN IF NOT EXISTS requested_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS confirmed_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS cancellation_reason VARCHAR(300),
    ADD COLUMN IF NOT EXISTS rejected_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS rejection_reason VARCHAR(300),
    ADD COLUMN IF NOT EXISTS approval_required BOOLEAN,
    ADD COLUMN IF NOT EXISTS approved_by UUID,
    ADD COLUMN IF NOT EXISTS approved_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS correlation_id VARCHAR(64);

UPDATE bookings.bookings
SET requested_at = COALESCE(requested_at, created_at),
    approval_required = COALESCE(approval_required, approval_mode <> 'AUTO_APPROVE'),
    approval_status = COALESCE(
        approval_status,
        CASE
            WHEN approval_mode = 'AUTO_APPROVE' THEN 'NOT_REQUIRED'
            WHEN status = 'APPROVED' THEN 'APPROVED'
            WHEN status = 'REJECTED' THEN 'REJECTED'
            ELSE 'PENDING'
        END
    ),
    confirmed_at = COALESCE(confirmed_at, CASE WHEN status = 'APPROVED' THEN COALESCE(decided_at, updated_at) END),
    rejected_at = COALESCE(rejected_at, CASE WHEN status = 'REJECTED' THEN COALESCE(decided_at, updated_at) END),
    approved_at = COALESCE(approved_at, CASE WHEN status = 'APPROVED' THEN COALESCE(decided_at, updated_at) END),
    correlation_id = COALESCE(correlation_id, NULL)
WHERE requested_at IS NULL
   OR approval_required IS NULL
   OR approval_status IS NULL
   OR (status = 'APPROVED' AND confirmed_at IS NULL)
   OR (status = 'REJECTED' AND rejected_at IS NULL);

CREATE TABLE IF NOT EXISTS bookings.waitlist_entries (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL REFERENCES bookings.bookings(booking_id) ON DELETE CASCADE,
    resource_id UUID NOT NULL,
    user_id UUID NOT NULL,
    start_at TIMESTAMPTZ NOT NULL,
    end_at TIMESTAMPTZ NOT NULL,
    position_index BIGINT NOT NULL,
    status VARCHAR(40) NOT NULL,
    promoted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_waitlist_resource_position
    ON bookings.waitlist_entries(resource_id, start_at, end_at, position_index);
CREATE INDEX IF NOT EXISTS idx_waitlist_lookup
    ON bookings.waitlist_entries(resource_id, start_at, end_at, status, position_index);

INSERT INTO bookings.waitlist_entries (
    id, booking_id, resource_id, user_id, start_at, end_at, position_index, status, promoted_at, created_at, updated_at, version
)
SELECT booking_id, booking_id, resource_id, user_id, start_at, end_at, waitlist_position, 'WAITING', NULL, created_at, updated_at, 0
FROM bookings.bookings
WHERE status = 'WAITLISTED'
  AND waitlist_position IS NOT NULL
ON CONFLICT (id) DO NOTHING;

CREATE TABLE IF NOT EXISTS bookings.resource_booking_locks (
    resource_id UUID PRIMARY KEY,
    version BIGINT
);

INSERT INTO bookings.resource_booking_locks(resource_id, version)
SELECT resource_id, 0
FROM bookings.booking_locks
ON CONFLICT (resource_id) DO NOTHING;

CREATE TABLE IF NOT EXISTS bookings.booking_idempotency_keys (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(120) NOT NULL,
    request_hash VARCHAR(128) NOT NULL,
    response_json TEXT,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_booking_idempotency_key ON bookings.booking_idempotency_keys(idempotency_key);
CREATE INDEX IF NOT EXISTS idx_booking_idempotency_expiry ON bookings.booking_idempotency_keys(expires_at);

CREATE TABLE IF NOT EXISTS bookings.booking_outbox (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    payload_json TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(512),
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_booking_outbox_status_created
    ON bookings.booking_outbox(status, created_at);

INSERT INTO bookings.booking_outbox (
    id, aggregate_type, aggregate_id, event_type, payload_json, status, attempts, last_error, occurred_at, created_at, published_at
)
SELECT message_id, aggregate_type, aggregate_id, event_type, payload::text, CASE WHEN published_at IS NULL THEN 'PENDING' ELSE 'PUBLISHED' END,
       0, NULL, created_at, created_at, published_at
FROM bookings.outbox_messages
ON CONFLICT (id) DO NOTHING;

CREATE TABLE IF NOT EXISTS bookings.consumed_messages (
    id UUID PRIMARY KEY,
    source VARCHAR(80) NOT NULL,
    message_id VARCHAR(120) NOT NULL,
    consumed_at TIMESTAMPTZ NOT NULL,
    payload_hash VARCHAR(128)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_consumed_message_source_id ON bookings.consumed_messages(source, message_id);
