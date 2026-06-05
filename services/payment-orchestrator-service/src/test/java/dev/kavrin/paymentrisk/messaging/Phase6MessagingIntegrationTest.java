package dev.kavrin.paymentrisk.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.ConnectionFactory;
import dev.kavrin.paymentrisk.TestPostgresConfiguration;
import dev.kavrin.paymentrisk.audit.application.PaymentAuditConsumerProperties;
import dev.kavrin.paymentrisk.audit.infrastructure.messaging.KafkaPaymentAuditConsumer;
import dev.kavrin.paymentrisk.audit.infrastructure.persistence.PaymentAuditEventRepository;
import dev.kavrin.paymentrisk.callback.application.PartnerWebhookClient;
import dev.kavrin.paymentrisk.callback.application.command.CallPartnerWebhookCommand;
import dev.kavrin.paymentrisk.callback.domain.CallbackType;
import dev.kavrin.paymentrisk.callback.infrastructure.config.CallbackMessagingProperties;
import dev.kavrin.paymentrisk.callback.infrastructure.messaging.PartnerCallbackWorker;
import dev.kavrin.paymentrisk.consumer.application.IdempotentConsumerGuard;
import dev.kavrin.paymentrisk.consumer.infrastructure.persistence.DatabaseKafkaDeadLetterRecorder;
import dev.kavrin.paymentrisk.consumer.infrastructure.persistence.ProcessedKafkaMessageEntityRepository;
import dev.kavrin.paymentrisk.ops.infrastructure.deadletter.persistence.DeadLetterRecordEntityRepository;
import dev.kavrin.paymentrisk.outbox.domain.OutboxEvent;
import dev.kavrin.paymentrisk.outbox.infrastructure.messaging.KafkaOutboxEventPublisher;
import dev.kavrin.paymentrisk.outbox.infrastructure.messaging.SpringKafkaRecordSender;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.repository.OutboxEventEntityRepository;
import dev.kavrin.paymentrisk.shared.messaging.KafkaTopicProperties;
import dev.kavrin.paymentrisk.shared.messaging.MessagingObservability;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration,"
                + "org.springframework.boot.data.redis.autoconfigure.DataRedisReactiveAutoConfiguration,"
                + "org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration,"
                + "org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration,"
                + "org.springframework.boot.security.autoconfigure.web.reactive.ReactiveWebSecurityAutoConfiguration,"
                + "org.springframework.boot.security.autoconfigure.actuate.web.reactive.ReactiveManagementWebSecurityAutoConfiguration"
})
@ActiveProfiles("test")
@Import(TestPostgresConfiguration.class)
class Phase6MessagingIntegrationTest {

    @Container
    private static final KafkaContainer KAFKA = new KafkaContainer(
            DockerImageName.parse("apache/kafka-native:4.3.0")
    );

    @Container
    private static final RabbitMQContainer RABBIT = new RabbitMQContainer(
            DockerImageName.parse("rabbitmq:4.3.0-management")
    );

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentAuditConsumerProperties auditProperties;

    @Autowired
    private IdempotentConsumerGuard consumerGuard;

    @Autowired
    private dev.kavrin.paymentrisk.audit.application.PaymentAuditProjector auditProjector;

    @Autowired
    private PaymentAuditEventRepository auditRepository;

    @Autowired
    private ProcessedKafkaMessageEntityRepository processedRepository;

    @Autowired
    private DatabaseKafkaDeadLetterRecorder deadLetterRecorder;

    @Autowired
    private DeadLetterRecordEntityRepository deadLetterRepository;

    @Autowired
    private OutboxEventEntityRepository outboxRepository;

    @Autowired
    private MeterRegistry meterRegistry;

    @BeforeEach
    void deleteExistingRecords() {
        outboxRepository.deleteAll().block();
        deadLetterRepository.deleteAll().block();
        auditRepository.deleteAll().block();
        processedRepository.deleteAll().block();
        meterRegistry.clear();
    }

