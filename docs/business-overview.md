# Business Overview

## Roles
- `USER`
- `ORGANIZER`
- `RESOURCE_MANAGER`
- `ADMIN`

## Main Features
- User registration and login
- Google OAuth2 login
- User profile and notification preferences
- Event creation, publishing, cancellation, registration, waitlist, and check-in
- Resource catalog, availability rules, maintenance windows, and booking policies
- Resource booking with conflict prevention, approval flow, and waitlist support
- Notifications for booking updates and reminders
- Approval workflows for booking and event scenarios
- Dashboard metrics and resource usage reporting

## Service Responsibilities
- `auth-service`: authentication, JWT, OAuth2, session endpoints
- `user-service`: profiles and notification preferences
- `event-service`: events, registrations, waitlist, check-in
- `resource-service`: resources, policies, maintenance, availability
- `booking-service`: booking lifecycle, conflict checks, waitlist promotion
- `notification-service`: in-app notifications, email-style delivery, reminders
- `workflow-service`: approval requests, steps, decisions, callbacks
- `analytics-service`: dashboard queries and aggregated usage metrics
- `api-gateway-service`: API routing and request filtering

## Core Flows
1. A user signs in and gets a JWT through the gateway.
2. An organizer creates and publishes an event.
3. A resource manager creates resources and configures booking rules.
4. A user creates a booking. The booking is approved immediately, routed for approval, or waitlisted depending on policy and availability.
5. Approval results update the booking or event state and trigger notifications.
6. Analytics reads aggregated data for dashboard views.
