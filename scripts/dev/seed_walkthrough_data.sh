#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
DB_CONTAINER="${DB_CONTAINER:-trms-postgres}"
DB_USER="${DB_USER:-team_resource}"
DB_NAME="${DB_NAME:-team_resource_mgmt}"
DEMO_PASSWORD="${DEMO_PASSWORD:-Password123}"

json_field() {
  local payload="$1"
  local path="$2"
  python3 - "$path" <<'PY' <<<"$payload"
import json, sys
path = sys.argv[1].split('.')
raw = sys.stdin.read().strip()
if not raw:
    print("")
    raise SystemExit(0)
obj = json.loads(raw)
cur = obj
for part in path:
    cur = cur.get(part) if isinstance(cur, dict) else None
    if cur is None:
        print("")
        raise SystemExit(0)
print(cur if not isinstance(cur, (dict, list)) else json.dumps(cur))
PY
}

ensure_account() {
  local email="$1"
  local login_payload register_payload response status_code user_id
  login_payload=$(printf '{"email":"%s","password":"%s"}' "$email" "$DEMO_PASSWORD")
  status_code=$(curl -sS -o /tmp/trms_auth_response.json -w '%{http_code}' \
    -X POST "$BASE_URL/api/v1/auth/login" \
    -H 'Content-Type: application/json' \
    -d "$login_payload" || true)

  if [[ "$status_code" != "200" ]]; then
    register_payload=$(printf '{"email":"%s","password":"%s"}' "$email" "$DEMO_PASSWORD")
    curl -sS -o /tmp/trms_auth_response.json \
      -X POST "$BASE_URL/api/v1/auth/register" \
      -H 'Content-Type: application/json' \
      -d "$register_payload" >/dev/null
  fi

  user_id=$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A \
    -c "select id from auth.app_users where email = '$email' limit 1;")

  if [[ -z "$user_id" ]]; then
    echo "Failed to provision account for $email" >&2
    exit 1
  fi

  printf '%s' "$user_id"
}

wait_for_endpoint() {
  local url="$1"
  for _ in $(seq 1 40); do
    if curl -fsS "$url" >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done
  echo "Endpoint not ready: $url" >&2
  exit 1
}

wait_for_endpoint "$BASE_URL/actuator/health"
wait_for_endpoint "http://localhost:8081/actuator/health"
wait_for_endpoint "http://localhost:8088/actuator/health"

ADMIN_EMAIL="demo.admin@teamresource.local"
ORGANIZER_EMAIL="demo.organizer@teamresource.local"
MANAGER_EMAIL="demo.manager@teamresource.local"
MEMBER_EMAIL="demo.member@teamresource.local"

ADMIN_ID=$(ensure_account "$ADMIN_EMAIL")
ORGANIZER_ID=$(ensure_account "$ORGANIZER_EMAIL")
MANAGER_ID=$(ensure_account "$MANAGER_EMAIL")
MEMBER_ID=$(ensure_account "$MEMBER_EMAIL")

export ADMIN_ID ORGANIZER_ID MANAGER_ID MEMBER_ID ADMIN_EMAIL ORGANIZER_EMAIL MANAGER_EMAIL MEMBER_EMAIL
python3 - <<'PY' > /tmp/trms_walkthrough_seed.sql
import os

admin_id = os.environ['ADMIN_ID']
organizer_id = os.environ['ORGANIZER_ID']
manager_id = os.environ['MANAGER_ID']
member_id = os.environ['MEMBER_ID']
admin_email = os.environ['ADMIN_EMAIL']
organizer_email = os.environ['ORGANIZER_EMAIL']
manager_email = os.environ['MANAGER_EMAIL']
member_email = os.environ['MEMBER_EMAIL']

