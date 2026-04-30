# Team Resource Management System V2 Business Capability Overview

## 1. Current V2 Position

The current V2 is a working internal operations platform for team event coordination and shared resource booking. It supports real user login, role-aware navigation, event management, resource booking, approval routing, notifications, and analytics-backed dashboard views through a microservice architecture.

This version is no longer just a structural skeleton. The platform now supports end-to-end walkthrough scenarios across the main operational workflows:
- user authentication and session handling
- profile and preference management
- event publishing and participation
- resource catalog browsing and booking
- approval-driven booking decisions
- notification delivery and history
- dashboard metrics and resource popularity insight

## 2. User Roles

The system currently recognizes these roles:
- `USER`
- `ORGANIZER`
- `RESOURCE_MANAGER`
- `ADMIN`

The frontend and backend both enforce role-aware behavior. Users see different navigation options and action surfaces based on the roles contained in the JWT.

## 3. Functional Capability Areas

### 3.1 Authentication and Session Management

**What is completed**
- email/password registration
- email/password login
- Google OAuth2 login entry flow
- JWT issuance and gateway validation
- current user session loading in the frontend
- role propagation in tokens

**Primary backend services**
- `api-gateway-service`
- `auth-service`

**User-facing result**
- users can sign in and enter the application with role-aware workspace access
- the frontend can restore the last authenticated session from local storage

### 3.2 User Profile and Notification Preferences

**What is completed**
- load current profile
- update display name and timezone
- load notification preferences
- update notification preferences
- internal profile provisioning after registration

**Primary backend service**
- `user-service`

**User-facing result**
- every registered user has an application profile
- users can control in-app and email preference settings

### 3.3 Event Management

**What is completed**
- create event
- update event
- publish event
- cancel event
- list published events
- list organizer-owned events
- event approval path for large events

**Primary backend service**
- `event-service`

**User-facing result**
- organizers can maintain an event pipeline from draft to published or approval-pending state
- admins and organizers can see their own operational event inventory

### 3.4 Event Registration and Check-in

**What is completed**
- register for an event
- cancel registration
- list my registrations
- list registrations for a managed event
- waitlist handling
- organizer/admin check-in

**Primary backend service**
- `event-service`

**User-facing result**
- users can join events
- organizers can manage attendance and event-day check-in activity

### 3.5 Resource Catalog and Policy Management

**What is completed**
- create and update resources
- activate and deactivate resources
- browse the resource catalog
- define approval mode
- define waitlist support
- define maximum duration and advance booking limits
- create maintenance windows
- define weekly availability rules

**Primary backend service**
- `resource-service`

**User-facing result**
- the platform can represent rooms, facilities, and equipment with realistic booking policies
- the frontend can display a populated resource catalog suitable for demo walkthroughs

### 3.6 Booking and Conflict-Safe Reservation Flow

**What is completed**
- create booking
- cancel booking
- list personal bookings
- approval-required booking flow
- waitlist flow
- booking approval and rejection
- booking outbox persistence
- idempotent booking create support
- concurrency-safe lock-based booking checks

**Primary backend service**
- `booking-service`

**User-facing result**
- users can submit bookings against shared resources
- the system prevents conflicting bookings and can place users on a waitlist when direct approval is not possible

### 3.7 Approval Workflow

**What is completed**
- create approval request for booking and event scenarios
- list pending approvals
- list requested approvals
- approve and reject requests
- multi-step approval persistence model
- approval decision history
- approval outbox persistence and relay
- booking callback integration
- event approval callback integration

**Primary backend service**
- `workflow-service`

**User-facing result**
- admins and resource managers can review and decide pending requests
- approval decisions flow back into booking or event state updates

### 3.8 Notification Delivery

**What is completed**
- booking-related notification event consumption
- in-app notification records
- email-simulation channel
- unread count query
- mark notification as read
- processed-message idempotency
- reminder-oriented notification foundation

**Primary backend service**
- `notification-service`

**User-facing result**
- users can see booking and approval outcomes reflected in a notification center
- the frontend can present both unread state and read history

### 3.9 Analytics and Dashboard Views

**What is completed**
- dashboard overview metrics
- popular resources view
- booking fact aggregation
- scheduled aggregation support
- asynchronous dashboard assembly

**Primary backend service**
- `analytics-service`

**User-facing result**
- the dashboard displays operational metrics instead of placeholder cards
- walkthrough users can see realistic usage and popularity indicators

## 4. V2 End-to-End Business Scenarios

### 4.1 Admin Walkthrough
- sign in as admin
- review dashboard totals and popular resources
- browse the resource catalog
- review organizer-owned and approval-pending events
- open the approvals view and inspect pending requests
- review notification history

### 4.2 Organizer Walkthrough
- sign in as organizer
- create or publish events
- review owned events
- review event registrations and attendee activity

### 4.3 Member Walkthrough
- sign in as member
- browse published events
- browse resources
- create a booking
- review bookings and notifications

### 4.4 Resource Manager Walkthrough
- sign in as resource manager
- review managed approval work
- browse managed resources
- inspect approval-driven bookings and notification flow

## 5. Architecture and Runtime Characteristics

### 5.1 Backend Architecture
- microservice-based Spring Boot backend
- service-owned PostgreSQL schemas
- API gateway as the single frontend entry point
- RabbitMQ for asynchronous event delivery
- Flyway for database migration control

### 5.2 Frontend Architecture
- React + TypeScript + Vite
- role-aware route and navigation model
- centralized application state provider
- API client layer with JWT propagation
- modular page and panel composition

### 5.3 Concurrency and Reliability Features Already Present
- conflict-safe booking with lock coordination
- optimistic versioning on mutable booking and workflow records
- waitlist promotion after booking changes
- outbox persistence for asynchronous publication
- processed-message tracking for idempotent message handling
- scheduled jobs for relay and aggregation behavior



