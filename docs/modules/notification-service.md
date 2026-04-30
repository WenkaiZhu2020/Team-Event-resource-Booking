# Notification Service

## Functions
- consume booking and workflow-related events
- create in-app notifications
- send email-style notifications
- track unread counts and read state
- retry failed deliveries
- schedule and send reminders

## Design Patterns
- `Facade`: notification creation and dispatch are coordinated through application services
- `Observer`: event consumers react to booking and workflow events
- `Strategy`: delivery behavior changes by channel
- `Decorator`: retry behavior wraps channel senders
- `Template Method`: notification content is rendered from reusable templates
- `Repository`: notifications, attempts, preferences, and processed messages are stored separately

## Concurrency and Parallelism
- message consumers process independent events concurrently
- idempotent consumption prevents duplicate notification creation on redelivery
- retry and reminder schedulers process due notifications in batches
- in-app and email channels can be dispatched independently

## Data Flow
1. A booking or workflow event arrives through RabbitMQ.
2. The service checks processed-message state for idempotency.
3. Templates and user preferences decide which channels to use.
4. Notification records are created and dispatched.
5. Delivery attempts are persisted.
6. Failed notifications are retried later by scheduler-driven batch processing.
