package dev.kavrin.paymentrisk.audit.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kavrin.paymentrisk.audit.application.PaymentAuditConsumerProperties;
import dev.kavrin.paymentrisk.audit.application.PaymentAuditProjection;
import dev.kavrin.paymentrisk.audit.application.PaymentAuditProjector;
import dev.kavrin.paymentrisk.audit.application.UnsupportedPaymentAuditEventSchemaException;
import dev.kavrin.paymentrisk.consumer.application.IdempotentConsumerGuard;
import dev.kavrin.paymentrisk.consumer.application.ProcessedMessageCommand;
import dev.kavrin.paymentrisk.consumer.application.ProcessedMessageStore;
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

class KafkaPaymentAuditConsumerTest {

    private final InMemoryProcessedMessageStore store = new InMemoryProcessedMessageStore();
    private final RecordingPaymentAuditProjector projector = new RecordingPaymentAuditProjector();
    private final KafkaPaymentAuditConsumer consumer = new KafkaPaymentAuditConsumer(
            new ObjectMapper(),
            properties(),
            new IdempotentConsumerGuard(store, transactionalOperator()),
            projector
    );

    @Test
    void shouldProjectAuthorizedDeclinedAndReversedEvents() {
        StepVerifier.create(consumer.handle(record("evt_auth", "PaymentAuthorized", 1)))
                .verifyComplete();
        StepVerifier.create(consumer.handle(record("evt_declined", "PaymentDeclined", 2)))
                .verifyComplete();
        StepVerifier.create(consumer.handle(record("evt_reversed", "PaymentReversed", 3)))
                .verifyComplete();

        assertThat(projector.projected)
                .extracting(PaymentAuditProjection::eventType)
                .containsExactly("PaymentAuthorized", "PaymentDeclined", "PaymentReversed");
        assertThat(projector.projected)
                .extracting(PaymentAuditProjection::correlationId)
                .containsOnly("corr_123");
        assertThat(projector.projected)
                .extracting(PaymentAuditProjection::occurredAt)
                .allMatch(occurredAt -> occurredAt.toString().equals("2026-06-05T10:00:00Z"));
    }

    @Test
    void shouldSkipDuplicateEvent() {
        StepVerifier.create(consumer.handle(record("evt_auth", "PaymentAuthorized", 1)))
                .verifyComplete();
        StepVerifier.create(consumer.handle(record("evt_auth", "PaymentAuthorized", 2)))
                .verifyComplete();

        assertThat(projector.projected)
                .extracting(PaymentAuditProjection::eventId)
                .containsExactly("evt_auth");
    }

    @Test
    void shouldRejectUnsupportedSchemaVersion() {
        StepVerifier.create(consumer.handle(recordWithSchema("evt_auth", "PaymentAuthorized", "2")))
                .expectError(UnsupportedPaymentAuditEventSchemaException.class)
                .verify();

        assertThat(projector.projected).isEmpty();
        assertThat(store.processed).isEmpty();
    }

    @Test
    void shouldRejectUnsupportedEventType() {
        StepVerifier.create(consumer.handle(record("evt_requested", "PaymentAuthorizationRequested", 1)))
                .expectError(IllegalArgumentException.class)
                .verify();

        assertThat(projector.projected).isEmpty();
        assertThat(store.processed).isEmpty();
    }

    private static PaymentAuditConsumerProperties properties() {
        var properties = new PaymentAuditConsumerProperties();
        properties.setConsumerName("payment-audit-consumer");
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

    private static String eventJson(
            String eventId,
            String eventType,
            String schemaVersion
    ) {
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
                  "payload": {
                    "paymentId": "pay_123",
                    "merchantId": "mer_123",
                    "customerId": "cus_123",
                    "amountMinor": 1299,
                    "currency": "USD"
                  }
                }
                """.formatted(eventId, schemaVersion, eventType);
    }

    private static final class RecordingPaymentAuditProjector implements PaymentAuditProjector {

        private final List<PaymentAuditProjection> projected = new ArrayList<>();

        @Override
        public Mono<Void> project(PaymentAuditProjection projection) {
            return Mono.fromRunnable(() -> projected.add(projection));
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
