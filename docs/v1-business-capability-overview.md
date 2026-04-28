# Team Resource Management System V1 Business and Engineering Overview

## 1. V1 Positioning

The current V1 is an internal team coordination platform built for organizations, university groups, and shared-operation teams that need to manage events, reserve shared resources, route approvals, notify participants, and monitor operational usage from a single system.

V1 is intentionally centered on operational clarity rather than marketplace complexity. It focuses on the workflows that create the most real-world engineering value in a team setting:
- identity and access control
- event lifecycle management
- resource catalog and policy management
- conflict-safe booking
- approval routing
- notification delivery
- dashboard-style operational metrics

The architecture is implemented as a Spring Boot microservices system behind an API gateway, with PostgreSQL for service-owned persistence, RabbitMQ for domain-event messaging, and a React + TypeScript frontend for role-aware user operations.

## 2. Primary V1 Business Scenarios

### 2.1 Team Member Joins and Starts Using the Platform
A user registers with email/password or signs in with Google OAuth2. The platform issues a JWT, provisions a user profile, and grants access to profile, event, booking, and notification features according to role.

### 2.2 Organizer Creates and Publishes an Event
An organizer creates a draft event, configures the registration window, attendee capacity, and timing, then publishes it. End users can register until the event reaches capacity or registration closes.

### 2.3 Team Member Registers for an Event
A user joins an event during the active registration window. If the event is full, the system places the user on a waitlist. If a confirmed attendee cancels, the next waitlisted user is promoted automatically.

### 2.4 Resource Manager Configures Shared Resources
A resource manager creates resources such as rooms, equipment, or facilities, defines booking policies, configures availability rules, and blocks maintenance windows.

### 2.5 Team Member Creates a Resource Booking
A user selects a resource and submits a booking for a specific time range. The system validates availability, enforces maintenance restrictions, prevents overlapping bookings, and either confirms the booking immediately, routes it for approval, or places it on a waitlist.

### 2.6 Manager Handles Approval Work
When a resource requires approval, the system creates an approval request. Approvers can review pending requests, accept or reject them, and the decision flows back to booking state updates and downstream notifications.

### 2.7 Users Receive Operational Notifications
Users receive booking confirmations, rejections, waitlist promotions, and event reminders through in-app notifications and an email-simulation channel.

### 2.8 Operations Team Monitors Usage
The dashboard aggregates booking and resource usage data so administrators or operations roles can monitor demand, activity volume, and resource popularity.

## 3. V1 Business Capability Map

### 3.1 Identity and Access Management
**Business goal**
Enable authenticated, role-aware access to platform capabilities while keeping the session lifecycle manageable for a multi-service system.

**Functional scope in V1**
- email/password registration
- email/password login
- Google OAuth2 login flow
- JWT issuance and validation
- current-user session retrieval
- user profile provisioning after registration
- role-based API access

**Primary services**
- `auth-service`
- `user-service`
- `api-gateway-service`

**Technology framework**
- Spring Boot
- Spring Security
- OAuth2 client support
- JWT token processing
- PostgreSQL
- Flyway
- Spring Cloud Gateway

**Design patterns in use**
- `Facade`: auth application service acts as the orchestration entry for register/login/session flows.
- `Repository`: user identity and token-related persistence are accessed through repository abstractions.
- `Proxy`: the API gateway acts as the front-door proxy for route forwarding and token-aware request entry.
- `Adapter`: Google OAuth user information is adapted from provider responses into the internal identity model.

**Parallelism / concurrency techniques**
- request-level concurrency is handled by standard servlet-thread execution in Spring Boot.
- gateway request handling separates authentication and forwarding concerns per request.
- profile provisioning is performed as a synchronous cross-service integration step so identity and usable profile state remain consistent at V1.

### 3.2 User Profile and Preference Management
**Business goal**
Maintain user-facing identity details and communication preferences independently from the authentication domain.

**Functional scope in V1**
- self profile query
- self profile update
- notification preference query
- notification preference update
- internal profile provisioning endpoint

**Primary service**
- `user-service`

**Technology framework**
- Spring Boot Web
- Spring Data JPA
- PostgreSQL
- Flyway

**Design patterns in use**
- `Repository`: profile and preference persistence.
- `Service Layer`: application logic isolates profile update rules from controller concerns.
- `DTO`: external API contracts are separated from persistence models.

