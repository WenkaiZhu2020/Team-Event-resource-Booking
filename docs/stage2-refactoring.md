# Stage 2 Refactoring

## Scope
This stage strengthens three backend services so that their internal structure, runtime behavior, and test coverage align with a production-style architecture while keeping public APIs stable.

## Workflow Service
- introduced `ApprovalWorkflowFacade` to centralize approval write flows
- added command models and handlers for create, approve, reject, and callback completion
- added `ApprovalStepEntity` and explicit state handlers for approval progression
- added approval template resolution chain for booking, event, explicit-role, and fallback flows
- added outbox persistence and relay publishing for approval events
- expanded tests for final-state protection, callback integrity, and multi-step progression

## Booking Service
- introduced layered application packages with facade, command, specification, state, and repository abstractions
- added richer booking read models, paginated search, and waitlist query support
- strengthened idempotency, outbox persistence, resource precheck integration, and lock handling
- aligned cancellation, approval, rejection, and waitlist promotion flows with explicit transition logic
- added local profile config, test profile config, concurrency coverage, application smoke coverage, and integration test skeletons

## Notification Service
- introduced dispatch facade, observer dispatching, channel pipeline, template rendering, and retry decorator structure
- added notification preference API, delivery-attempt persistence, consumed-message tracking, and notification outbox model
- strengthened idempotent event consumption, batch read flows, and reminder-ready scheduling structure
- added local profile config, test profile config, application smoke coverage, consumer integration coverage, and dispatch integration coverage

## Compatibility
- preserved existing controller routes already used by the frontend and service integrations
- kept current security boundaries in place while layering richer application and persistence components underneath
- retained current migration history and extended it with Stage 2 persistence enhancements

## Validation Focus
- controller behavior remains stable for existing frontend flows
- booking concurrency and duplicate-slot handling remain covered
- notification dispatch remains idempotent across repeated message delivery
- workflow callbacks and approval transitions remain protected against invalid final-state mutations