    @Test
    void shouldPublishOutboxEnvelopeThroughKafkaContainer() throws Exception {
        var topic = topicName("outbox-publish");
        createKafkaTopic(topic);

        var template = kafkaTemplate();
        var publisher = new KafkaOutboxEventPublisher(
                objectMapper,
                kafkaTopics(topic),
                new SpringKafkaRecordSender(template)
        );

        StepVerifier.create(publisher.publish(outboxEvent()))
                .verifyComplete();

        var published = pollSingleRecord(topic);

        assertThat(published.key()).isEqualTo("pay_123");
        assertThat(published.value()).contains("\"eventId\":\"evt_phase6\"");
        assertThat(published.value()).contains("\"schemaVersion\":\"v1\"");
        assertThat(published.value()).contains("\"eventType\":\"PaymentAuthorized\"");
        assertThat(published.value()).contains("\"payload\":{\"paymentId\":\"pay_123\"");
        assertThat(header(published, "correlation_id")).isEqualTo("corr_phase6");
    }

    @Test
    void shouldConsumeKafkaProjectionOnceAndPersistDeadLetterRecord() throws Exception {
        var topic = topicName("audit-projection");
        createKafkaTopic(topic);

        kafkaTemplate().send(topic, "pay_123", paymentAuthorizedEnvelope("evt_phase6_projection"))
                .get();

        var firstDelivery = pollSingleRecord(topic);
        var auditConsumer = new KafkaPaymentAuditConsumer(
                objectMapper,
                auditProperties,
                consumerGuard,
                auditProjector
        );

        auditConsumer.consume(firstDelivery);
        auditConsumer.consume(new ConsumerRecord<>(
                firstDelivery.topic(),
                firstDelivery.partition(),
                firstDelivery.offset() + 1,
                firstDelivery.key(),
                firstDelivery.value()
        ));

        StepVerifier.create(auditRepository.findByPaymentIdOrderByOccurredAtAsc("pay_123").collectList())
                .assertNext(events -> {
                    assertThat(events).hasSize(1);
                    assertThat(events.getFirst().eventId()).isEqualTo("evt_phase6_projection");
                    assertThat(events.getFirst().schemaVersion()).isEqualTo("v1");
                })
                .verifyComplete();

        StepVerifier.create(processedRepository.findAll().collectList())
                .assertNext(processed -> assertThat(processed).hasSize(1))
                .verifyComplete();

        var poison = new ConsumerRecord<>(
                topic,
                0,
                99,
                "pay_123",
                "{bad-json"
        );
        poison.headers()
                .add("event_id", bytes("evt_phase6_poison"))
                .add("correlation_id", bytes("corr_phase6"));

        StepVerifier.create(deadLetterRecorder.record(
                        "payment-audit-consumer",
                        poison,
                        new IllegalArgumentException("bad payload")
                ))
                .verifyComplete();

        StepVerifier.create(deadLetterRepository.findByEventId("evt_phase6_poison").collectList())
                .assertNext(deadLetters -> {
                    assertThat(deadLetters).hasSize(1);
                    assertThat(deadLetters.getFirst().getDestinationName()).isEqualTo(topic);
                })
                .verifyComplete();
    }

    @Test
    void shouldDeliverCallbackCommandThroughRabbitContainer() throws Exception {
        var properties = callbackProperties();
        var rabbitTemplate = rabbitTemplate(properties);
        var webhookClient = new RecordingWebhookClient();
        var worker = new PartnerCallbackWorker(
                objectMapper,
                webhookClient,
                properties,
                new MessagingObservability(meterRegistry)
        );

        var command = new CallPartnerWebhookCommand(
                "pay_123",
                "mer_123",
                "https://partner.example/callback",
                CallbackType.PAYMENT_AUTHORIZED,
                0,
                "corr_phase6"
        );

        rabbitTemplate.convertAndSend(
                properties.getExchange(),
                properties.getRoutingKey(),
                objectMapper.writeValueAsString(command)
        );

        var message = (String) rabbitTemplate.receiveAndConvert(properties.getQueue(), 10_000);
        assertThat(message).isNotBlank();

        worker.consume(message);

        assertThat(webhookClient.commands)
                .extracting(CallPartnerWebhookCommand::paymentId)
                .containsExactly("pay_123");
    }

