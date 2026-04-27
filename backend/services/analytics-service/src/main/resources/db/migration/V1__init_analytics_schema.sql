create schema if not exists analytics;

create table if not exists analytics.consumed_events (
    message_id uuid primary key,
    aggregate_type varchar(64) not null,
    aggregate_id uuid not null,
    event_type varchar(100) not null,
    consumed_at timestamptz not null
);

create table if not exists analytics.booking_facts (
    booking_id uuid primary key,
    user_id uuid not null,
    linked_event_id uuid,
    resource_id uuid not null,
    resource_name varchar(160) not null,
    resource_type varchar(32) not null,
    booking_status varchar(32) not null,
    approval_mode varchar(32) not null,
    waitlist_position integer,
    start_at timestamptz not null,
    end_at timestamptz not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    last_event_type varchar(100) not null,
    last_event_at timestamptz not null
);

create index if not exists idx_booking_facts_status on analytics.booking_facts (booking_status);
create index if not exists idx_booking_facts_resource on analytics.booking_facts (resource_id, booking_status);
create index if not exists idx_booking_facts_start_at on analytics.booking_facts (start_at);
create index if not exists idx_booking_facts_linked_event on analytics.booking_facts (linked_event_id);

create table if not exists analytics.resource_popularity (
    resource_id uuid primary key,
    resource_name varchar(160) not null,
    resource_type varchar(32) not null,
    total_bookings bigint not null,
    approved_bookings bigint not null,
    pending_bookings bigint not null,
    waitlisted_bookings bigint not null,
    cancelled_bookings bigint not null,
    total_reserved_minutes bigint not null,
    popularity_score numeric(12,2) not null,
    last_refreshed_at timestamptz not null
);

create index if not exists idx_resource_popularity_score on analytics.resource_popularity (popularity_score desc);
