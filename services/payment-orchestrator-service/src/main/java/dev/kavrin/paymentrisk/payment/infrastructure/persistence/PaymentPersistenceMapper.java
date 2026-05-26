package dev.kavrin.paymentrisk.payment.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKey;
import dev.kavrin.paymentrisk.payment.domain.model.*;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentAuthorizationRow;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentRiskDecisionRow;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentRow;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class PaymentPersistenceMapper {

    private static final TypeReference<List<String>> STRING_LIST =
            new TypeReference<>() {
            };

    private final ObjectMapper objectMapper;

    public PaymentPersistenceMapper() {
        this(new ObjectMapper());
    }

    PaymentPersistenceMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public PaymentRow toPaymentRow(
            Payment payment,
            String paymentMethodTokenHash,
            String paymentMethodTokenLast4,
            String deviceFingerprintHash
    ) {
        return new PaymentRow(
                payment.getId().value(),
                payment.getMerchantId().value(),
                payment.getCustomerId().value(),
                payment.getAmount().amountMinor(),
                payment.getAmount().currencyCode(),
                paymentMethodTokenHash,
                paymentMethodTokenLast4,
                deviceFingerprintHash,
                payment.getExternalReference() == null
                        ? null
                        : payment.getExternalReference().value(),
                payment.getIdempotencyKey().value(),
                payment.getStatus().name(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }

    public PaymentAuthorizationRow toAuthorizationRow(Payment payment) {
        PaymentAuthorization authorization = payment.getAuthorization();

        return switch (authorization) {

            case PaymentAuthorization.Requested requested -> new PaymentAuthorizationRow(
                    newAuthorizationRowId(),
                    payment.getId().value(),
                    "REQUESTED",
                    null,
                    null,
                    requested.requestedAt(),
                    null,
                    null,
                    null,
                    null,
                    payment.getCreatedAt(),
                    payment.getUpdatedAt()
            );

            case PaymentAuthorization.RiskPending riskPending -> new PaymentAuthorizationRow(
                    newAuthorizationRowId(),
                    payment.getId().value(),
                    "RISK_PENDING",
                    null,
                    null,
                    riskPending.requestedAt(),
                    riskPending.riskPendingAt(),
                    null,
                    null,
                    null,
                    payment.getCreatedAt(),
                    payment.getUpdatedAt()
            );

            case PaymentAuthorization.Authorized authorized -> new PaymentAuthorizationRow(
                    newAuthorizationRowId(),
                    payment.getId().value(),
                    "AUTHORIZED",
                    authorized.authorizationCode().value(),
                    null,
                    authorized.requestedAt(),
                    authorized.riskPendingAt(),
                    authorized.authorizedAt(),
                    null,
                    null,
                    payment.getCreatedAt(),
                    payment.getUpdatedAt()
            );

            case PaymentAuthorization.Declined declined -> new PaymentAuthorizationRow(
                    newAuthorizationRowId(),
                    payment.getId().value(),
                    "DECLINED",
                    null,
                    null,
                    declined.requestedAt(),
                    declined.riskPendingAt(),
                    null,
                    declined.declinedAt(),
                    null,
                    payment.getCreatedAt(),
                    payment.getUpdatedAt()
            );

            case PaymentAuthorization.Failed failed -> new PaymentAuthorizationRow(
                    newAuthorizationRowId(),
                    payment.getId().value(),
                    "FAILED",
                    null,
                    failed.failureReason(),
                    failed.requestedAt(),
                    null,
                    null,
                    null,
                    failed.failedAt(),
                    payment.getCreatedAt(),
                    payment.getUpdatedAt()
            );
        };
    }

    public PaymentRiskDecisionRow toRiskDecisionRow(Payment payment) {

        PaymentRiskDecision riskDecision = payment.getRiskDecision();

        if (riskDecision == null) {
            return null;
        }

        return new PaymentRiskDecisionRow(
                newRiskDecisionRowId(),
                payment.getId().value(),
                riskDecision.decision().name(),
                riskDecision.score(),
                writeReasonCodes(riskDecision.reasonCodes()),
                riskDecision.ruleVersion(),
                riskDecision.decidedAt(),
                Instant.now()
        );
    }

    public Payment toDomain(
            PaymentRow paymentRow,
            PaymentAuthorizationRow authorizationRow,
            PaymentRiskDecisionRow riskDecisionRow
    ) {

        PaymentAuthorization authorization =
                toDomainAuthorization(authorizationRow);

        PaymentRiskDecision riskDecision =
                riskDecisionRow == null
                        ? null
                        : toDomainRiskDecision(riskDecisionRow);

        return Payment.restore(
                PaymentId.of(paymentRow.getPaymentId()),
                MerchantId.of(paymentRow.getMerchantId()),
                CustomerId.of(paymentRow.getCustomerId()),
                Money.of(
                        paymentRow.getAmountMinor(),
                        paymentRow.getCurrency()
                ),
                PaymentMethodToken.restoredMasked(),
                DeviceFingerprint.restoredMasked(),
                ExternalReference.ofNullable(
                        paymentRow.getExternalReference()
                ),
                IdempotencyKey.of(paymentRow.getIdempotencyKey()),
                PaymentStatus.valueOf(paymentRow.getStatus()),
                authorization,
                riskDecision,
                paymentRow.getCreatedAt(),
                paymentRow.getUpdatedAt()
        );
    }

    private PaymentAuthorization toDomainAuthorization(
            PaymentAuthorizationRow row
    ) {

        return switch (row.getStatus()) {

            case "REQUESTED" -> new PaymentAuthorization.Requested(
                    row.getRequestedAt()
            );

            case "RISK_PENDING" -> new PaymentAuthorization.RiskPending(
                    row.getRequestedAt(),
                    row.getRiskPendingAt()
            );

            case "AUTHORIZED" -> new PaymentAuthorization.Authorized(
                    row.getRequestedAt(),
                    row.getRiskPendingAt(),
                    AuthorizationCode.of(
                            row.getAuthorizationCode()
                    ),
                    row.getAuthorizedAt()
            );

            case "DECLINED" -> new PaymentAuthorization.Declined(
                    row.getRequestedAt(),
                    row.getRiskPendingAt(),
                    row.getDeclinedAt()
            );

            case "FAILED" -> new PaymentAuthorization.Failed(
                    row.getRequestedAt(),
                    row.getFailureReason(),
                    row.getFailedAt()
            );

            default -> throw new IllegalStateException(
                    "Unknown authorization status: "
                            + row.getStatus()
            );
        };
    }

    private PaymentRiskDecision toDomainRiskDecision(
            PaymentRiskDecisionRow row
    ) {

        return new PaymentRiskDecision(
                RiskDecision.valueOf(row.getDecision()),
                row.getScore(),
                readReasonCodes(row.getReasonCodesJson()),
                row.getRuleVersion(),
                row.getDecidedAt()
        );
    }

    private String writeReasonCodes(List<String> reasonCodes) {
        try {
            return objectMapper.writeValueAsString(reasonCodes);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Failed to serialize reason codes",
                    exception
            );
        }
    }

    private List<String> readReasonCodes(String json) {
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Failed to deserialize reason codes",
                    exception
            );
        }
    }

    private static String newAuthorizationRowId() {
        return "pauth_" +
                UUID.randomUUID()
                        .toString()
                        .replace("-", "");
    }

    private static String newRiskDecisionRowId() {
        return "prd_" +
                UUID.randomUUID()
                        .toString()
                        .replace("-", "");
    }
}
