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

## First Commit Scope
- Repository baseline (`.gitignore`, `.env.example`, `README.md`)
- Backend skeleton for three services: `api-gateway-service`, `auth-service`, `user-service`
- Minimal frontend shell with Vite + React
- Docker Compose foundation with PostgreSQL and RabbitMQ
