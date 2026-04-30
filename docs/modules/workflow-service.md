# Workflow Service

## Functions
- create approval requests for booking and event scenarios
- list pending and requested approvals
- approve and reject requests
- persist approval steps and decision history
- callback booking and event services with final decisions
- relay workflow events through an outbox publisher

## Design Patterns
- `Facade`: approval write flows are exposed through `ApprovalWorkflowFacade`
- `Command`: create, approve, reject, and callback actions are expressed as commands
- `State`: approval transitions are validated by explicit state handlers
- `Chain of Responsibility`: template and approver resolution use handler chains
- `Repository`: requests, steps, history, consumed messages, and outbox records are stored separately
- `Outbox`: workflow events are persisted before publication

## Concurrency and Parallelism
- approval requests are independent units of work and can be processed concurrently
- state handlers protect final-state integrity under repeated decisions
- outbox relay publishing runs asynchronously from decision writes
- multi-step approvals move one step at a time and keep callback execution to final transitions

## Data Flow
1. `booking-service` or `event-service` requests an approval.
2. The workflow service resolves approval type and approvers.
3. A request and its steps are persisted.
4. Approvers read pending work and submit decisions.
5. Intermediate approvals advance the next step.
6. Final decisions update the request, write outbox records, and callback the target service.
