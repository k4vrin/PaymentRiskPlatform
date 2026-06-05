package dev.kavrin.paymentrisk.ops.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kavrin.paymentrisk.consumer.application.IdempotentConsumerGuard;
import dev.kavrin.paymentrisk.consumer.application.KafkaConsumerFailureHandler;
import dev.kavrin.paymentrisk.consumer.application.ProcessedMessageCommand;
import dev.kavrin.paymentrisk.consumer.application.ProcessedMessageStore;
import dev.kavrin.paymentrisk.ops.application.metrics.OpsMetricsConsumerProperties;
import dev.kavrin.paymentrisk.ops.application.metrics.OpsMetricsEvent;
import dev.kavrin.paymentrisk.ops.application.metrics.OpsMetricsProjector;
import dev.kavrin.paymentrisk.ops.application.metrics.UnsupportedOpsMetricsEventSchemaException;
import dev.kavrin.paymentrisk.shared.messaging.MessagingObservability;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KafkaOpsMetricsConsumerTest {

    private final InMemoryProcessedMessageStore store = new InMemoryProcessedMessageStore();
    private final RecordingOpsMetricsProjector projector = new RecordingOpsMetricsProjector();
    private final KafkaOpsMetricsConsumer consumer = new KafkaOpsMetricsConsumer(
            new ObjectMapper(),
            properties(),
            new IdempotentConsumerGuard(store, transactionalOperator()),
            projector,
            mock(KafkaConsumerFailureHandler.class),
            new MessagingObservability(new SimpleMeterRegistry())
    );

    @Test
    void shouldProjectSelectedPaymentAndPlatformEvents() {
        StepVerifier.create(consumer.handle(record("evt_auth", "PaymentAuthorized", 1)))
                .verifyComplete();
        StepVerifier.create(consumer.handle(record("evt_dlq", "DeadLetterRecorded", 2)))
                .verifyComplete();

        assertThat(projector.projected)
                .extracting(OpsMetricsEvent::eventType)
                .containsExactly("PaymentAuthorized", "DeadLetterRecorded");
    }

    @Test
    void shouldSkipDuplicateEvent() {
        StepVerifier.create(consumer.handle(record("evt_auth", "PaymentAuthorized", 1)))
                .verifyComplete();
        StepVerifier.create(consumer.handle(record("evt_auth", "PaymentAuthorized", 2)))
                .verifyComplete();

        assertThat(projector.projected)
                .extracting(OpsMetricsEvent::eventId)
                .containsExactly("evt_auth");
    }

    @Test
    void shouldRejectUnsupportedSchemaVersion() {
        StepVerifier.create(consumer.handle(recordWithSchema("evt_auth", "PaymentAuthorized", "2")))
                .expectError(UnsupportedOpsMetricsEventSchemaException.class)
                .verify();

        assertThat(projector.projected).isEmpty();
    }

    private static OpsMetricsConsumerProperties properties() {
        var properties = new OpsMetricsConsumerProperties();
        properties.setConsumerName("ops-metrics-consumer");
        properties.setExpectedSchemaVersion("v1");
        return properties;
    }

    private static TransactionalOperator transactionalOperator() {
        var transactionalOperator = mock(TransactionalOperator.class);
        when(transactionalOperator.transactional(any(Mono.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        return transactionalOperator;
    }

    private static ConsumerRecord<String, String> record(
            String eventId,
            String eventType,
            long offset
    ) {
        return new ConsumerRecord<>(
                "payment.authorization.completed",
                0,
                offset,
                "pay_123",
                eventJson(eventId, eventType, "v1")
        );
    }

    private static ConsumerRecord<String, String> recordWithSchema(
            String eventId,
            String eventType,
            String schemaVersion
    ) {
        return new ConsumerRecord<>(
                "payment.authorization.completed",
                0,
                1,
                "pay_123",
                eventJson(eventId, eventType, schemaVersion)
        );
    }

    private static String eventJson(String eventId, String eventType, String schemaVersion) {
        return """
                {
                  "eventId": "%s",
                  "schemaVersion": "%s",
                  "eventType": "%s",
                  "aggregateId": "pay_123",
                  "aggregateType": "Payment",
                  "occurredAt": "2026-06-05T10:00:00Z",
                  "producer": "payment-orchestrator-service",
                  "correlationId": "corr_123",
                  "payload": {"paymentId": "pay_123"}
                }
                """.formatted(eventId, schemaVersion, eventType);
    }

    private static final class RecordingOpsMetricsProjector implements OpsMetricsProjector {

        private final List<OpsMetricsEvent> projected = new ArrayList<>();

        @Override
        public Mono<Void> project(OpsMetricsEvent event) {
            return Mono.fromRunnable(() -> projected.add(event));
        }
    }

    private static final class InMemoryProcessedMessageStore implements ProcessedMessageStore {

        private final Set<String> processed = new HashSet<>();

        @Override
        public Mono<Boolean> isProcessed(String consumerName, String eventId) {
            return Mono.just(processed.contains(key(consumerName, eventId)));
        }

        @Override
        public Mono<Boolean> recordProcessed(ProcessedMessageCommand command) {
            return Mono.just(processed.add(key(command.consumerName(), command.eventId())));
        }

        private static String key(String consumerName, String eventId) {
            return consumerName + ":" + eventId;
        }
    }
}