**Parallelism / concurrency techniques**
- low-contention optimistic update model for profile and preference records.
- synchronous integration from auth to user provisioning to keep registration usable immediately.

### 3.3 Event Lifecycle and Participant Management
**Business goal**
Allow organizers to manage events and allow users to register, cancel, and check in while preserving capacity constraints.

**Functional scope in V1**
- create event draft
- update draft event
- publish event
- cancel event
- browse published events
- list organizer-owned events
- event registration
- event registration cancellation
- event waitlist promotion
- attendee check-in
- attendee/waitlist/check-in projection support
- reminder candidate query for notification scheduling

**Primary service**
- `event-service`

**Technology framework**
- Spring Boot Web
- Spring Data JPA
- PostgreSQL
- Flyway

**Design patterns in use**
- `State`: event lifecycle is modeled through business status transitions such as draft, published, and cancelled.
- `Builder`: event creation/update logic is shaped as structured object assembly rather than direct controller-to-entity mutation.
- `Specification`: event filtering and registration-window eligibility rules fit specification-style business checks.
- `Domain Event` (lightweight V1 form): lifecycle changes and registration effects are treated as business transitions, even where the current implementation is not yet fully event-driven.
- `Repository`: event, registration, and projection data access.

**Parallelism / concurrency techniques**
- registration capacity handling requires correctness under concurrent registrations.
- waitlist promotion is processed as a coordinated state update after cancellations.
- reminder retrieval is scheduler-friendly and can be called periodically by notification flows.
- concurrent reads for list/detail endpoints are naturally handled by Spring MVC request threading.

### 3.4 Resource Catalog and Policy Management
**Business goal**
Provide a controlled inventory of bookable rooms, equipment, and facilities with explicit booking policies.

**Functional scope in V1**
- create resource
- update resource
- activate/deactivate resource
- browse resource catalog
- list manager-owned resources
- define booking approval mode
- define waitlist support
- define duration and advance-booking rules
- create/delete maintenance slots
- configure recurring availability rules

**Primary service**
- `resource-service`

**Technology framework**
- Spring Boot Web
- Spring Data JPA
- PostgreSQL
- Flyway

**Design patterns in use**
- `Strategy`: approval behavior and policy interpretation vary by approval mode.
- `Factory Method`: default policy generation varies by resource type.
- `Specification`: resource catalog filtering and eligibility checks.
- `Template Method` (light V1 use): resource-scoped operations follow shared validation and ownership patterns.
- `Repository`: resources, availability rules, and maintenance slots are persisted through repositories.

**Parallelism / concurrency techniques**
- availability and maintenance data is read concurrently by booking flows.
- maintenance overlap checks protect against conflicting unavailability definitions.
- resource policy evaluation is synchronous to keep booking decisions deterministic.

### 3.5 Conflict-Safe Resource Booking
**Business goal**
Enable booking creation and cancellation for shared resources without double-booking, while supporting approval-required and waitlist scenarios.

**Functional scope in V1**
- create booking
- cancel booking
- list my bookings
- get booking details
- list pending approvals
- approve or reject booking
- resource conflict detection
- approval-aware booking states
- waitlist support
- waitlist promotion after cancellation or rejection
- idempotency key support for create booking
- outbox record creation for downstream messaging
- synchronous validation against event and resource services

**Primary service**
- `booking-service`

**Technology framework**
- Spring Boot Web
- Spring Data JPA
- PostgreSQL
- Flyway
- RabbitMQ
- scheduled outbox publishing

**Design patterns in use**
- `State`: booking status drives valid transitions such as pending approval, approved, rejected, cancelled, and waitlisted.
- `Command` (partial V1 shape): booking creation/cancellation/approval actions are conceptually isolated write operations even though internal refactoring is still planned.
- `Facade`: the booking application layer coordinates policy checks, state changes, and event publication.
- `Specification`: conflict checks, availability checks, and approval requirement rules behave like domain specifications.
- `Observer` / `Domain Event`: booking changes are published for downstream consumers.
- `Repository`: bookings, lock rows, idempotency records, and outbox messages.
- `Saga-like orchestration` (practical V1 form): the service coordinates with resource, event, workflow, and notification flows without forcing a heavy distributed-transaction model.

