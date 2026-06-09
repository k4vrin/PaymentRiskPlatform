package dev.kavrin.paymentrisk.shared.messaging;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class MessagingObservabilityTest {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final MessagingObservability observability = new MessagingObservability(meterRegistry);

    @Test
    void recordsProducerAndOutboxMetrics() {
        observability.recordOutboxPublished("PaymentAuthorized");
        observability.recordOutboxPublishFailure("PaymentAuthorized");
        observability.recordOutboxLag(
                "PaymentAuthorized",
                Instant.parse("2026-06-08T10:00:00Z"),
                Instant.parse("2026-06-08T10:00:05Z")
        );

        assertThat(meterRegistry.counter(
                "payment_risk_outbox_publish_total",
                "result", "success",
                "event_type", "PaymentAuthorized"
        ).count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter(
                "payment_risk_outbox_publish_total",
                "result", "failure",
                "event_type", "PaymentAuthorized"
        ).count()).isEqualTo(1.0);
        assertThat(meterRegistry.summary(
                "payment_risk_outbox_lag_seconds",
                "event_type", "PaymentAuthorized"
        ).totalAmount()).isEqualTo(5.0);
    }

    @Test
    void recordsConsumerDeadLetterReplayAndCallbackMetrics() {
        observability.recordConsumerProcessed("audit-consumer", "PaymentAuthorized");
        observability.recordConsumerSkipped("audit-consumer", "PaymentAuthorized");
        observability.recordConsumerLag("audit-consumer", "payment.authorization.completed", 42);
        observability.recordDeadLetter("KAFKA");
        observability.recordReplaySuccess("OUTBOX");
        observability.recordReplayFailure("DEAD_LETTER");
        observability.recordCallbackSuccess("PAYMENT_AUTHORIZED");
        observability.recordCallbackFailure("PAYMENT_AUTHORIZED");

        assertThat(meterRegistry.counter(
                "payment_risk_consumer_events_total",
                "consumer", "audit-consumer",
                "event_type", "PaymentAuthorized",
                "result", "processed"
        ).count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter("payment_risk_dead_letters_total", "source", "KAFKA").count())
                .isEqualTo(1.0);
        assertThat(meterRegistry.counter(
                "payment_risk_replay_requests_total",
                "source", "OUTBOX",
                "result", "success"
        ).count()).isEqualTo(1.0);
        assertThat(meterRegistry.summary(
                "payment_risk_consumer_lag_records",
                "consumer", "AUDIT-CONSUMER",
                "topic", "PAYMENT.AUTHORIZATION.COMPLETED"
        ).totalAmount()).isEqualTo(42.0);
        assertThat(meterRegistry.counter(
                "payment_risk_partner_callback_total",
                "callback_type", "PAYMENT_AUTHORIZED",
                "result", "failure"
        ).count()).isEqualTo(1.0);
    }
}