sql = f"""
begin;

insert into auth.app_user_roles (user_id, role) values
  ('{admin_id}', 'ADMIN'),
  ('{admin_id}', 'ORGANIZER'),
  ('{admin_id}', 'RESOURCE_MANAGER'),
  ('{admin_id}', 'USER'),
  ('{organizer_id}', 'ORGANIZER'),
  ('{organizer_id}', 'USER'),
  ('{manager_id}', 'RESOURCE_MANAGER'),
  ('{manager_id}', 'USER'),
  ('{member_id}', 'USER')
on conflict do nothing;

update auth.app_users set
  display_name = case id
    when '{admin_id}' then 'Admin Demo'
    when '{organizer_id}' then 'Olivia Organizer'
    when '{manager_id}' then 'Mason Manager'
    when '{member_id}' then 'Uma Member'
    else display_name
  end,
  email_verified = true,
  updated_at = now()
where id in ('{admin_id}', '{organizer_id}', '{manager_id}', '{member_id}');

update users.user_profiles set
  display_name = case user_id
    when '{admin_id}' then 'Admin Demo'
    when '{organizer_id}' then 'Olivia Organizer'
    when '{manager_id}' then 'Mason Manager'
    when '{member_id}' then 'Uma Member'
    else display_name
  end,
  role_summary = case user_id
    when '{admin_id}' then 'ADMIN,ORGANIZER,RESOURCE_MANAGER,USER'
    when '{organizer_id}' then 'ORGANIZER,USER'
    when '{manager_id}' then 'RESOURCE_MANAGER,USER'
    when '{member_id}' then 'USER'
    else role_summary
  end,
  updated_at = now()
where user_id in ('{admin_id}', '{organizer_id}', '{manager_id}', '{member_id}');

insert into resources.resources (
  resource_id, manager_id, name, description, type, location, capacity, status,
  approval_mode, allow_waitlist, max_booking_duration_minutes, advance_booking_window_days,
  created_at, updated_at, version
) values
  ('10000000-0000-0000-0000-000000000001', '{admin_id}', 'Grand Hall', 'Main event hall for large internal gatherings and demo sessions.', 'FACILITY', 'North Campus / Building A', 180, 'ACTIVE', 'ADMIN_APPROVAL', true, 240, 60, now() - interval '20 days', now(), 0),
  ('10000000-0000-0000-0000-000000000002', '{manager_id}', 'Innovation Lab', 'Flexible lab space for workshops, training, and equipment setup.', 'ROOM', 'North Campus / Building B', 40, 'ACTIVE', 'MANAGER_APPROVAL', true, 180, 45, now() - interval '18 days', now(), 0),
  ('10000000-0000-0000-0000-000000000003', '{manager_id}', 'Design Studio', 'Creative project room with collaboration boards and hybrid meeting kit.', 'ROOM', 'West Wing / Floor 2', 16, 'ACTIVE', 'AUTO_APPROVE', false, 120, 30, now() - interval '15 days', now(), 0),
  ('10000000-0000-0000-0000-000000000004', '{manager_id}', 'Camera Kit Alpha', 'Portable mirrorless camera kit with tripod and lighting pack.', 'EQUIPMENT', 'Media Storage / Room M1', 1, 'ACTIVE', 'MANAGER_APPROVAL', false, 480, 20, now() - interval '10 days', now(), 0)
on conflict (resource_id) do update set
  manager_id = excluded.manager_id,
  name = excluded.name,
  description = excluded.description,
  type = excluded.type,
  location = excluded.location,
  capacity = excluded.capacity,
  status = excluded.status,
  approval_mode = excluded.approval_mode,
  allow_waitlist = excluded.allow_waitlist,
  max_booking_duration_minutes = excluded.max_booking_duration_minutes,
  advance_booking_window_days = excluded.advance_booking_window_days,
  updated_at = now();

insert into resources.availability_rules (availability_rule_id, resource_id, day_of_week, start_time, end_time, available, created_at) values
  ('11000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 1, '08:00', '20:00', true, now()),
  ('11000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 2, '08:00', '20:00', true, now()),
  ('11000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', 3, '08:00', '20:00', true, now()),
  ('11000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000001', 4, '08:00', '20:00', true, now()),
  ('11000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000001', 5, '08:00', '20:00', true, now()),
  ('11000000-0000-0000-0000-000000000006', '10000000-0000-0000-0000-000000000002', 1, '08:00', '19:00', true, now()),
  ('11000000-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000002', 2, '08:00', '19:00', true, now()),
  ('11000000-0000-0000-0000-000000000008', '10000000-0000-0000-0000-000000000002', 3, '08:00', '19:00', true, now()),
  ('11000000-0000-0000-0000-000000000009', '10000000-0000-0000-0000-000000000003', 1, '09:00', '18:00', true, now()),
  ('11000000-0000-0000-0000-000000000010', '10000000-0000-0000-0000-000000000003', 2, '09:00', '18:00', true, now()),
  ('11000000-0000-0000-0000-000000000011', '10000000-0000-0000-0000-000000000004', 1, '09:00', '17:00', true, now()),
  ('11000000-0000-0000-0000-000000000012', '10000000-0000-0000-0000-000000000004', 3, '09:00', '17:00', true, now())
on conflict (resource_id, day_of_week, start_time, end_time) do update set
  available = excluded.available;

insert into events.events (
  event_id, organizer_id, title, description, category, location, capacity,
  registration_open_at, registration_close_at, start_at, end_at, status,
  created_at, updated_at, version, attendee_projected_count, waitlist_projected_count, checked_in_count
) values
  ('20000000-0000-0000-0000-000000000001', '{admin_id}', 'Architecture Guild Meetup', 'Internal sharing session covering service boundaries, approval flow, and booking consistency.', 'WORKSHOP', 'Design Studio', 24, now() - interval '14 days', now() + interval '1 day', now() + interval '2 days', now() + interval '2 days 2 hours', 'PUBLISHED', now() - interval '14 days', now(), 0, 1, 0, 0),
  ('20000000-0000-0000-0000-000000000002', '{organizer_id}', 'Semester Lab Orientation', 'Hands-on onboarding for new student volunteers and internal team members.', 'TRAINING', 'Innovation Lab', 32, now() - interval '20 days', now() - interval '4 days', now() - interval '2 days', now() - interval '2 days' + interval '2 hours', 'PUBLISHED', now() - interval '20 days', now(), 0, 1, 0, 1),
  ('20000000-0000-0000-0000-000000000003', '{admin_id}', 'Annual Operations Summit', 'Large internal summit that intentionally exercises approval-required event lifecycle.', 'MEETING', 'Grand Hall', 180, now() - interval '3 days', now() + interval '10 days', now() + interval '14 days', now() + interval '14 days 4 hours', 'PENDING_APPROVAL', now() - interval '3 days', now(), 0, 0, 0, 0),
  ('20000000-0000-0000-0000-000000000004', '{organizer_id}', 'Community Design Sprint', 'Collaborative sprint for posters, booth assets, and registration materials.', 'SOCIAL', 'Innovation Lab', 20, now() - interval '7 days', now() + interval '5 days', now() + interval '7 days', now() + interval '7 days 3 hours', 'PUBLISHED', now() - interval '7 days', now(), 0, 1, 1, 0)
on conflict (event_id) do update set
  organizer_id = excluded.organizer_id,
  title = excluded.title,
  description = excluded.description,
  category = excluded.category,
  location = excluded.location,
  capacity = excluded.capacity,
  registration_open_at = excluded.registration_open_at,
  registration_close_at = excluded.registration_close_at,
  start_at = excluded.start_at,
  end_at = excluded.end_at,
  status = excluded.status,
  updated_at = now(),
  attendee_projected_count = excluded.attendee_projected_count,
  waitlist_projected_count = excluded.waitlist_projected_count,
  checked_in_count = excluded.checked_in_count;

insert into events.event_registrations (
  registration_id, event_id, user_id, status, waitlist_position, registered_at, cancelled_at,
  created_at, updated_at, checked_in_at, checked_in_by
) values
  ('21000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', '{member_id}', 'REGISTERED', null, now() - interval '2 days', null, now() - interval '2 days', now(), null, null),
  ('21000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002', '{admin_id}', 'REGISTERED', null, now() - interval '8 days', null, now() - interval '8 days', now(), now() - interval '2 days' + interval '10 minutes', '{organizer_id}'),
  ('21000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000004', '{member_id}', 'WAITLISTED', 1, now() - interval '1 day', null, now() - interval '1 day', now(), null, null)
on conflict (registration_id) do update set
  status = excluded.status,
  waitlist_position = excluded.waitlist_position,
  updated_at = now(),
  checked_in_at = excluded.checked_in_at,
  checked_in_by = excluded.checked_in_by;

insert into bookings.bookings (
  booking_id, user_id, linked_event_id, resource_id, resource_name, resource_manager_id, resource_type,
  start_at, end_at, purpose, status, approval_mode, waitlist_position, approval_requested_at,
  decided_at, decision_note, cancelled_at, created_at, updated_at, version
) values
  ('30000000-0000-0000-0000-000000000001', '{admin_id}', '20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000003', 'Design Studio', '{manager_id}', 'ROOM', now() + interval '1 day', now() + interval '1 day 2 hours', 'Prepare architecture guild workshop materials', 'APPROVED', 'AUTO_APPROVE', null, null, now() - interval '1 day', 'Auto-approved standard studio reservation', null, now() - interval '1 day', now(), 0),
  ('30000000-0000-0000-0000-000000000002', '{member_id}', null, '10000000-0000-0000-0000-000000000001', 'Grand Hall', '{admin_id}', 'FACILITY', now() + interval '5 days 14 hours', now() + interval '5 days 17 hours', 'Town hall rehearsal block', 'PENDING_APPROVAL', 'ADMIN_APPROVAL', null, now() - interval '4 hours', null, null, null, now() - interval '4 hours', now(), 0),
  ('30000000-0000-0000-0000-000000000003', '{member_id}', null, '10000000-0000-0000-0000-000000000001', 'Grand Hall', '{admin_id}', 'FACILITY', now() + interval '5 days 14 hours 30 minutes', now() + interval '5 days 16 hours', 'Overflow rehearsal fallback', 'WAITLISTED', 'ADMIN_APPROVAL', 1, now() - interval '3 hours', null, null, null, now() - interval '3 hours', now(), 0),
  ('30000000-0000-0000-0000-000000000004', '{organizer_id}', null, '10000000-0000-0000-0000-000000000004', 'Camera Kit Alpha', '{manager_id}', 'EQUIPMENT', now() - interval '1 day 5 hours', now() - interval '1 day 3 hours', 'Photo coverage dry run', 'CANCELLED', 'MANAGER_APPROVAL', null, now() - interval '2 days', now() - interval '1 day 6 hours', 'Cancelled after schedule change', now() - interval '1 day 6 hours', now() - interval '2 days', now(), 0),
  ('30000000-0000-0000-0000-000000000005', '{member_id}', '20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002', 'Innovation Lab', '{manager_id}', 'ROOM', now() + interval '2 days 12 hours', now() + interval '2 days 14 hours', 'Workshop hardware staging', 'APPROVED', 'MANAGER_APPROVAL', null, now() - interval '2 days', now() - interval '1 day', 'Approved by resource manager', null, now() - interval '2 days', now(), 0),
  ('30000000-0000-0000-0000-000000000006', '{manager_id}', null, '10000000-0000-0000-0000-000000000001', 'Grand Hall', '{admin_id}', 'FACILITY', now() + interval '10 days 10 hours', now() + interval '10 days 12 hours', 'Equipment logistics checkpoint', 'REJECTED', 'ADMIN_APPROVAL', null, now() - interval '6 days', now() - interval '5 days', 'Hall reserved for a larger approved event', null, now() - interval '6 days', now(), 0)
on conflict (booking_id) do update set
  user_id = excluded.user_id,
  linked_event_id = excluded.linked_event_id,
  resource_id = excluded.resource_id,
  resource_name = excluded.resource_name,
  resource_manager_id = excluded.resource_manager_id,
  resource_type = excluded.resource_type,
  start_at = excluded.start_at,
  end_at = excluded.end_at,
  purpose = excluded.purpose,
  status = excluded.status,
  approval_mode = excluded.approval_mode,
  waitlist_position = excluded.waitlist_position,
  approval_requested_at = excluded.approval_requested_at,
  decided_at = excluded.decided_at,
  decision_note = excluded.decision_note,
  cancelled_at = excluded.cancelled_at,
  updated_at = now();

insert into analytics.booking_facts (
  booking_id, user_id, linked_event_id, resource_id, resource_name, resource_type, booking_status,
  approval_mode, waitlist_position, start_at, end_at, created_at, updated_at, last_event_type, last_event_at
) values
  ('30000000-0000-0000-0000-000000000001', '{admin_id}', '20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000003', 'Design Studio', 'ROOM', 'APPROVED', 'AUTO_APPROVE', null, now() + interval '1 day', now() + interval '1 day 2 hours', now() - interval '1 day', now(), 'booking.approved', now() - interval '1 day'),
  ('30000000-0000-0000-0000-000000000002', '{member_id}', null, '10000000-0000-0000-0000-000000000001', 'Grand Hall', 'FACILITY', 'PENDING_APPROVAL', 'ADMIN_APPROVAL', null, now() + interval '5 days 14 hours', now() + interval '5 days 17 hours', now() - interval '4 hours', now(), 'booking.created', now() - interval '4 hours'),
  ('30000000-0000-0000-0000-000000000003', '{member_id}', null, '10000000-0000-0000-0000-000000000001', 'Grand Hall', 'FACILITY', 'WAITLISTED', 'ADMIN_APPROVAL', 1, now() + interval '5 days 14 hours 30 minutes', now() + interval '5 days 16 hours', now() - interval '3 hours', now(), 'booking.waitlisted', now() - interval '3 hours'),
  ('30000000-0000-0000-0000-000000000004', '{organizer_id}', null, '10000000-0000-0000-0000-000000000004', 'Camera Kit Alpha', 'EQUIPMENT', 'CANCELLED', 'MANAGER_APPROVAL', null, now() - interval '1 day 5 hours', now() - interval '1 day 3 hours', now() - interval '2 days', now(), 'booking.cancelled', now() - interval '1 day 6 hours'),
  ('30000000-0000-0000-0000-000000000005', '{member_id}', '20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002', 'Innovation Lab', 'ROOM', 'APPROVED', 'MANAGER_APPROVAL', null, now() + interval '2 days 12 hours', now() + interval '2 days 14 hours', now() - interval '2 days', now(), 'booking.approved', now() - interval '1 day'),
  ('30000000-0000-0000-0000-000000000006', '{manager_id}', null, '10000000-0000-0000-0000-000000000001', 'Grand Hall', 'FACILITY', 'REJECTED', 'ADMIN_APPROVAL', null, now() + interval '10 days 10 hours', now() + interval '10 days 12 hours', now() - interval '6 days', now(), 'booking.rejected', now() - interval '5 days')
on conflict (booking_id) do update set
  booking_status = excluded.booking_status,
  approval_mode = excluded.approval_mode,
  waitlist_position = excluded.waitlist_position,
  start_at = excluded.start_at,
  end_at = excluded.end_at,
  updated_at = now(),
  last_event_type = excluded.last_event_type,
  last_event_at = excluded.last_event_at;

insert into workflows.approval_requests (
  approval_id, target_type, target_id, approval_type, requester_id, approver_id, target_owner_id, resource_id,
  title, summary, current_step, total_steps, status, submitted_at, decided_at, decision_note, created_at, updated_at, version, approval_scope
) values
  ('40000000-0000-0000-0000-000000000001', 'BOOKING', '30000000-0000-0000-0000-000000000002', 'RESOURCE_BOOKING_ADMIN_APPROVAL', '{member_id}', '{admin_id}', '{admin_id}', '10000000-0000-0000-0000-000000000001', 'Grand Hall booking approval', 'Member request waiting for admin decision before the slot can be confirmed.', 1, 1, 'PENDING', now() - interval '4 hours', null, null, now() - interval '4 hours', now(), 0, 'ADMIN_ONLY'),
  ('40000000-0000-0000-0000-000000000002', 'BOOKING', '30000000-0000-0000-0000-000000000005', 'RESOURCE_BOOKING_APPROVAL', '{member_id}', '{manager_id}', '{manager_id}', '10000000-0000-0000-0000-000000000002', 'Innovation Lab booking approval', 'Historical approved request kept for approval center walkthrough context.', 1, 1, 'APPROVED', now() - interval '2 days', now() - interval '1 day', 'Approved by resource manager', now() - interval '2 days', now(), 0, 'ASSIGNED_USER')
on conflict (approval_id) do update set
  target_type = excluded.target_type,
  target_id = excluded.target_id,
  approval_type = excluded.approval_type,
  requester_id = excluded.requester_id,
  approver_id = excluded.approver_id,
  target_owner_id = excluded.target_owner_id,
  resource_id = excluded.resource_id,
  title = excluded.title,
  summary = excluded.summary,
  current_step = excluded.current_step,
  total_steps = excluded.total_steps,
  status = excluded.status,
  submitted_at = excluded.submitted_at,
  decided_at = excluded.decided_at,
  decision_note = excluded.decision_note,
  updated_at = now(),
  approval_scope = excluded.approval_scope;

insert into workflows.approval_steps (
  step_id, approval_id, step_number, approver_id, status, decision_note, decided_at, created_at, updated_at
) values
  ('41000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 1, '{admin_id}', 'PENDING', null, null, now() - interval '4 hours', now()),
  ('41000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000002', 1, '{manager_id}', 'APPROVED', 'Approved by resource manager', now() - interval '1 day', now() - interval '2 days', now())
on conflict (step_id) do update set
  status = excluded.status,
  decision_note = excluded.decision_note,
  decided_at = excluded.decided_at,
  updated_at = now();

insert into workflows.approval_decision_history (
  decision_id, approval_id, action, actor_id, note, acted_at, created_at
) values
  ('42000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', 'CREATED', '{member_id}', 'Approval created for walkthrough seed data.', now() - interval '4 hours', now() - interval '4 hours'),
  ('42000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000002', 'CREATED', '{member_id}', 'Initial approval request created.', now() - interval '2 days', now() - interval '2 days'),
  ('42000000-0000-0000-0000-000000000003', '40000000-0000-0000-0000-000000000002', 'APPROVED', '{manager_id}', 'Approved by resource manager', now() - interval '1 day', now() - interval '1 day')
on conflict (decision_id) do update set
  note = excluded.note,
  acted_at = excluded.acted_at,
  created_at = excluded.created_at;

insert into notifications.notification_records (
  notification_id, user_id, source_event_id, source_event_type, notification_type, channel,
  subject, body, status, read_at, sent_at, failure_reason, created_at, updated_at
) values
  ('50000000-0000-0000-0000-000000000001', '{admin_id}', '40000000-0000-0000-0000-000000000001', 'workflow.approval.created', 'BOOKING_CREATED', 'IN_APP', 'Grand Hall approval waiting', 'A new booking approval request is waiting in your approval queue.', 'SENT', null, now() - interval '3 hours', null, now() - interval '3 hours', now()),
  ('50000000-0000-0000-0000-000000000002', '{admin_id}', '20000000-0000-0000-0000-000000000002', 'event.reminder', 'EVENT_REMINDER', 'IN_APP', 'Semester Lab Orientation completed', 'Orientation attendance was checked in successfully.', 'READ', now() - interval '1 day', now() - interval '2 days', null, now() - interval '2 days', now()),
  ('50000000-0000-0000-0000-000000000003', '{member_id}', '30000000-0000-0000-0000-000000000005', 'booking.approved', 'BOOKING_APPROVED', 'IN_APP', 'Innovation Lab approved', 'Your Innovation Lab booking was approved and added to your active schedule.', 'SENT', null, now() - interval '1 day', null, now() - interval '1 day', now()),
  ('50000000-0000-0000-0000-000000000004', '{member_id}', '30000000-0000-0000-0000-000000000003', 'booking.waitlisted', 'WAITLIST_PROMOTED', 'EMAIL', 'Grand Hall waitlist update', 'Your fallback request is currently waitlisted. We will notify you if it is promoted.', 'SENT', null, now() - interval '2 hours', null, now() - interval '2 hours', now())
on conflict (source_event_id, user_id, channel) do update set
  subject = excluded.subject,
  body = excluded.body,
  status = excluded.status,
  read_at = excluded.read_at,
  sent_at = excluded.sent_at,
  updated_at = now();

insert into analytics.resource_popularity (
  resource_id, resource_name, resource_type, total_bookings, approved_bookings, pending_bookings,
  waitlisted_bookings, cancelled_bookings, total_reserved_minutes, popularity_score, last_refreshed_at
)
select
  resource_id,
  max(resource_name),
  max(resource_type),
  count(*),
  sum(case when booking_status = 'APPROVED' then 1 else 0 end),
  sum(case when booking_status = 'PENDING_APPROVAL' then 1 else 0 end),
  sum(case when booking_status = 'WAITLISTED' then 1 else 0 end),
  sum(case when booking_status in ('CANCELLED', 'REJECTED') then 1 else 0 end),
  coalesce(sum(case when booking_status = 'APPROVED' then extract(epoch from (end_at - start_at)) / 60 else 0 end), 0)::bigint,
  (
    sum(case when booking_status = 'APPROVED' then 10 else 0 end) +
    sum(case when booking_status = 'PENDING_APPROVAL' then 6 else 0 end) +
    sum(case when booking_status = 'WAITLISTED' then 3 else 0 end) +
    coalesce(sum(case when booking_status = 'APPROVED' then extract(epoch from (end_at - start_at)) / 60 else 0 end), 0) / 60.0
  )::numeric(12,2),
  now()
from analytics.booking_facts
where resource_id in (
  '10000000-0000-0000-0000-000000000001',
  '10000000-0000-0000-0000-000000000002',
  '10000000-0000-0000-0000-000000000003',
  '10000000-0000-0000-0000-000000000004'
)
group by resource_id
on conflict (resource_id) do update set
  resource_name = excluded.resource_name,
  resource_type = excluded.resource_type,
  total_bookings = excluded.total_bookings,
  approved_bookings = excluded.approved_bookings,
  pending_bookings = excluded.pending_bookings,
  waitlisted_bookings = excluded.waitlisted_bookings,
  cancelled_bookings = excluded.cancelled_bookings,
  total_reserved_minutes = excluded.total_reserved_minutes,
  popularity_score = excluded.popularity_score,
  last_refreshed_at = excluded.last_refreshed_at;

commit;
"""
print(sql)
PY

docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 < /tmp/trms_walkthrough_seed.sql

echo "Seeded walkthrough data."
echo "Admin:    $ADMIN_EMAIL / $DEMO_PASSWORD"
echo "Organizer:$ORGANIZER_EMAIL / $DEMO_PASSWORD"
echo "Manager:  $MANAGER_EMAIL / $DEMO_PASSWORD"
echo "Member:   $MEMBER_EMAIL / $DEMO_PASSWORD"
