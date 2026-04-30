# Resource Service

## Functions
- resource create and update
- activate and deactivate resources
- catalog listing and detail queries
- availability rules and maintenance windows
- booking policy configuration
- internal approval policy lookup for workflow decisions

## Design Patterns
- `Strategy`: approval behavior changes by approval mode
- `Factory Method`: default policies can be created by resource type
- `Specification`: filtering and resource eligibility checks are isolated as rules
- `Template Method`: shared validation structure is reused across resource-scoped operations
- `Repository`: resources, policies, rules, and maintenance records are persisted through repositories

## Concurrency and Parallelism
- booking and workflow services read resource policy data concurrently
- maintenance overlap checks prevent conflicting unavailability windows
- policy lookups are synchronous because booking decisions must be deterministic

## Data Flow
1. A manager creates or updates a resource and its booking rules.
2. The frontend reads the catalog through the gateway.
3. `booking-service` fetches resource details before creating a booking.
4. `workflow-service` queries approval policy when an approval decision must be created.
5. Maintenance and availability constraints feed directly into booking validation.
