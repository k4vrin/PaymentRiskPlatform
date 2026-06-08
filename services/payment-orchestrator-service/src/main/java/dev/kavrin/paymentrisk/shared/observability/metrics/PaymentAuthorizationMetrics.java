package dev.kavrin.paymentrisk.shared.observability.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Small facade around Micrometer for payment authorization metrics.
 *
 * <p>Using a facade keeps business services readable and prevents metric naming
 * from being duplicated across the codebase.</p>
 */
@Component
@RequiredArgsConstructor
public class PaymentAuthorizationMetrics {

    private final MeterRegistry meterRegistry;

    public void recordAuthorizationAttempt() {
        meterRegistry.counter("paymentrisk.payment.authorization.attempts").increment();
    }

    public void recordAuthorizationOutcome(String outcome) {
        meterRegistry.counter(
                "paymentrisk.payment.authorization.outcomes",
                "outcome", normalize(outcome)
        ).increment();
    }

    public void recordDuplicateIdempotencyReplay() {
        meterRegistry.counter("paymentrisk.payment.authorization.idempotency.replays").increment();
    }

    public void recordDeclineReason(String reasonCode) {
        meterRegistry.counter(
                "paymentrisk.payment.authorization.declines",
                "reason_code", normalize(reasonCode)
        ).increment();
    }

    public void recordRiskServiceLatency(Duration duration) {
        Timer.builder("paymentrisk.risk.service.duration")
                .description("Risk service call duration")
                .register(meterRegistry)
                .record(duration);
    }

    public void recordRiskTimeout() {
        meterRegistry.counter("paymentrisk.risk.service.timeouts").increment();
    }

    public void recordRiskUnavailable() {
        meterRegistry.counter("paymentrisk.risk.service.unavailable").increment();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "UNKNOWN";
        }

        return value.trim().toUpperCase();
    }
}