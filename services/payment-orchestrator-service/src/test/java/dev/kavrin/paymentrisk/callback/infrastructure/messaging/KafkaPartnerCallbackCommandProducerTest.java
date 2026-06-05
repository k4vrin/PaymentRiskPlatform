package dev.kavrin.paymentrisk.callback.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kavrin.paymentrisk.callback.application.PartnerCallbackCommandPublisher;
import dev.kavrin.paymentrisk.callback.application.command.CallPartnerWebhookCommand;
import dev.kavrin.paymentrisk.callback.domain.CallbackType;
import dev.kavrin.paymentrisk.callback.infrastructure.config.CallbackMessagingProperties;
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

class KafkaPartnerCallbackCommandProducerTest {

    private final InMemoryProcessedMessageStore store = new InMemoryProcessedMessageStore();
    private final RecordingPublisher publisher = new RecordingPublisher();
    private final KafkaPartnerCallbackCommandProducer producer = new KafkaPartnerCallbackCommandProducer(
            new ObjectMapper(),
            properties(),
            new IdempotentConsumerGuard(store, transactionalOperator()),
            publisher
    );

    @Test
    void shouldPublishCallbackCommandForPaymentOutcomeEvent() {
        StepVerifier.create(producer.handle(record("evt_1", "PaymentAuthorized", 1)))
                .verifyComplete();

        assertThat(publisher.commands).hasSize(1);

        var command = publisher.commands.getFirst();
        assertThat(command.paymentId()).isEqualTo("pay_123");
        assertThat(command.merchantId()).isEqualTo("mer_123");
        assertThat(command.callbackType()).isEqualTo(CallbackType.PAYMENT_AUTHORIZED);
        assertThat(command.attempt()).isZero();
        assertThat(command.correlationId()).isEqualTo("corr_123");
        assertThat(command.targetUrl()).isEqualTo("https://callbacks.example/mer_123");
    }

    @Test
    void shouldSkipDuplicateOutcomeEvent() {
        StepVerifier.create(producer.handle(record("evt_1", "PaymentAuthorized", 1)))
                .verifyComplete();
        StepVerifier.create(producer.handle(record("evt_1", "PaymentAuthorized", 2)))
                .verifyComplete();

        assertThat(publisher.commands).hasSize(1);
    }

    @Test
    void shouldRejectUnsupportedEventType() {
        StepVerifier.create(producer.handle(record("evt_1", "PaymentAuthorizationRequested", 1)))
                .expectError(IllegalArgumentException.class)
                .verify();

        assertThat(publisher.commands).isEmpty();
    }

    private static CallbackMessagingProperties properties() {
        var properties = new CallbackMessagingProperties();
        properties.setExchange("partner.callback.exchange");
        properties.setQueue("partner.callback.commands");
        properties.setDeadLetterQueue("partner.callback.commands.dlq");
        properties.setRoutingKey("partner.callback.command");
        properties.setMaxAttempts(3);
        properties.setCommandProducerConsumerName("partner-callback-command-producer");
        properties.setExpectedSchemaVersion("v1");
        properties.setTargetUrlTemplate("https://callbacks.example/{merchantId}");
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
                """
                        {
                          "eventId": "%s",
                          "schemaVersion": "v1",
                          "eventType": "%s",
                          "aggregateId": "pay_123",
                          "aggregateType": "Payment",
                          "occurredAt": "2026-06-05T10:00:00Z",
                          "producer": "payment-orchestrator-service",
                          "correlationId": "corr_123",
                          "payload": {
                            "paymentId": "pay_123",
                            "merchantId": "mer_123"
                          }
                        }
                        """.formatted(eventId, eventType)
        );
    }

    private static final class RecordingPublisher implements PartnerCallbackCommandPublisher {

        private final List<CallPartnerWebhookCommand> commands = new ArrayList<>();

        @Override
        public Mono<Void> publish(CallPartnerWebhookCommand command) {
            return Mono.fromRunnable(() -> commands.add(command));
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
