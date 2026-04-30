# Team Resource Management System

Team Resource Management System is a full-stack microservices project for internal teams, labs, university groups, and organizations that need to manage events, reserve shared resources, route approvals, notify users, and monitor operational usage from one platform.

## Stack
- Backend: Java 21, Spring Boot, Spring Security, Spring Cloud Gateway
- Frontend: React, TypeScript, Vite
- Database: PostgreSQL
- Messaging: RabbitMQ
- API documentation: Swagger / OpenAPI
- Local orchestration: Docker Compose

## Monorepo Structure
- `backend/`: Spring Boot microservices and the backend parent build
- `frontend/`: React application
- `infra/`: supporting local infrastructure assets
- `docs/`: architecture, stage refactoring, and engineering notes

## Backend Services
- `api-gateway-service`: route entry, JWT-aware request filtering, gateway error handling
- `auth-service`: email/password auth, Google OAuth2 login, JWT issuing, refresh/session operations
- `user-service`: user profile, preference management, role-aware metadata support
- `event-service`: event lifecycle, registration, waitlist, check-in
- `resource-service`: resource catalog, policy rules, availability, maintenance windows
- `booking-service`: conflict-safe booking, approval-aware lifecycle, waitlist promotion
- `notification-service`: in-app and email-style notification delivery, reminder scheduling
- `workflow-service`: approval orchestration and callback handling
- `analytics-service`: dashboard queries and usage aggregation

## Frontend Scope
- authentication and Google OAuth callback
- role-aware navigation shell
- dashboard, events, resources, bookings, approvals, notifications, account pages
- JWT-based API integration through the gateway

## Local Quick Start
1. Create a local environment file.
   - `cp .env.example .env`
2. Build backend artifacts.
   - `mvn -f backend/pom.xml -DskipTests package`
3. Start the full stack.
   - `docker compose up -d --build`
4. Open the main entry points.
   - frontend: `http://localhost:5173`
   - gateway: `http://localhost:8080`
   - RabbitMQ management: `http://localhost:15672`
5. Wait for cold-start services to finish booting before doing end-to-end checks.
   - `docker compose ps`
   - `curl http://localhost:8080/actuator/health`
   - `curl http://localhost:8081/actuator/health`

## Notes About Compose
- `.env.example` includes `COMPOSE_PROJECT_NAME=trms` so local Compose commands do not fail with `project name must not be empty`.
- If you do not want to copy `.env.example` first, use:
  - `docker compose --env-file .env.example -p trms up -d --build`
- On a cold build, some backend services can take roughly 30 to 45 seconds to become ready after the containers show `Started`.

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

## GitHub Actions
- `.github/workflows/ci.yml`: backend tests and frontend build for pull requests and non-main branch pushes
- `.github/workflows/release-package.yml`: backend tests, backend packaging, frontend build, artifact upload, and Docker image build checks for `main`

## Validation Status
The main branch has been checked with:
- backend service tests
- frontend production build
- compose configuration validation

Recent local validation includes:
- `mvn -f backend/pom.xml -pl services/booking-service,services/notification-service test -DskipITs`
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

## Current Priority Areas
- keep cross-service integration contracts stable
- preserve booking and approval correctness under concurrent load
- improve internal layering where the business logic is already rich enough to justify it
