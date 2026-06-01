package dev.kavrin.paymentrisk.payment.application.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentOutboxPayloadSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Test
    void serializesPaymentAuthorizationRequestedPayload() throws Exception {
        var payload = PaymentAuthorizationRequestedPayload.v1(
                "pay_123",
                "merchant_123",
                "customer_123",
                10_000,
                "USD",
                "order_123",
                Instant.parse("2026-06-01T10:00:00Z")
        );

        String json = objectMapper.writeValueAsString(payload);

        assertThat(json).contains("\"schemaVersion\":\"v1\"");
        assertThat(json).contains("\"paymentId\":\"pay_123\"");
        assertThat(json).contains("\"merchantId\":\"merchant_123\"");
        assertThat(json).contains("\"amountMinor\":10000");
    }

    @Test
    void serializesPaymentAuthorizedPayload() throws Exception {
        var payload = PaymentAuthorizedPayload.v1(
                "pay_123",
                "merchant_123",
                "customer_123",
                10_000,
                "USD",
                "auth_123",
                12,
                List.of("LOW_RISK"),
                "rules-v1",
                Instant.parse("2026-06-01T10:00:01Z")
        );

        String json = objectMapper.writeValueAsString(payload);

        assertThat(json).contains("\"schemaVersion\":\"v1\"");
        assertThat(json).contains("\"authorizationCode\":\"auth_123\"");
        assertThat(json).contains("\"riskScore\":12");
        assertThat(json).contains("\"reasonCodes\":[\"LOW_RISK\"]");
    }

    @Test
    void serializesPaymentDeclinedPayload() throws Exception {
        var payload = PaymentDeclinedPayload.v1(
                "pay_123",
                "merchant_123",
                "customer_123",
                10_000,
                "USD",
                91,
                List.of("HIGH_RISK"),
                "rules-v1",
                Instant.parse("2026-06-01T10:00:02Z")
        );

        String json = objectMapper.writeValueAsString(payload);

        assertThat(json).contains("\"schemaVersion\":\"v1\"");
        assertThat(json).contains("\"riskScore\":91");
        assertThat(json).contains("\"reasonCodes\":[\"HIGH_RISK\"]");
        assertThat(json).doesNotContain("authorizationCode");
    }
}
