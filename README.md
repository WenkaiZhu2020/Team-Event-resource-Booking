# Team Resource Management System

An internal platform for teams and organizations to manage events and reserve shared resources with a microservices architecture.

## Tech Stack
- Backend: Java 21 + Spring Boot
- Frontend: React + TypeScript + Vite
- Database: PostgreSQL
- Messaging: RabbitMQ
- API Gateway: Spring Cloud Gateway

## Monorepo Structure
- `backend/` backend microservices
- `frontend/` frontend application
- `infra/` local infrastructure setup
- `docs/` business, engineering, and refactoring notes

## Current Scope
- `frontend`
  - Login and registration UI
  - Profile and notification preference workspace
  - Event list and event creation workflow
  - Resource catalog, booking form, notification inbox, approval inbox
  - JWT storage and API client integration
- `auth-service`
  - `POST /api/v1/auth/register`
  - `POST /api/v1/auth/login`
  - `GET /api/v1/auth/oauth2/google/authorize`
  - `GET /api/v1/auth/me`
  - JWT issuing and validation
  - Google OAuth2 browser login callback flow
  - PostgreSQL persistence + Flyway migration
  - Swagger UI: `http://localhost:8081/swagger-ui.html`
- `user-service`
  - `GET /api/v1/users/me`
  - `PUT /api/v1/users/me`
  - `GET /api/v1/preferences/notifications`
  - `PUT /api/v1/preferences/notifications`
  - Internal provisioning endpoint for auth registration
  - PostgreSQL persistence + Flyway migration
  - Swagger UI: `http://localhost:8082/swagger-ui.html`
- `event-service`
  - `POST /api/v1/events`
  - `PUT /api/v1/events/{eventId}`
  - `GET /api/v1/events`
  - `GET /api/v1/events/me`
  - `GET /api/v1/events/{eventId}`
  - `POST /api/v1/events/{eventId}/publish`
  - `POST /api/v1/events/{eventId}/cancel`
  - `POST /api/v1/events/{eventId}/registrations`
  - `POST /api/v1/events/{eventId}/registrations/cancel`
  - `GET /api/v1/events/{eventId}/registrations/me`
  - `GET /api/v1/events/registrations/me`
  - `GET /api/v1/events/{eventId}/registrations`
  - `POST /api/v1/events/{eventId}/registrations/{registrationId}/check-in`
  - `GET /api/v1/internal/events/reminders/due`
  - Organizer ownership, draft/publish/cancel lifecycle, event registration, waitlist promotion, attendee/waitlist/check-in projections
  - PostgreSQL persistence + Flyway migration
  - Swagger UI: `http://localhost:8083/swagger-ui.html`
- `resource-service`
  - `POST /api/v1/resources`
  - `PUT /api/v1/resources/{resourceId}`
  - `GET /api/v1/resources`
  - `GET /api/v1/resources/me`
  - `GET /api/v1/resources/{resourceId}`
  - `POST /api/v1/resources/{resourceId}/activate`
  - `POST /api/v1/resources/{resourceId}/deactivate`
  - `POST /api/v1/resources/{resourceId}/maintenance`
  - `DELETE /api/v1/resources/{resourceId}/maintenance/{slotId}`
  - Resource catalog, policy defaults, approval mode settings, maintenance windows
  - PostgreSQL persistence + Flyway migration
  - Swagger UI: `http://localhost:8084/swagger-ui.html`
- `booking-service`
  - `POST /api/v1/bookings`
  - `GET /api/v1/bookings/me`
  - `GET /api/v1/bookings/{bookingId}`
  - `POST /api/v1/bookings/{bookingId}/cancel`
  - `GET /api/v1/bookings/approvals/pending`
  - `POST /api/v1/bookings/{bookingId}/approve`
  - `POST /api/v1/bookings/{bookingId}/reject`
  - Concurrency-safe resource locking, approval-aware booking states, waitlist promotion, idempotency key support, outbox records
  - Synchronous integration with `resource-service` and `event-service`
  - PostgreSQL persistence + Flyway migration
  - Swagger UI: `http://localhost:8085/swagger-ui.html`
- `notification-service`
  - `GET /api/v1/notifications/me`
  - `GET /api/v1/notifications/me/unread-count`
  - `POST /api/v1/notifications/{notificationId}/read`
  - RabbitMQ consumer for `booking.*` domain events
  - Scheduled event reminders pulled from `event-service`
  - In-app and email-simulation channels
  - Processed-event idempotency tracking
  - PostgreSQL persistence + Flyway migration
  - Swagger UI: `http://localhost:8086/swagger-ui.html`
