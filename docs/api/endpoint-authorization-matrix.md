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

Allowed roles:

- `OPS`
- `ADMIN`

## Audit Read APIs

Examples:

- future `GET /api/v1/audit/**` read endpoints

Authentication:

- JWT bearer token with `AUDITOR`, `OPS`, or `ADMIN` authority.
- Current local/test fallback: `X-User-Roles: AUDITOR`, `OPS`, or `ADMIN`.

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

Allowed roles:

- `SERVICE`
- `ADMIN`

## Health And Readiness

Local behavior:

- `/actuator/health` and `/actuator/health/**` are public and expose only health status.

Production behavior:

- readiness/details should remain minimal publicly or move behind `SERVICE`/`ADMIN`.
