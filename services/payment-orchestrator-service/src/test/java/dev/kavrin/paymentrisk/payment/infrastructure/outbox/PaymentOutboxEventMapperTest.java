package dev.kavrin.paymentrisk.payment.infrastructure.outbox;

import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKey;
import dev.kavrin.paymentrisk.payment.application.outbox.PaymentAuthorizedPayload;
import dev.kavrin.paymentrisk.payment.application.outbox.PaymentDeclinedPayload;
import dev.kavrin.paymentrisk.payment.domain.model.*;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.OutboxEventEntity;
import dev.kavrin.paymentrisk.shared.id.PlatformIdGeneratorFactory;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentOutboxEventMapperTest {

    private static final Instant REQUESTED_AT = Instant.parse("2026-05-25T10:15:30Z");
    private static final Instant COMPLETED_AT = Instant.parse("2026-05-25T10:15:35Z");
    private static final Instant CREATED_AT = Instant.parse("2026-05-25T10:15:40Z");

    private final PlatformIdGeneratorFactory idGenerator = mock(PlatformIdGeneratorFactory.class);
    private final PaymentOutboxEventMapper mapper = new PaymentOutboxEventMapper(
            Clock.fixed(CREATED_AT, ZoneOffset.UTC),
            idGenerator
    );

    @Test
    void mapsRequestedEventEnvelopeFields() {
        when(idGenerator.outboxEventId()).thenReturn("evt_requested");
        Payment payment = authorizedPayment();

        Object payload = mapper.toAuthorizationRequestedPayload(payment);
        OutboxEventEntity event = mapper.toAuthorizationRequestedEvent(
                payment,
                "corr-authorization-service",
                "{\"paymentId\":\"pay_test\"}"
        );

        assertThat(payload)
                .extracting("schemaVersion", "paymentId", "merchantId", "customerId",
                        "amountMinor", "currency", "externalReference", "requestedAt")
                .containsExactly("v1", "pay_test", "mer_test", "cus_test",
                        1299L, "USD", "order_2026_000123", REQUESTED_AT);
        assertThat(event.getEventId()).isEqualTo("evt_requested");
        assertThat(event.getEventType()).isEqualTo("PaymentAuthorizationRequested");
        assertThat(event.getSchemaVersion()).isEqualTo("v1");
        assertThat(event.getAggregateType()).isEqualTo("PAYMENT");
        assertThat(event.getAggregateId()).isEqualTo("pay_test");
        assertThat(event.getProducer()).isEqualTo("payment-orchestrator-service");
        assertThat(event.getCorrelationId()).isEqualTo("corr-authorization-service");
        assertThat(event.getStatus()).isEqualTo("PENDING");
        assertThat(event.getRetryCount()).isZero();
        assertThat(event.getNextRetryAt()).isEqualTo(CREATED_AT);
        assertThat(event.getOccurredAt()).isEqualTo(REQUESTED_AT);
        assertThat(event.getCreatedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void mapsAuthorizedEventEnvelopeAndPayload() {
        when(idGenerator.outboxEventId()).thenReturn("evt_authorized");
        Payment payment = authorizedPayment();

        Object payload = mapper.toAuthorizationCompletedPayload(payment);
        OutboxEventEntity event = mapper.toAuthorizationCompletedEvent(
                payment,
                "corr-authorization-service",
                "{\"paymentId\":\"pay_test\"}"
        );

        assertThat(payload).isInstanceOf(PaymentAuthorizedPayload.class);
        assertThat((PaymentAuthorizedPayload) payload)
                .extracting("schemaVersion", "paymentId", "authorizationCode",
                        "riskScore", "reasonCodes", "ruleVersion", "authorizedAt")
                .containsExactly("v1", "pay_test", "AUTH-ABCDEFG123",
                        7, List.of("LOW_RISK"), "risk-rules-v1", COMPLETED_AT);
        assertThat(event.getEventId()).isEqualTo("evt_authorized");
        assertThat(event.getEventType()).isEqualTo("PaymentAuthorized");
        assertThat(event.getSchemaVersion()).isEqualTo("v1");
        assertThat(event.getOccurredAt()).isEqualTo(COMPLETED_AT);
    }

    @Test
    void mapsDeclinedEventEnvelopeAndPayload() {
        when(idGenerator.outboxEventId()).thenReturn("evt_declined");
        Payment payment = declinedPayment();

        Object payload = mapper.toAuthorizationCompletedPayload(payment);
        OutboxEventEntity event = mapper.toAuthorizationCompletedEvent(
                payment,
                "corr-authorization-service",
                "{\"paymentId\":\"pay_test\"}"
        );

        assertThat(payload).isInstanceOf(PaymentDeclinedPayload.class);
        assertThat((PaymentDeclinedPayload) payload)
                .extracting("schemaVersion", "paymentId", "riskScore",
                        "reasonCodes", "ruleVersion", "declinedAt")
                .containsExactly("v1", "pay_test", 91,
                        List.of("HIGH_RISK"), "risk-rules-v1", COMPLETED_AT);
        assertThat(event.getEventId()).isEqualTo("evt_declined");
        assertThat(event.getEventType()).isEqualTo("PaymentDeclined");
        assertThat(event.getSchemaVersion()).isEqualTo("v1");
        assertThat(event.getOccurredAt()).isEqualTo(COMPLETED_AT);
    }

    private static Payment requestedPayment() {
        return Payment.newAuthorizationAttempt(
                PaymentId.of("pay_test"),
                MerchantId.of("mer_test"),
                CustomerId.of("cus_test"),
                Money.of(1299, "USD"),
                PaymentMethodToken.of("pmt_tok_sensitive_1234"),
                DeviceFingerprint.of("dfp_sensitive_device_value"),
                ExternalReference.of("order_2026_000123"),
                IdempotencyKey.of("idem_01HX7QK9JP7E5W5NRZ6T5Q3R1A"),
                REQUESTED_AT
        );
    }

    private static Payment authorizedPayment() {
        Payment payment = requestedPayment();

        payment.markRiskPending(REQUESTED_AT);
        payment.markAuthorized(
                new PaymentRiskDecision(
                        RiskDecision.APPROVED,
                        7,
                        List.of("LOW_RISK"),
                        "risk-rules-v1",
                        COMPLETED_AT
                ),
                AuthorizationCode.of("AUTH-ABCDEFG123"),
                COMPLETED_AT
        );
        return payment;
    }

    private static Payment declinedPayment() {
        Payment payment = requestedPayment();

        payment.markRiskPending(REQUESTED_AT);
        payment.markDeclined(
                new PaymentRiskDecision(
                        RiskDecision.DECLINED,
                        91,
                        List.of("HIGH_RISK"),
                        "risk-rules-v1",
                        COMPLETED_AT
                ),
                COMPLETED_AT
        );
        return payment;
    }
}
