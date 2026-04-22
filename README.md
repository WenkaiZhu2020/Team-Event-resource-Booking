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
- `auth-service`
  - `POST /api/v1/auth/register`
  - `POST /api/v1/auth/login`
  - `GET /api/v1/auth/me`
  - JWT issuing and validation
  - PostgreSQL persistence + Flyway migration
  - Swagger UI: `http://localhost:8081/swagger-ui.html`
- `api-gateway-service`
  - Routes `/api/v1/auth/**` to `auth-service`

## Local Run
1. Copy env file
   - `cp .env.example .env`
2. Build backend jars
   - `mvn -f backend/pom.xml -DskipTests package`
3. Start services
   - `docker compose up -d --build`

Gateway URL: `http://localhost:8080`
Auth URL: `http://localhost:8081`
