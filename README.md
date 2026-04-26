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

## Current Scope
- `frontend`
  - Login and registration UI
  - User profile view and update form
  - Notification preference view and update form
  - Event list and event creation workflow
  - JWT storage and API client integration
- `auth-service`
  - `POST /api/v1/auth/register`
  - `POST /api/v1/auth/login`
  - `GET /api/v1/auth/me`
  - JWT issuing and validation
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
  - Organizer ownership, draft/publish/cancel lifecycle
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
- `api-gateway-service`
  - Routes `/api/v1/auth/**` to `auth-service`
  - Routes `/api/v1/users/**` and `/api/v1/preferences/**` to `user-service`
  - Routes `/api/v1/events/**` to `event-service`
  - Routes `/api/v1/resources/**` to `resource-service`
  - Routes `/api/v1/bookings/**` to `booking-service`

## Local Run
1. Copy env file
   - `cp .env.example .env`
2. Build backend jars
   - `mvn -f backend/pom.xml -DskipTests package`
3. Start services
   - `docker compose up -d --build`

Gateway URL: `http://localhost:8080`
Auth URL: `http://localhost:8081`
User URL: `http://localhost:8082`
Event URL: `http://localhost:8083`
Resource URL: `http://localhost:8084`
Booking URL: `http://localhost:8085`
