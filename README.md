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

## Current Scope (Auth V1)
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
- `api-gateway-service`
  - Routes `/api/v1/auth/**` to `auth-service`
  - Routes `/api/v1/users/**` and `/api/v1/preferences/**` to `user-service`
  - Routes `/api/v1/events/**` to `event-service`

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