    private KafkaTemplate<String, String> kafkaTemplate() {
        var producerFactory = new DefaultKafkaProducerFactory<String, String>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class
        ));

        return new KafkaTemplate<>(producerFactory);
    }

    private ConsumerRecord<String, String> pollSingleRecord(String topic) {
        try (var consumer = new KafkaConsumer<String, String>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "phase6-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class
        ))) {
            consumer.subscribe(List.of(topic));

            var deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
            while (System.nanoTime() < deadline) {
                var records = consumer.poll(Duration.ofMillis(250));
                if (!records.isEmpty()) {
                    return records.iterator().next();
                }
            }
        }

        throw new AssertionError("Timed out waiting for Kafka record on topic " + topic);
    }

    private void createKafkaTopic(String topic) throws Exception {
        try (var admin = AdminClient.create(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers()
        ))) {
            admin.createTopics(List.of(new NewTopic(topic, 1, (short) 1)))
                    .all()
                    .get();
        }
    }

    private KafkaTopicProperties kafkaTopics(String authorizationCompletedTopic) {
        return new KafkaTopicProperties(
                new KafkaTopicProperties.Topics(
                        topicName("authorization-requested"),
                        topicName("risk-score-completed"),
                        authorizationCompletedTopic,
                        topicName("reversal-completed"),
                        topicName("dead-letter-recorded")
                ),
                null
        );
    }

    private RabbitTemplate rabbitTemplate(CallbackMessagingProperties properties) throws Exception {
        var connectionFactory = rabbitConnectionFactory();
        var admin = new RabbitAdmin(connectionFactory);
        var exchange = new DirectExchange(properties.getExchange());
        var queue = new Queue(properties.getQueue(), true);
        var dlq = new Queue(properties.getDeadLetterQueue(), true);

        admin.declareExchange(exchange);
        admin.declareQueue(queue);
        admin.declareQueue(dlq);
        admin.declareBinding(BindingBuilder.bind(queue).to(exchange).with(properties.getRoutingKey()));

        return new RabbitTemplate(connectionFactory);
    }

    private CachingConnectionFactory rabbitConnectionFactory() throws Exception {
        var factory = new ConnectionFactory();
        factory.setHost(RABBIT.getHost());
        factory.setPort(RABBIT.getAmqpPort());
        factory.setUsername(RABBIT.getAdminUsername());
        factory.setPassword(RABBIT.getAdminPassword());
        return new CachingConnectionFactory(factory);
    }

    private static CallbackMessagingProperties callbackProperties() {
        var properties = new CallbackMessagingProperties();
        var suffix = UUID.randomUUID();
        properties.setExchange("phase6.callback.exchange." + suffix);
        properties.setQueue("phase6.callback.commands." + suffix);
        properties.setDeadLetterQueue("phase6.callback.commands.dlq." + suffix);
        properties.setRoutingKey("phase6.callback.command");
        properties.setMaxAttempts(3);
        return properties;
    }

    private static OutboxEvent outboxEvent() {
        var now = Instant.parse("2026-06-05T08:00:00Z");
        return new OutboxEvent(
                "evt_phase6",
                "PAYMENT",
                "pay_123",
                "PaymentAuthorized",
                "v1",
                "payment-orchestrator-service",
                "corr_phase6",
                "{\"paymentId\":\"pay_123\",\"merchantId\":\"mer_123\",\"amountMinor\":1299,\"currency\":\"USD\"}",
                "PUBLISHING",
                0,
                null,
                null,
                now,
                now,
                null,
                now,
                "relay-1"
        );
    }

    private static String paymentAuthorizedEnvelope(String eventId) {
        return """
                {
                  "eventId": "%s",
                  "schemaVersion": "v1",
                  "eventType": "PaymentAuthorized",
                  "aggregateId": "pay_123",
                  "aggregateType": "PAYMENT",
                  "occurredAt": "2026-06-05T10:00:00Z",
                  "producer": "payment-orchestrator-service",
                  "correlationId": "corr_phase6",
                  "payload": {
                    "paymentId": "pay_123",
                    "merchantId": "mer_123",
                    "customerId": "cus_123",
                    "amountMinor": 1299,
                    "currency": "USD"
                  }
                }
                """.formatted(eventId);
    }

    private static String topicName(String suffix) {
        return "phase6." + suffix + "." + UUID.randomUUID();
    }

    private static String header(ConsumerRecord<String, String> record, String name) {
        return new String(record.headers().lastHeader(name).value(), StandardCharsets.UTF_8);
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static final class RecordingWebhookClient implements PartnerWebhookClient {

        private final List<CallPartnerWebhookCommand> commands = new java.util.ArrayList<>();

        @Override
        public Mono<Void> call(CallPartnerWebhookCommand command) {
            commands.add(command);
            return Mono.empty();
        }
    }
}
