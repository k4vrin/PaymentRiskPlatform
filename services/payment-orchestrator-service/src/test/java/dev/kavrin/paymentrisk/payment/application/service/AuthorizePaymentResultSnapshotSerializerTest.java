package dev.kavrin.paymentrisk.payment.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import dev.kavrin.paymentrisk.payment.application.command.AuthorizePaymentResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthorizePaymentResultSnapshotSerializerTest {

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .findAndAddModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    private final AuthorizePaymentResultSnapshotSerializer serializer =
            new AuthorizePaymentResultSnapshotSerializer(objectMapper);

    @Test
    void shouldRoundTripAuthorizePaymentResultSnapshot() {
        AuthorizePaymentResult original = new AuthorizePaymentResult(
                "pay_01HTEST0000000000000000000",
                "AUTHORIZED",
                "AUTH123456",
                "APPROVED",
                List.of("LOW_RISK"),
                "corr_01HTEST0000000000000000000",
                12,
                "risk-rules-v1",
                Instant.parse("2026-05-26T10:15:30Z")
        );

        String json = serializer.serialize(original);
        AuthorizePaymentResult restored =
                serializer.deserialize(json, AuthorizePaymentResult.class);

        assertThat(restored).isEqualTo(original);
        assertThat(json).contains("\"paymentId\":\"pay_01HTEST0000000000000000000\"");
        assertThat(json).contains("\"status\":\"AUTHORIZED\"");
        assertThat(json).contains("\"riskDecision\":\"APPROVED\"");
    }

    @Test
    void shouldRejectUnsupportedSnapshotObjectType() {
        assertThatThrownBy(() -> serializer.serialize("not-supported"))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("Unsupported idempotency response snapshot type");
    }

    @Test
    void shouldRejectNullSnapshotObjectType() {
        assertThatThrownBy(() -> serializer.serialize(null))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("Unsupported idempotency response snapshot type: null");
    }

    @Test
    void shouldRejectUnsupportedSnapshotClassOnDeserialize() {
        assertThatThrownBy(() -> serializer.deserialize("{}", String.class))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("Unsupported idempotency response snapshot class");
    }
}