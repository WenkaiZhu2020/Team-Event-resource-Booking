# API Gateway Service

## Functions
- exposes the main HTTP entry point
- routes requests to backend services
- validates JWTs for protected routes
- allows public auth and health endpoints
- returns JSON error responses for unauthorized and forbidden requests
- adds and forwards trace IDs

## Design Patterns
- `Proxy`: the gateway stands in front of all backend services
- `Facade`: route and security configuration present a single entry surface to clients
- `Chain of Responsibility`: request filters run before downstream routing
- `Adapter`: JWT claims are converted into Spring Security authorities

## Concurrency and Parallelism
- reactive request handling supports concurrent IO-bound traffic
- trace ID generation is stateless and per-request
- gateway filtering is non-blocking and does not serialize unrelated requests

## Data Flow
1. A request enters the gateway.
2. Public paths bypass JWT enforcement.
3. Protected paths go through JWT parsing and authority mapping.
4. A trace ID is attached to the request and response.
5. The request is forwarded to the target service.
6. Security failures are converted into JSON error payloads.
