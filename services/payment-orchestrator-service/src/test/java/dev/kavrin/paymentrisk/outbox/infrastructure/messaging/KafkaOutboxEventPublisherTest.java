package dev.kavrin.paymentrisk.outbox.infrastructure.messaging;

import dev.kavrin.paymentrisk.outbox.domain.OutboxEvent;
import dev.kavrin.paymentrisk.shared.messaging.KafkaTopicProperties;
import dev.kavrin.paymentrisk.shared.messaging.KafkaTopics;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KafkaOutboxEventPublisherTest {

    private final CapturingSender sender = new CapturingSender();
    private final KafkaOutboxEventPublisher publisher =
            new KafkaOutboxEventPublisher(new KafkaTopicProperties(null, null), sender);

    @Test
    void shouldPublishPayloadToMappedTopicWithAggregateKeyAndHeaders() {
        var event = event("PaymentAuthorized", "{\"paymentId\":\"pay_123\"}");

        publisher.publish(event).block();

        ProducerRecord<String, String> record = sender.record;

        assertThat(record.topic()).isEqualTo(KafkaTopics.PAYMENT_AUTHORIZATION_COMPLETED);
        assertThat(record.key()).isEqualTo("pay_123");
        assertThat(record.value()).isEqualTo("{\"paymentId\":\"pay_123\"}");
        assertThat(header(record, "event_id")).isEqualTo("evt_123");
        assertThat(header(record, "event_type")).isEqualTo("PaymentAuthorized");
        assertThat(header(record, "schema_version")).isEqualTo("v1");
        assertThat(header(record, "correlation_id")).isEqualTo("corr_123");
    }

    @Test
    void shouldRejectUnsupportedEventType() {
        assertThatThrownBy(() -> publisher.publish(event("UnsupportedEvent", "{}")).block())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported outbox event type: UnsupportedEvent");
    }

    private static String header(ProducerRecord<String, String> record, String name) {
        return new String(record.headers().lastHeader(name).value());
    }

    private static OutboxEvent event(String eventType, String payloadJson) {
        var now = Instant.parse("2026-06-05T08:00:00Z");
        return new OutboxEvent(
                "evt_123",
                "PAYMENT",
                "pay_123",
                eventType,
                "v1",
                "payment-orchestrator-service",
                "corr_123",
                payloadJson,
                "PUBLISHING",
                0,
                now,
                null,
                now,
                now,
                null,
                now,
                "relay-1"
        );
    }

    private static final class CapturingSender implements KafkaRecordSender {

        private ProducerRecord<String, String> record;

        @Override
        public Mono<Void> send(ProducerRecord<String, String> record) {
            this.record = record;
            return Mono.empty();
        }
    }
}
