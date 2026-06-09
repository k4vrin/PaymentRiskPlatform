package dev.kavrin.paymentrisk.shared.messaging;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class MessagingObservability {

    private final MeterRegistry meterRegistry;

    public void recordOutboxPublished(String eventType) {
        increment("payment_risk_outbox_publish_total", "result", "success", "event_type", eventType);
    }

    public void recordOutboxLag(String eventType, Instant createdAt, Instant observedAt) {
        DistributionSummary.builder("payment_risk_outbox_lag_seconds")
                .tag("event_type", eventType)
                .register(meterRegistry)
                .record(Math.max(0, Duration.between(createdAt, observedAt).toMillis() / 1000.0));
    }

    public void recordOutboxPublishFailure(String eventType) {
        increment("payment_risk_outbox_publish_total", "result", "failure", "event_type", eventType);
    }

    public void recordConsumerProcessed(String consumerName, String eventType) {
        increment("payment_risk_consumer_events_total", "consumer", consumerName, "event_type", eventType, "result", "processed");
    }

    public void recordConsumerSkipped(String consumerName, String eventType) {
        increment("payment_risk_consumer_events_total", "consumer", consumerName, "event_type", eventType, "result", "duplicate");
    }

    public void recordConsumerLag(String consumerName, String topic, long lag) {
        DistributionSummary.builder("payment_risk_consumer_lag_records")
                .tag("consumer", normalize(consumerName))
                .tag("topic", normalize(topic))
                .register(meterRegistry)
                .record(Math.max(0, lag));
    }

    public void recordDeadLetter(String sourceSystem) {
        increment("payment_risk_dead_letters_total", "source", sourceSystem);
    }

    public void recordReplaySuccess(String source) {
        increment("payment_risk_replay_requests_total", "source", normalize(source), "result", "success");
    }

    public void recordReplayFailure(String source) {
        increment("payment_risk_replay_requests_total", "source", normalize(source), "result", "failure");
    }

    public void recordCallbackSuccess(String callbackType) {
        increment("payment_risk_partner_callback_total", "callback_type", callbackType, "result", "success");
    }

    public void recordCallbackFailure(String callbackType) {
        increment("payment_risk_partner_callback_total", "callback_type", callbackType, "result", "failure");
    }

    private void increment(String meterName, String... tags) {
        Counter.builder(meterName)
                .tags(tags)
                .register(meterRegistry)
                .increment();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "UNKNOWN";
        }

        return value.trim().toUpperCase();
    }
}
