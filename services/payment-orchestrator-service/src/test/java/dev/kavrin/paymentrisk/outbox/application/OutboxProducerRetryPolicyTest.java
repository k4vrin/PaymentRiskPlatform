package dev.kavrin.paymentrisk.outbox.application;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxProducerRetryPolicyTest {

    @Test
    void shouldScheduleRetryWithExponentialBackoff() {
        var properties = properties();
        var policy = new OutboxProducerRetryPolicy(properties);

        var failedAt = Instant.parse("2026-06-05T10:00:00Z");

        var first = policy.decide(0, failedAt);
        var second = policy.decide(1, failedAt);
        var third = policy.decide(2, failedAt);

        assertThat(first.retryable()).isTrue();
        assertThat(first.nextRetryCount()).isEqualTo(1);
        assertThat(first.nextRetryAt()).isEqualTo(Instant.parse("2026-06-05T10:00:01Z"));

        assertThat(second.retryable()).isTrue();
        assertThat(second.nextRetryCount()).isEqualTo(2);
        assertThat(second.nextRetryAt()).isEqualTo(Instant.parse("2026-06-05T10:00:02Z"));

        assertThat(third.retryable()).isTrue();
        assertThat(third.nextRetryCount()).isEqualTo(3);
        assertThat(third.nextRetryAt()).isEqualTo(Instant.parse("2026-06-05T10:00:04Z"));
    }

    @Test
    void shouldCapBackoffAtMaximum() {
        var properties = properties();
        properties.setMaxBackoffMillis(3000);

        var policy = new OutboxProducerRetryPolicy(properties);

        var failedAt = Instant.parse("2026-06-05T10:00:00Z");

        var decision = policy.decide(3, failedAt);

        assertThat(decision.retryable()).isTrue();
        assertThat(decision.nextRetryCount()).isEqualTo(4);
        assertThat(decision.nextRetryAt()).isEqualTo(Instant.parse("2026-06-05T10:00:03Z"));
    }

    @Test
    void shouldReturnTerminalDecisionWhenMaxAttemptsReached() {
        var properties = properties();
        properties.setMaxAttempts(3);

        var policy = new OutboxProducerRetryPolicy(properties);

        var decision = policy.decide(
                2,
                Instant.parse("2026-06-05T10:00:00Z")
        );

        assertThat(decision.retryable()).isFalse();
        assertThat(decision.nextRetryCount()).isEqualTo(3);
        assertThat(decision.nextRetryAt()).isNull();
        assertThat(decision.failureStatus()).isEqualTo("FAILED");
    }

    private static OutboxRelayProperties properties() {
        var properties = new OutboxRelayProperties();
        properties.setEnabled(true);
        properties.setBatchSize(50);
        properties.setFixedDelayMillis(5000);
        properties.setMaxAttempts(5);
        properties.setInitialBackoffMillis(1000);
        properties.setMaxBackoffMillis(60000);
        return properties;
    }
}
