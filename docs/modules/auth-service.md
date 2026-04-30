# Auth Service

## Functions
- email and password registration
- email and password login
- Google OAuth2 login entry and callback processing
- JWT issuance and current-session lookup
- refresh and session-related operations
- profile provisioning call into `user-service`

## Design Patterns
- `Facade`: authentication flows are coordinated through application services
- `Repository`: identity, account, token, and provider data are persisted through repositories
- `Adapter`: Google OAuth user data is mapped into the internal identity model
- `Factory`: token creation centralizes JWT assembly

## Concurrency and Parallelism
- registration and login requests execute independently per user
- profile provisioning is synchronous to keep account and profile creation aligned
- token validation is stateless and scales horizontally

## Data Flow
1. A user submits register, login, or OAuth callback input.
2. Credentials or provider data are validated.
3. Identity records are created or loaded.
4. JWTs are generated.
5. For new accounts, a provisioning request is sent to `user-service`.
6. The frontend uses the returned token on subsequent gateway requests.
