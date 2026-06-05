package dev.kavrin.paymentrisk.shared.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventEnvelopeTest {

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    @Test
    void shouldSerializeEnvelopeWithRequiredFields() throws Exception {
        var envelope = new EventEnvelope<>(
                "evt_123",
                "v1",
                "PaymentAuthorized",
                "pay_123",
                "Payment",
                Instant.parse("2026-06-05T10:00:00Z"),
                "payment-orchestrator-service",
                "corr_123",
                new TestPayload("AUTHORIZED")
        );

        var json = objectMapper.writeValueAsString(envelope);
        JsonNode root = objectMapper.readTree(json);

        assertThat(root.get("eventId").asText()).isEqualTo("evt_123");
        assertThat(root.get("schemaVersion").asText()).isEqualTo("v1");
        assertThat(root.get("eventType").asText()).isEqualTo("PaymentAuthorized");
        assertThat(root.get("aggregateId").asText()).isEqualTo("pay_123");
        assertThat(root.get("aggregateType").asText()).isEqualTo("Payment");
        assertThat(root.get("occurredAt").asText()).isEqualTo("2026-06-05T10:00:00Z");
        assertThat(root.get("producer").asText()).isEqualTo("payment-orchestrator-service");
        assertThat(root.get("correlationId").asText()).isEqualTo("corr_123");
        assertThat(root.get("payload").get("status").asText()).isEqualTo("AUTHORIZED");
    }

    @ParameterizedTest
    @MethodSource("missingTextFields")
    void shouldRejectMissingRequiredTextFields(
            String fieldName,
            Function<EventEnvelopeBuilder, EventEnvelopeBuilder> mutation
    ) {
        assertThatThrownBy(() -> mutation.apply(validEnvelope()).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(fieldName + " is required");
    }

    @Test
    void shouldRejectMissingPayload() {
        assertThatThrownBy(() -> new EventEnvelope<>(
                "evt_123",
                "v1",
                "PaymentAuthorized",
                "pay_123",
                "Payment",
                Instant.parse("2026-06-05T10:00:00Z"),
                "payment-orchestrator-service",
                "corr_123",
                null
        )).isInstanceOf(NullPointerException.class)
                .hasMessage("payload is required");
    }

    @Test
    void shouldRejectMissingOccurredAt() {
        assertThatThrownBy(() -> new EventEnvelope<>(
                "evt_123",
                "v1",
                "PaymentAuthorized",
                "pay_123",
                "Payment",
                null,
                "payment-orchestrator-service",
                "corr_123",
                new TestPayload("AUTHORIZED")
        )).isInstanceOf(NullPointerException.class)
                .hasMessage("occurredAt is required");
    }

    private record TestPayload(String status) {
    }

    private static Stream<Arguments> missingTextFields() {
        return Stream.of(
                Arguments.of("eventId", mutate(builder -> builder.eventId = "")),
                Arguments.of("schemaVersion", mutate(builder -> builder.schemaVersion = " ")),
                Arguments.of("eventType", mutate(builder -> builder.eventType = null)),
                Arguments.of("aggregateId", mutate(builder -> builder.aggregateId = "")),
                Arguments.of("aggregateType", mutate(builder -> builder.aggregateType = " ")),
                Arguments.of("producer", mutate(builder -> builder.producer = null)),
                Arguments.of("correlationId", mutate(builder -> builder.correlationId = ""))
        );
    }

    private static Function<EventEnvelopeBuilder, EventEnvelopeBuilder> mutate(
            java.util.function.Consumer<EventEnvelopeBuilder> mutation
    ) {
        return builder -> {
            mutation.accept(builder);
            return builder;
        };
    }

    private static EventEnvelopeBuilder validEnvelope() {
        return new EventEnvelopeBuilder();
    }

    private static final class EventEnvelopeBuilder {

        private String eventId = "evt_123";
        private String schemaVersion = "v1";
        private String eventType = "PaymentAuthorized";
        private String aggregateId = "pay_123";
        private String aggregateType = "Payment";
        private Instant occurredAt = Instant.parse("2026-06-05T10:00:00Z");
        private String producer = "payment-orchestrator-service";
        private String correlationId = "corr_123";
        private TestPayload payload = new TestPayload("AUTHORIZED");

        private EventEnvelope<TestPayload> build() {
            return new EventEnvelope<>(
                    eventId,
                    schemaVersion,
                    eventType,
                    aggregateId,
                    aggregateType,
                    occurredAt,
                    producer,
                    correlationId,
                    payload
            );
        }
    }
}
