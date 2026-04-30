# User Service

## Functions
- current profile query
- current profile update
- notification preference query and update
- internal profile provisioning for new accounts
- role summary and account metadata exposure to the frontend

## Design Patterns
- `Service Layer`: controllers delegate profile and preference logic to services
- `Repository`: profile and preference persistence are isolated behind repositories
- `DTO`: API contracts are separated from JPA entities

## Concurrency and Parallelism
- profile and preference updates are low-contention writes
- registration provisioning is synchronous so a new account has a usable profile immediately
- reads are independent and handled concurrently per request

## Data Flow
1. `auth-service` provisions a profile after registration.
2. The frontend requests `/users/me` and `/preferences/notifications`.
3. Updates are validated and written back to PostgreSQL.
4. Returned profile data is used by the account page and navigation header.