**Parallelism / concurrency techniques**
- PostgreSQL row-level locking is used through resource-scoped lock rows to serialize conflicting booking writes.
- overlap checks run inside a transactional boundary to prevent double booking.
- idempotency records absorb duplicate create requests from retries or client re-submissions.
- outbox publishing decouples transaction commit from downstream event delivery.
- approval-required bookings reserve the slot statefully so simultaneous requests do not over-allocate the same interval.

### 3.6 Approval Workflow Management
**Business goal**
Separate approval decision tracking from booking state management while keeping business flows coordinated.

**Functional scope in V1**
- create approval request through internal API
- list pending approvals
- list requested approvals
- fetch approval detail
- approve request
- reject request
- persist decision history
- push final decision back to booking service

**Primary service**
- `workflow-service`

**Technology framework**
- Spring Boot Web
- Spring Data JPA
- PostgreSQL
- Flyway
- internal HTTP integration with booking-service

**Design patterns in use**
- `Chain of Responsibility`: approver resolution and future policy-extension points fit handler chains.
- `State`: approval records move through pending, approved, and rejected states.
- `Command`: approve/reject actions are isolated decision operations.
- `Template Method`: approval creation and approval completion share a stable process skeleton with request-specific rules.
- `Repository`: approval request and decision history persistence.

**Parallelism / concurrency techniques**
- approval decisions are transactional to ensure a single final state.
- cross-service callback to booking is kept synchronous to reduce V1 coordination ambiguity.
- separate request processing threads naturally support multiple independent approval operations at the same time.

### 3.7 Notification and Reminder Delivery
**Business goal**
Provide timely user communication for booking outcomes and event reminders through extensible channels.

**Functional scope in V1**
- consume booking domain events
- create in-app notifications
- simulate email delivery
- query user notifications
- unread count query
- mark notification as read
- scheduled event reminders by polling event-service internal endpoint
- processed-event idempotency tracking

**Primary service**
- `notification-service`

**Technology framework**
- Spring Boot Web
- Spring AMQP / RabbitMQ
- Spring Scheduling
- Spring Data JPA
- PostgreSQL
- Flyway

**Design patterns in use**
- `Observer`: notification reacts to booking events emitted by other services.
- `Strategy`: delivery channel selection varies by notification channel.
- `Factory`: sender/template selection is channel-aware and event-aware.
- `Facade`: the notification service coordinates template resolution, persistence, and delivery.
- `Template Method`: message composition and send pipelines follow common steps with channel-specific execution.
- `Decorator`: sender decoration is a natural fit for retries and delivery instrumentation, and parts of the design already align with that approach.
- `Repository`: notifications and processed event records.

**Parallelism / concurrency techniques**
- asynchronous RabbitMQ consumption decouples booking transactions from user communication.
- scheduler-driven reminder dispatch runs independently of user request traffic.
- idempotent event consumption prevents duplicate fan-out on redelivery.
- multi-notification creation can run in parallel at the consumer level across independent queue deliveries.

### 3.8 Analytics and Dashboard Metrics
**Business goal**
Provide operational visibility on bookings and resource usage without coupling dashboard reads directly to live transactional tables in multiple services.

**Functional scope in V1**
- consume booking events
- store booking facts
- aggregate dashboard overview
- refresh resource popularity metrics
- query popular resources
- scheduled aggregation refresh

**Primary service**
- `analytics-service`

**Technology framework**
- Spring Boot Web
- Spring AMQP / RabbitMQ
- Spring Scheduling
- Spring Data JPA
- PostgreSQL
- Flyway
- `CompletableFuture`

**Design patterns in use**
- `Observer`: analytics reacts to booking events.
- `Strategy`: aggregation logic is naturally grouped by metric type.
- `Facade`: dashboard endpoints collect multiple aggregate computations behind a single application entry.
- `Builder`: dashboard response assembly benefits from structured response building.
- `Repository`: fact and aggregate persistence.

**Parallelism / concurrency techniques**
- `CompletableFuture` is used for dashboard aggregation where independent metrics can be resolved concurrently.
- event-driven updates reduce synchronous coupling to booking-service.
- scheduled refresh jobs precompute popularity metrics to improve read latency.
- idempotent event consumption avoids double counting when RabbitMQ messages are retried.

### 3.9 API Gateway and Edge Routing
**Business goal**
Provide a single entry point for frontend traffic and centralized route forwarding.

