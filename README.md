# Team Resource Management System

Team Resource Management System is a microservices application for managing events, shared resources, approvals, notifications, and reporting.

## Stack
- Backend: Java 21, Spring Boot, Spring Cloud Gateway
- Frontend: React, TypeScript, Vite
- Database: PostgreSQL
- Messaging: RabbitMQ
- API docs: Swagger / OpenAPI
- Local runtime: Docker Compose

## Services
- `api-gateway-service`
- `auth-service`
- `user-service`
- `event-service`
- `resource-service`
- `booking-service`
- `notification-service`
- `workflow-service`
- `analytics-service`

## Local Run
1. Copy the environment file.
   - `cp .env.example .env`
2. Build backend services.
   - `mvn -f backend/pom.xml -DskipTests package`
3. Start the stack.
   - `docker compose up -d --build`

## Ports
- `8080`: API gateway
- `8081`: auth-service
- `8082`: user-service
- `8083`: event-service
- `8084`: resource-service
- `8085`: booking-service
- `8086`: notification-service
- `8087`: workflow-service
- `8088`: analytics-service
- `5173`: frontend dev server
- `15672`: RabbitMQ management

## GitHub Actions
- `.github/workflows/ci.yml`: backend tests and frontend build for pull requests and branch pushes
- `.github/workflows/release-package.yml`: backend packaging, frontend build, artifact upload, and Docker build checks for `main`

## Documentation
- [`docs/business-overview.md`](./docs/business-overview.md)
- [`docs/demo-accounts.md`](./docs/demo-accounts.md)
- [`docs/v2-business-capability-overview.md`](./docs/v2-business-capability-overview.md)
- [`docs/modules/`](./docs/modules)

## Troubleshooting
- If Compose fails immediately, verify that `.env` exists or start with `--env-file .env.example -p trms`.
- If a service returns connection errors right after startup, wait for the initial cold boot to finish and check `docker compose ps`.
- If authentication starts failing after schema or token changes, clear local storage and sign in again.
