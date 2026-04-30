# Booking Service

## Functions
- booking create, read, search, and cancel
- approval-required booking flow
- waitlist support and promotion
- idempotent create requests
- outbox persistence for booking events
- synchronous validation against resource and event services

## Design Patterns
- `Facade`: write flows are exposed through application-level entry points
- `Command`: create, cancel, approve, and reject operations use explicit command models
- `State`: booking status transitions are centralized in transition logic
- `Specification`: overlap and eligibility checks are isolated from controllers
- `Repository`: bookings, locks, idempotency, and outbox data use separate persistence boundaries
- `Domain Event`: booking state changes are recorded for downstream services

## Concurrency and Parallelism
- resource-level lock acquisition prevents double booking under concurrent requests
- overlap checks run inside the same transactional path as state updates
- idempotency records suppress duplicate client retries
- waitlist promotion runs after cancellations and approval changes

## Data Flow
1. The frontend submits a booking request.
2. The service validates time, resource policy, and optional linked event state.
3. A resource lock is acquired and overlapping bookings are checked.
4. The booking becomes approved, pending approval, or waitlisted.
5. Pending approvals trigger `workflow-service`.
6. Booking events are written to the outbox for notifications and analytics.
7. Cancellation or rejection can trigger waitlist promotion.
