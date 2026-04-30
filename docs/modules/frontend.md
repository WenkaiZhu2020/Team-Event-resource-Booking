# Frontend Module

## Functions
- authentication views and OAuth callback handling
- role-aware navigation and protected routes
- dashboard, system, account, events, resources, bookings, notifications, and approvals pages
- API calls through the gateway
- local session state and form state management

## Design Patterns
- `Context`: shared application state is provided through `AppContext`
- `Adapter`: API responses are normalized for UI consumption
- `Composition`: pages are built from smaller panels and shared layout components
- `Guard`: protected routes prevent access without a valid session

## Concurrency and Parallelism
- browser-side concurrency comes from parallel HTTP requests during workspace reload
- page refresh actions can load independent datasets without blocking unrelated screens
- UI state prevents duplicate submissions on long-running actions

## Data Flow
1. The user signs in or completes OAuth.
2. The frontend stores the token and calls the gateway.
3. Workspace pages request data from service-specific endpoints through the gateway.
4. Responses are mapped into local state and rendered into module panels.
5. Write actions post mutations and then reload the affected workspace slices.
