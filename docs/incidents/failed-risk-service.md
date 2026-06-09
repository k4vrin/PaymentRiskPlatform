# Incident Write-Up: Failed Risk Service

## Summary

The Go risk scoring service becomes unavailable while merchants continue submitting payment authorization requests. The
Java orchestrator maps risk unavailability to structured downstream errors, expires started idempotency records, and
keeps raw payment data out of logs and errors.

## Impact

- New authorization attempts cannot complete risk scoring.
- Existing completed idempotency replays still return stored responses when available.
- No payment should be marked authorized or declined without a risk decision.
- Operators may see increased `503 DOWNSTREAM_UNAVAILABLE` responses.

## Detection Signals

- `paymentrisk.risk.service.unavailable` increases.
- `paymentrisk.risk.service.duration` may show elevated latency before failures.
- API `5xx` status class increases on payment authorization route.
- Logs include correlation IDs for failed authorization attempts.
- No new successful risk outcomes appear in authorization outcome metrics.

## Timeline Template

```text
T+00m Alert fires for risk unavailable metric.
T+05m Operator confirms Go service health check is failing.
T+10m Operator verifies Java API returns structured downstream errors.
T+15m Risk service restart or rollback starts.
T+20m Risk service health returns to serving.
T+25m Authorization attempts recover; metrics stabilize.
```

## Mitigation

1. Check risk service logs with the affected correlation IDs.
2. Verify gRPC port and environment variables.
3. Restart or roll back risk service.
4. Confirm Java service can reach risk gRPC endpoint.
5. Re-run a low-risk authorization smoke request.
6. Monitor idempotency replays and failed authorization metrics.

## Prevention And Follow-Up

- Add alerting on risk unavailability and API 5xx rate.
- Add deployment smoke check for gRPC health before routing traffic.
- Keep risk timeout configuration low enough to prevent request pileups.
- Add dashboard annotations for risk service deploys.
- Review dependency and network failures in post-incident notes.
