# Event Service

## Functions
- event create, update, publish, and cancel
- published event browsing and organizer-owned event lists
- event registration and cancellation
- waitlist handling and promotion
- attendee check-in
- approval-aware publish flow for event scenarios
- reminder candidate lookup for notifications

## Design Patterns
- `State`: event status drives publish, cancel, and approval transitions
- `Builder`: event write models are assembled before persistence
- `Specification`: registration-window and eligibility checks are expressed as business rules
- `Repository`: events, registrations, and projections are stored through repositories
- `Domain Event`: event changes feed downstream workflows and reminders

## Concurrency and Parallelism
- registration requests can arrive concurrently and must respect capacity
- waitlist promotion happens after cancellation or seat release
- check-in updates are independent per attendee
- reminder queries support scheduled background polling

## Data Flow
1. An organizer creates or updates an event.
2. Publish may move directly to `PUBLISHED` or route through approval.
3. Users register while registration is open.
4. Capacity checks either confirm registration or place the user on a waitlist.
5. Cancellation or approval callbacks update event status and attendee projections.
6. Notification and workflow services consume or query event state for reminders and approvals.
