# Endpoint Authorization Matrix

## Payment APIs

Examples:

- `POST /api/v1/payments/authorize`
- `GET /api/v1/payments/{paymentId}`
- `POST /api/v1/payments/{paymentId}/reverse`

Authentication:

- Merchant API key resolving to the `MERCHANT` authority.
- Header format: `X-API-Key: <keyId>.<secret>`.
- `keyId` is used for lookup; `secret` is verified against stored `hmac-sha256` hash.
- Raw API key secrets are never stored.

Allowed roles:

- `MERCHANT`

## Ops APIs

Examples:

- `GET /api/v1/ops/payments`
- `GET /api/v1/ops/outbox`
- `GET /api/v1/ops/dead-letters`
- `POST /api/v1/ops/replay/{source}/{targetId}`

Authentication:

- JWT bearer token with `OPS` or `ADMIN` authority.
- Current local/test fallback: `X-User-Roles: OPS` or `X-User-Roles: ADMIN`.
- JWTs are validated for signature, issuer, audience, expiration, and `roles` claims.

Allowed roles:

- `OPS`
- `ADMIN`

## Audit Read APIs

Examples:

- future `GET /api/v1/audit/**` read endpoints

Authentication:

- JWT bearer token with `AUDITOR`, `OPS`, or `ADMIN` authority.
- Current local/test fallback: `X-User-Roles: AUDITOR`, `OPS`, or `ADMIN`.
- JWTs are validated for signature, issuer, audience, expiration, and `roles` claims.

Allowed roles:

- `AUDITOR`
- `OPS`
- `ADMIN`

## Internal Service APIs

Examples:

- future `/api/v1/internal/**` service-only endpoints

Authentication:

- JWT bearer token with `SERVICE` or `ADMIN` authority.
- Current local/test fallback: `X-User-Roles: SERVICE` or `ADMIN`.
- Internal callers send bearer JWTs with `actor_type=SERVICE` and `roles=["SERVICE"]`.

Allowed roles:

- `SERVICE`
- `ADMIN`

## Health And Readiness

Local behavior:

- `/actuator/health` and `/actuator/health/**` are public and expose only health status.
- `/actuator/prometheus` is public for local Prometheus scraping.

Production behavior:

- readiness/details should remain minimal publicly or move behind `SERVICE`/`ADMIN`.
- metrics should be restricted at the network boundary or moved behind `SERVICE` authentication.

## Security Headers And CORS

Responses include:

- `Content-Security-Policy`
- `X-Frame-Options: DENY`
- `X-Content-Type-Options: nosniff`
- HSTS headers when served over HTTPS

CORS allows configured origins only. Local defaults include:

- `http://localhost:3000`
- `http://localhost:5173`
- `http://localhost:8080`

Allowed methods are `GET`, `POST`, and `OPTIONS`. Allowed request headers are restricted to auth, API key, correlation,
content type, and idempotency headers.

## Request Rate Limiting

Payment API requests are protected by a Redis fixed-window limiter under `payment-risk.security.rate-limit`.

- Authenticated merchant requests use the resolved merchant ID as the rate-limit identity.
- Unauthenticated requests fall back to client IP before authentication rejects the request.
- Responses include `X-RateLimit-Limit` and `X-RateLimit-Remaining`.
- Exceeded limits return `429 Too Many Requests`, `RATE_LIMIT_EXCEEDED`, and `Retry-After`.