- `workflow-service`
  - `POST /api/v1/internal/workflows/approvals`
  - `GET /api/v1/workflows/approvals/pending`
  - `GET /api/v1/workflows/approvals/requested`
  - `GET /api/v1/workflows/approvals/{approvalId}`
  - `POST /api/v1/workflows/approvals/{approvalId}/approve`
  - `POST /api/v1/workflows/approvals/{approvalId}/reject`
  - Booking approval ownership, decision history, internal booking callback
  - PostgreSQL persistence + Flyway migration
  - Swagger UI: `http://localhost:8087/swagger-ui.html`
- `analytics-service`
  - `GET /api/v1/analytics/dashboard/overview`
  - `GET /api/v1/analytics/dashboard/resources/popular`
  - `POST /api/v1/analytics/admin/resource-popularity/refresh`
  - RabbitMQ consumer for `booking.*` domain events
  - Idempotent event consumption, booking fact aggregation, scheduled popularity refresh
  - Parallel dashboard aggregation with `CompletableFuture`
  - PostgreSQL persistence + Flyway migration
  - Swagger UI: `http://localhost:8088/swagger-ui.html`
- `api-gateway-service`
  - Routes `/api/v1/auth/**` to `auth-service`
  - Routes `/oauth2/**` and `/login/oauth2/**` to `auth-service`
  - Routes `/api/v1/users/**` and `/api/v1/preferences/**` to `user-service`
  - Routes `/api/v1/events/**` to `event-service`
  - Routes `/api/v1/resources/**` to `resource-service`
  - Routes `/api/v1/bookings/**` to `booking-service`
  - Routes `/api/v1/notifications/**` to `notification-service`
  - Routes `/api/v1/workflows/**` to `workflow-service`
  - Routes `/api/v1/analytics/**` to `analytics-service`

## Local Run
1. Copy env file
   - `cp .env.example .env`
2. Build backend jars
   - `mvn -f backend/pom.xml -DskipTests package`
3. Start services
   - `docker compose up -d --build`

## Service Ports
- `8080`: API gateway
- `8081`: auth-service
- `8082`: user-service
- `8083`: event-service
- `8084`: resource-service
- `8085`: booking-service
- `8086`: notification-service
- `8087`: workflow-service
- `8088`: analytics-service

## Notes About Compose
- `.env.example` includes `COMPOSE_PROJECT_NAME=trms` so local Compose commands do not fail with an empty project name.
- If you do not want to copy `.env.example` first, use:
  - `docker compose --env-file .env.example -p trms up -d --build`
- On a cold build, some backend services can take roughly 30 to 45 seconds to become ready after the containers show `Started`.

## GitHub Actions
- `.github/workflows/ci.yml`: backend tests and frontend build for pull requests and non-main branch pushes
- `.github/workflows/release-package.yml`: backend tests, backend packaging, frontend build, artifact upload, and Docker image build checks for `main`

## Validation Status
Recent local validation includes:
- `mvn -f backend/pom.xml test -DskipITs`
- `npm run build`

## Main Business Flows
- register or sign in, then access a role-aware workspace
- create and publish events
- register for events and handle waitlist promotion
- create and manage shared resources
- submit resource bookings with conflict-safe approval-aware rules
- review approvals and route final decisions back into booking or event state
- deliver user notifications and reminders
- view dashboard metrics and resource usage trends

## Documentation
- [V1 business capability overview](./docs/v1-business-capability-overview.md)
- [V2 business capability overview](./docs/v2-business-capability-overview.md)
- [Stage 2 refactoring summary](./docs/stage2-refactoring.md)
- [Engineering assessment and optimization backlog](./docs/engineering-assessment.md)

## Troubleshooting
- If Compose fails immediately, verify that `.env` exists or pass `--env-file .env.example -p trms`.
- If gateway requests return connection errors right after `docker compose up`, wait for downstream services such as `auth-service` and `booking-service` to finish their first cold start.
- If the frontend shows unauthorized responses after a schema or auth change, clear local storage and sign in again.
- If notification tests log RabbitMQ connection refused during local unit testing, that is expected when the broker is not started and does not indicate a failing test by itself.
