package dev.kavrin.paymentrisk.shared.observability.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentAuthorizationMetricsTest {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final PaymentAuthorizationMetrics metrics = new PaymentAuthorizationMetrics(meterRegistry);

    @Test
    void recordsAuthorizationAttempt() {
        metrics.recordAuthorizationAttempt();

        assertThat(meterRegistry.counter("paymentrisk.payment.authorization.attempts").count())
                .isEqualTo(1.0);
    }

    @Test
    void recordsAuthorizationOutcome() {
        metrics.recordAuthorizationOutcome("AUTHORIZED");

        assertThat(meterRegistry.counter(
                "paymentrisk.payment.authorization.outcomes",
                "outcome", "AUTHORIZED"
        ).count()).isEqualTo(1.0);
    }

    @Test
    void recordsDuplicateIdempotencyReplay() {
        metrics.recordDuplicateIdempotencyReplay();

        assertThat(meterRegistry.counter("paymentrisk.payment.authorization.idempotency.replays").count())
                .isEqualTo(1.0);
    }

    @Test
    void recordsDeclineReason() {
        metrics.recordDeclineReason("HIGH_AMOUNT");

        assertThat(meterRegistry.counter(
                "paymentrisk.payment.authorization.declines",
                "reason_code", "HIGH_AMOUNT"
        ).count()).isEqualTo(1.0);
    }

    @Test
    void recordsRiskServiceLatency() {
        metrics.recordRiskServiceLatency(Duration.ofMillis(120));

        var timer = meterRegistry.timer("paymentrisk.risk.service.duration");

        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isEqualTo(120.0);
    }

    @Test
    void recordsRiskTimeout() {
        metrics.recordRiskTimeout();

        assertThat(meterRegistry.counter("paymentrisk.risk.service.timeouts").count())
                .isEqualTo(1.0);
    }

    @Test
    void recordsRiskUnavailable() {
        metrics.recordRiskUnavailable();

        assertThat(meterRegistry.counter("paymentrisk.risk.service.unavailable").count())
                .isEqualTo(1.0);
    }
}