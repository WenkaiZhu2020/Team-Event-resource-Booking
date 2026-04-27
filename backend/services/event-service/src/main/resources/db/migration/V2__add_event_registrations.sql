alter table events.events
    add column if not exists attendee_projected_count integer not null default 0,
    add column if not exists waitlist_projected_count integer not null default 0;

create table if not exists events.event_registrations (
    registration_id uuid primary key,
    event_id uuid not null references events.events(event_id) on delete cascade,
    user_id uuid not null,
    status varchar(32) not null,
    waitlist_position integer,
    registered_at timestamptz not null,
    cancelled_at timestamptz,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint uk_event_registration_event_user unique (event_id, user_id)
);

create index if not exists idx_event_registrations_event_status
    on events.event_registrations(event_id, status, waitlist_position);

create index if not exists idx_event_registrations_user_registered
    on events.event_registrations(user_id, registered_at desc);
