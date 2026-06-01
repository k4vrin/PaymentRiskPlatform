package dev.kavrin.paymentrisk.payment.infrastructure.outbox;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKey;
import dev.kavrin.paymentrisk.payment.domain.model.*;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.OutboxEventEntity;
import dev.kavrin.paymentrisk.shared.id.PlatformIdGeneratorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.core.ReactiveInsertOperation;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
class DatabasePaymentOutboxEventWriterTest {

    private static final Instant NOW = Instant.parse("2026-05-25T10:15:30Z");

    private final PlatformIdGeneratorFactory idGenerator = mock(PlatformIdGeneratorFactory.class);
    private final R2dbcEntityTemplate entityTemplate = mock(R2dbcEntityTemplate.class);
    private final ReactiveInsertOperation.ReactiveInsert<OutboxEventEntity> insertSpec =
            mock(ReactiveInsertOperation.ReactiveInsert.class);
    private final DatabasePaymentOutboxEventWriter writer = new DatabasePaymentOutboxEventWriter(
            new PaymentOutboxEventMapper(Clock.fixed(NOW, ZoneOffset.UTC), idGenerator),
            entityTemplate,
            JsonMapper.builder()
                    .addModule(new JavaTimeModule())
                    .build()
    );

    @BeforeEach
    void configureMocks() {
        reset(idGenerator, entityTemplate, insertSpec);
        when(idGenerator.outboxEventId())
                .thenReturn("evt_requested", "evt_completed");
        when(entityTemplate.insert(OutboxEventEntity.class))
                .thenReturn(insertSpec);
        when(insertSpec.using(any(OutboxEventEntity.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    }

    @Test
    void writeAuthorizationEventsPersistsRequestedAndAuthorizedEventsInOrder() {
        StepVerifier.create(writer.writeAuthorizationEvents(authorizedPayment(), "corr-authorization-service"))
                .verifyComplete();

        ArgumentCaptor<OutboxEventEntity> eventCaptor =
                ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(insertSpec, times(2)).using(eventCaptor.capture());

        assertThat(eventCaptor.getAllValues())
                .extracting(OutboxEventEntity::getEventType)
                .containsExactly("PaymentAuthorizationRequested", "PaymentAuthorized");
        assertThat(eventCaptor.getAllValues())
                .extracting(OutboxEventEntity::getStatus)
                .containsExactly("PENDING", "PENDING");
        assertThat(eventCaptor.getAllValues().get(0).getPayloadJson())
                .contains("\"schemaVersion\":\"v1\"")
                .contains("\"requestedAt\"");
        assertThat(eventCaptor.getAllValues().get(1).getPayloadJson())
                .contains("\"authorizationCode\":\"AUTH-ABCDEFG123\"")
                .contains("\"riskScore\":7");
    }

    @Test
    void writeAuthorizationEventsPersistsRequestedAndDeclinedEventsInOrder() {
        StepVerifier.create(writer.writeAuthorizationEvents(declinedPayment(), "corr-authorization-service"))
                .verifyComplete();

        ArgumentCaptor<OutboxEventEntity> eventCaptor =
                ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(insertSpec, times(2)).using(eventCaptor.capture());

        assertThat(eventCaptor.getAllValues())
                .extracting(OutboxEventEntity::getEventType)
                .containsExactly("PaymentAuthorizationRequested", "PaymentDeclined");
        assertThat(eventCaptor.getAllValues().get(1).getPayloadJson())
                .contains("\"riskScore\":91")
                .doesNotContain("authorizationCode");
    }

    @Test
    void writeAuthorizationEventsPropagatesRepositoryFailure() {
        when(insertSpec.using(any(OutboxEventEntity.class)))
                .thenReturn(Mono.error(new IllegalStateException("outbox insert failed")));

        StepVerifier.create(writer.writeAuthorizationEvents(authorizedPayment(), "corr-authorization-service"))
                .expectErrorMessage("outbox insert failed")
                .verify();
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
                NOW
        );
    }

    private static Payment authorizedPayment() {
        Payment payment = requestedPayment();

        payment.markRiskPending(NOW);
        payment.markAuthorized(
                new PaymentRiskDecision(
                        RiskDecision.APPROVED,
                        7,
                        List.of("LOW_RISK"),
                        "risk-rules-v1",
                        NOW
                ),
                AuthorizationCode.of("AUTH-ABCDEFG123"),
                NOW
        );
        return payment;
    }

    private static Payment declinedPayment() {
        Payment payment = requestedPayment();

        payment.markRiskPending(NOW);
        payment.markDeclined(
                new PaymentRiskDecision(
                        RiskDecision.DECLINED,
                        91,
                        List.of("HIGH_RISK"),
                        "risk-rules-v1",
                        NOW
                ),
                NOW
        );
        return payment;
    }
}