**Functional scope in V1**
- forward public and protected routes to backend services
- route auth endpoints and OAuth callbacks
- centralize frontend-facing API entry path

**Primary service**
- `api-gateway-service`

**Technology framework**
- Spring Cloud Gateway
- Spring Boot
- JWT-aware routing policy

**Design patterns in use**
- `Proxy`: gateway fronts all downstream services.
- `Facade` (edge-system interpretation): gateway presents a unified API surface to the frontend.
- `Chain of Responsibility`: route matching and filter execution behave as an edge-processing chain.

**Parallelism / concurrency techniques**
- non-blocking gateway request forwarding supports concurrent cross-service traffic handling.
- route-level separation keeps public and private paths explicit at the edge.

## 4. Cross-Cutting Engineering Model in V1

### 4.1 Parallelism and Concurrency That Matter in V1
The current version uses concurrency where it changes business correctness or operational throughput, not just for academic decoration.

**Key concurrency-sensitive areas**
- booking creation for overlapping resource slots
- booking approval and cancellation transitions
- event registration capacity and waitlist promotion
- asynchronous notification fan-out
- analytics event consumption and aggregate refresh

**Implemented mechanisms**
- PostgreSQL transactional writes
- row-level locking for booking conflict serialization
- idempotency keys for sensitive booking writes
- outbox pattern for post-transaction message publication
- RabbitMQ asynchronous event delivery
- scheduler-based reminders and metric refresh jobs
- `CompletableFuture` for independent dashboard metric aggregation

### 4.2 Design Pattern Coverage in the Current V1
The current version already demonstrates a practical pattern set without forcing artificial abstraction layers.

**Most visible patterns in V1**
- `Repository`
- `Facade`
- `Strategy`
- `State`
- `Specification`
- `Observer`
- `Factory Method`
- `Template Method`
- `Command` (partial, with room for refactoring expansion)
- `Proxy`
- `Domain Event`
- `Outbox`
- `Saga-like orchestration`

### 4.3 Technology Stack by Layer
**Frontend**
- React
- TypeScript
- Vite
- role-aware UI composition
- JWT-based API client integration

**Backend service layer**
- Java 21
- Spring Boot
- Spring Web
- Spring Security
- Spring Data JPA
- Spring Cloud Gateway
- Spring AMQP
- Spring Scheduling

**Data and messaging**
- PostgreSQL
- Flyway
- RabbitMQ

**Development and delivery**
- Docker Compose
- modular per-service configuration
- service-isolated schemas
- Swagger / OpenAPI per service

## 5. V1 Functional Area Summary Matrix

| Functional area | Main services | Key business outcome | Concurrency / parallelism focus | Pattern emphasis |
| --- | --- | --- | --- | --- |
| Identity and access | auth, user, gateway | secure login and role-aware access | synchronous provisioning, request concurrency | Facade, Repository, Adapter, Proxy |
| User profile and preferences | user | profile maintenance and notification settings | low-contention profile updates | Repository, Service Layer, DTO |
| Event lifecycle | event | organizer-driven event operations | registration capacity, waitlist promotion | State, Builder, Specification, Repository |
| Resource catalog | resource | governed bookable inventory | maintenance conflict checks | Strategy, Factory Method, Specification, Template Method |
| Booking | booking, resource, event, workflow | conflict-safe reservations | row locking, idempotency, outbox, transactional overlap checks | State, Facade, Specification, Domain Event, Repository |
| Approval workflow | workflow, booking | controlled manual approval | transactional final-state protection | Chain of Responsibility, State, Command, Template Method |
| Notifications | notification, event, booking | user communication and reminders | async event consumption, scheduler fan-out | Observer, Strategy, Factory, Template Method, Decorator |
| Analytics | analytics | operational dashboard metrics | `CompletableFuture`, event-driven aggregation, scheduled refresh | Observer, Strategy, Facade, Builder |
| Edge routing | gateway | unified API entry | concurrent non-blocking forwarding | Proxy, Chain of Responsibility |

## 6. Current V1 Boundaries

V1 is operationally meaningful but not yet the fully enriched architecture.

The largest opportunities for the next refactoring cycle are:
- deeper internal structuring of booking, workflow, and notification subsystems
- fuller auth token lifecycle management
- richer event lifecycle modeling and outbox use
- stronger gateway governance
- richer role-profile modeling in user-service.


