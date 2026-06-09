# Linux Operations Runbook

## Local Startup And Shutdown

Start local infrastructure:

```bash
make platform-up
make platform-ps
```

Start services:

```bash
make risk-run
make spring-run
```

Shutdown:

```bash
make platform-down
```

## Logs

Platform logs:

```bash
make platform-logs
docker compose -f platform/compose.local.yaml logs -f postgres redis kafka rabbitmq prometheus grafana
```

Service logs:

```bash
cd services/payment-orchestrator-service && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
cd services/risk-scoring-service && go run ./cmd/risk-scoring-service
```

Always search by `X-Correlation-Id` or event ID. Do not paste API keys, bearer tokens, payment method tokens, or device
fingerprints into shared tickets.

## Database Checks

```bash
docker exec -it payment-risk-postgres psql -U payment_risk -d payment_risk
```

Useful SQL:

```sql
select status, count(*) from payments group by status;
select status, count(*) from outbox_events group by status;
select status, count(*) from idempotency_records group by status;
select status, count(*) from dead_letter_records group by status;
select status, count(*) from ops_replay_jobs group by status;
```

## Redis Checks

```bash
docker exec -it payment-risk-redis redis-cli ping
docker exec -it payment-risk-redis redis-cli scan 0 match 'idempotency:*' count 20
docker exec -it payment-risk-redis redis-cli scan 0 match 'rate-limit:*' count 20
```

Expected signals:

- idempotency snapshots should have TTLs;
- rate-limit keys should expire within the configured window;
- Redis misses should fall back to database idempotency records.

## Kafka Checks

```bash
docker exec -it payment-risk-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
docker exec -it payment-risk-kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --all-groups --describe
```

Watch:

- outbox rows stuck in `FAILED`;
- consumer lag increasing;
- repeated dead-letter records for the same topic or event type.

## RabbitMQ Checks

Open management UI:

```text
http://localhost:15672
```

Credentials:

```text
payment_risk / payment_risk
```

Check queue depth for callback command queues and DLQs. Rising DLQ depth usually means partner endpoint failures or
payload/contract drift.

## Replay And Dead-Letter Workflows

Inspect failed outbox:

```bash
curl -H 'Authorization: Bearer <ops-jwt>' http://localhost:8080/api/v1/ops/outbox?status=FAILED
```

Inspect dead letters:

```bash
curl -H 'Authorization: Bearer <ops-jwt>' http://localhost:8080/api/v1/ops/dead-letters
```

Request replay:

```bash
curl -X POST \
  -H 'Authorization: Bearer <ops-jwt>' \
  -H 'Content-Type: application/json' \
  -d '{"reason":"manual retry after dependency recovery"}' \
  http://localhost:8080/api/v1/ops/replay/OUTBOX/<event-id>
```

After replay request:

- verify an `ops_replay_jobs` row exists;
- verify replay audit outbox event exists;
- monitor `payment_risk_replay_requests_total`;
- verify original failed row moves through the expected retry/replay path when replay execution is enabled.

## Metrics And Dashboards

Prometheus:

```text
http://localhost:9090
```

Grafana:

```text
http://localhost:3000
admin / admin
```

Key dashboards:

- API health;
- authorization;
- risk service;
- Redis/idempotency;
- Kafka/outbox;
- database health.
