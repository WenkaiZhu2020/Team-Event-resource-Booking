# Analytics Service

## Functions
- dashboard overview metrics
- popular resource queries
- booking fact aggregation
- scheduled refresh of derived analytics data
- administrative refresh endpoint

## Design Patterns
- `Facade`: dashboard assembly is coordinated through query services
- `Observer`: domain events update analytics facts
- `Strategy`: aggregation paths can vary by event type or metric family
- `Builder`: dashboard responses are assembled from multiple aggregated sources
- `Repository`: facts and derived views are stored behind repositories

## Concurrency and Parallelism
- dashboard queries use `CompletableFuture` to assemble independent metric groups in parallel
- event consumption updates facts asynchronously from the user-facing request path
- scheduled refresh jobs recompute popularity snapshots in the background

## Data Flow
1. Booking events are consumed from RabbitMQ.
2. Fact tables are updated for reporting.
3. Scheduled refresh jobs recompute derived metrics such as popularity.
4. The frontend dashboard requests overview and top-resource data.
5. The service combines aggregated results into dashboard responses.
