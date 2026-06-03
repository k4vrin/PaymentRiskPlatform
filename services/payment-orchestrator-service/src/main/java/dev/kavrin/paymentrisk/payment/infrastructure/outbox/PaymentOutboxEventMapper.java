package dev.kavrin.paymentrisk.payment.infrastructure.outbox;

import dev.kavrin.paymentrisk.payment.application.outbox.*;
import dev.kavrin.paymentrisk.payment.domain.model.Payment;
import dev.kavrin.paymentrisk.payment.domain.model.PaymentAuthorization;
import dev.kavrin.paymentrisk.payment.domain.model.PaymentRiskDecision;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.OutboxEventEntity;
import dev.kavrin.paymentrisk.shared.id.PlatformIdGeneratorFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class PaymentOutboxEventMapper {

    static final String AGGREGATE_TYPE = "PAYMENT";
    static final String PRODUCER = "payment-orchestrator-service";
    static final String STATUS_PENDING = "PENDING";
    static final String PAYMENT_AUTHORIZATION_REQUESTED = "PaymentAuthorizationRequested";
    static final String PAYMENT_AUTHORIZED = "PaymentAuthorized";
    static final String PAYMENT_DECLINED = "PaymentDeclined";
    static final String PAYMENT_REVERSED = "PaymentReversed";

    private final Clock clock;
    private final PlatformIdGeneratorFactory idGenerator;

    public Object toAuthorizationRequestedPayload(Payment payment) {
        return PaymentAuthorizationRequestedPayload.v1(
                payment.getId().value(),
                payment.getMerchantId().value(),
                payment.getCustomerId().value(),
                payment.getAmount().amountMinor(),
                payment.getAmount().currencyCode(),
                payment.getExternalReference() == null
                        ? null
                        : payment.getExternalReference().value(),
                payment.getAuthorization().requestedAt()
        );
    }

    public Object toAuthorizationCompletedPayload(Payment payment) {
        PaymentRiskDecision riskDecision = requireRiskDecision(payment);

        return switch (payment.getAuthorization()) {
            case PaymentAuthorization.Authorized authorized -> PaymentAuthorizedPayload.v1(
                    payment.getId().value(),
                    payment.getMerchantId().value(),
                    payment.getCustomerId().value(),
                    payment.getAmount().amountMinor(),
                    payment.getAmount().currencyCode(),
                    authorized.authorizationCode().value(),
                    riskDecision.score(),
                    riskDecision.reasonCodes(),
                    riskDecision.ruleVersion(),
                    authorized.authorizedAt()
            );
            case PaymentAuthorization.Declined declined -> PaymentDeclinedPayload.v1(
                    payment.getId().value(),
                    payment.getMerchantId().value(),
                    payment.getCustomerId().value(),
                    payment.getAmount().amountMinor(),
                    payment.getAmount().currencyCode(),
                    riskDecision.score(),
                    riskDecision.reasonCodes(),
                    riskDecision.ruleVersion(),
                    declined.declinedAt()
            );
            default -> throw new IllegalStateException(
                    "Payment authorization is not completed: "
                            + payment.getAuthorization().getClass().getSimpleName()
            );
        };
    }

    public OutboxEventEntity toAuthorizationRequestedEvent(
            Payment payment,
            String correlationId,
            String payloadJson
    ) {
        return toEventEntity(
                payment,
                correlationId,
                payloadJson,
                PAYMENT_AUTHORIZATION_REQUESTED,
                PaymentOutboxSchemaVersions.PAYMENT_AUTHORIZATION_REQUESTED_V1,
                payment.getAuthorization().requestedAt()
        );
    }

    public OutboxEventEntity toAuthorizationCompletedEvent(
            Payment payment,
            String correlationId,
            String payloadJson
    ) {
        PaymentAuthorization authorization = payment.getAuthorization();
        String eventType = switch (authorization) {
            case PaymentAuthorization.Authorized ignored -> PAYMENT_AUTHORIZED;
            case PaymentAuthorization.Declined ignored -> PAYMENT_DECLINED;
            default -> throw new IllegalStateException(
                    "Payment authorization is not completed: "
                            + authorization.getClass().getSimpleName()
            );
        };

        String schemaVersion = switch (authorization) {
            case PaymentAuthorization.Authorized ignored ->
                    PaymentOutboxSchemaVersions.PAYMENT_AUTHORIZED_V1;
            case PaymentAuthorization.Declined ignored ->
                    PaymentOutboxSchemaVersions.PAYMENT_DECLINED_V1;
            default -> throw new IllegalStateException(
                    "Payment authorization is not completed: "
                            + authorization.getClass().getSimpleName()
            );
        };

        return toEventEntity(
                payment,
                correlationId,
                payloadJson,
                eventType,
                schemaVersion,
                completedAt(authorization)
        );
    }

    private OutboxEventEntity toEventEntity(
            Payment payment,
            String correlationId,
            String payloadJson,
            String eventType,
            String schemaVersion,
            Instant occurredAt
    ) {
        Instant createdAt = clock.instant();

        return OutboxEventEntity.builder()
                .eventId(idGenerator.outboxEventId())
                .eventType(eventType)
                .schemaVersion(schemaVersion)
                .aggregateType(AGGREGATE_TYPE)
                .aggregateId(payment.getId().value())
                .producer(PRODUCER)
                .correlationId(correlationId)
                .payloadJson(payloadJson)
                .status(STATUS_PENDING)
                .retryCount(0)
                .nextRetryAt(createdAt)
                .occurredAt(occurredAt)
                .createdAt(createdAt)
                .build();
    }

    public OutboxEventEntity toPaymentReversedEvent(
            Payment payment,
            String correlationId,
            String payloadJson
    ) {
        var reversal = payment.reversal()
                .orElseThrow(() -> new IllegalStateException("Payment is missing reversal state"));

        return toEventEntity(
                payment,
                correlationId,
                payloadJson,
                PAYMENT_REVERSED,
                PaymentOutboxSchemaVersions.PAYMENT_REVERSED_V1,
                reversal.reversedAt()
        );
    }

    public Object toPaymentReversedPayload(Payment payment) {
        var reversal = payment.reversal()
                .orElseThrow(() -> new IllegalStateException("Payment is missing reversal state"));

        return new PaymentReversedPayload(
                payment.getId().value(),
                reversal.reversalId().value(),
                payment.getMerchantId().value(),
                payment.getCustomerId().value(),
                payment.getAmount().amountMinor(),
                payment.getAmount().currencyCode(),
                reversal.reason().value(),
                reversal.reversedAt()
        );
    }

    private static PaymentRiskDecision requireRiskDecision(Payment payment) {
        PaymentRiskDecision riskDecision = payment.getRiskDecision();

        if (riskDecision == null) {
            throw new IllegalStateException(
                    "Payment risk decision is missing for payment ID: " + payment.getId().value()
            );
        }

        return riskDecision;
    }

    private static Instant completedAt(PaymentAuthorization authorization) {
        return switch (authorization) {
            case PaymentAuthorization.Authorized authorized -> authorized.authorizedAt();
            case PaymentAuthorization.Declined declined -> declined.declinedAt();
            default -> throw new IllegalStateException(
                    "Payment authorization is not completed: "
                            + authorization.getClass().getSimpleName()
            );
        };
    }
}
