package dev.kavrin.paymentrisk.payment.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKey;
import dev.kavrin.paymentrisk.payment.domain.model.*;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentAuthorizationRow;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentRiskDecisionRow;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentRow;
import dev.kavrin.paymentrisk.shared.id.PlatformIdGeneratorFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class PaymentPersistenceMapper {

    private static final TypeReference<List<String>> STRING_LIST =
            new TypeReference<>() {
            };

    private final ObjectMapper objectMapper;
    private final PlatformIdGeneratorFactory idGenerator;

    public PaymentPersistenceMapper() {
        this(new ObjectMapper(), new PlatformIdGeneratorFactory());
    }

    PaymentPersistenceMapper(ObjectMapper objectMapper) {
        this(objectMapper, new PlatformIdGeneratorFactory());
    }

    PaymentPersistenceMapper(ObjectMapper objectMapper, PlatformIdGeneratorFactory idGenerator) {
        this.objectMapper = objectMapper;
        this.idGenerator = idGenerator;
    }

    public PaymentRow toPaymentRow(
            Payment payment,
            String paymentMethodTokenHash,
            String paymentMethodTokenLast4,
            String deviceFingerprintHash
    ) {
        return PaymentRow.builder()
                .paymentId(payment.getId().value())
                .merchantId(payment.getMerchantId().value())
                .customerId(payment.getCustomerId().value())
                .amountMinor(payment.getAmount().amountMinor())
                .currency(payment.getAmount().currencyCode())
                .paymentMethodTokenHash(paymentMethodTokenHash)
                .paymentMethodTokenLast4(paymentMethodTokenLast4)
                .deviceFingerprintHash(deviceFingerprintHash)
                .externalReference(payment.getExternalReference() == null
                        ? null
                        : payment.getExternalReference().value())
                .idempotencyKey(payment.getIdempotencyKey().value())
                .status(payment.getStatus().name())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }

    public PaymentAuthorizationRow toAuthorizationRow(Payment payment) {
        PaymentAuthorization authorization = payment.getAuthorization();

        return switch (authorization) {

            case PaymentAuthorization.Requested requested -> PaymentAuthorizationRow.builder()
                    .paymentAuthorizationId(idGenerator.paymentAuthorizationId())
                    .paymentId(payment.getId().value())
                    .status("REQUESTED")
                    .requestedAt(requested.requestedAt())
                    .createdAt(payment.getCreatedAt())
                    .updatedAt(payment.getUpdatedAt())
                    .build();

            case PaymentAuthorization.RiskPending riskPending -> PaymentAuthorizationRow.builder()
                    .paymentAuthorizationId(idGenerator.paymentAuthorizationId())
                    .paymentId(payment.getId().value())
                    .status("RISK_PENDING")
                    .requestedAt(riskPending.requestedAt())
                    .riskPendingAt(riskPending.riskPendingAt())
                    .createdAt(payment.getCreatedAt())
                    .updatedAt(payment.getUpdatedAt())
                    .build();

            case PaymentAuthorization.Authorized authorized -> PaymentAuthorizationRow.builder()
                    .paymentAuthorizationId(idGenerator.paymentAuthorizationId())
                    .paymentId(payment.getId().value())
                    .status("AUTHORIZED")
                    .authorizationCode(authorized.authorizationCode().value())
                    .requestedAt(authorized.requestedAt())
                    .riskPendingAt(authorized.riskPendingAt())
                    .authorizedAt(authorized.authorizedAt())
                    .createdAt(payment.getCreatedAt())
                    .updatedAt(payment.getUpdatedAt())
                    .build();

            case PaymentAuthorization.Declined declined -> PaymentAuthorizationRow.builder()
                    .paymentAuthorizationId(idGenerator.paymentAuthorizationId())
                    .paymentId(payment.getId().value())
                    .status("DECLINED")
                    .requestedAt(declined.requestedAt())
                    .riskPendingAt(declined.riskPendingAt())
                    .declinedAt(declined.declinedAt())
                    .createdAt(payment.getCreatedAt())
                    .updatedAt(payment.getUpdatedAt())
                    .build();

            case PaymentAuthorization.Failed failed -> PaymentAuthorizationRow.builder()
                    .paymentAuthorizationId(idGenerator.paymentAuthorizationId())
                    .paymentId(payment.getId().value())
                    .status("FAILED")
                    .failureReason(failed.failureReason())
                    .requestedAt(failed.requestedAt())
                    .failedAt(failed.failedAt())
                    .createdAt(payment.getCreatedAt())
                    .updatedAt(payment.getUpdatedAt())
                    .build();
        };
    }

    public PaymentRiskDecisionRow toRiskDecisionRow(Payment payment) {

        PaymentRiskDecision riskDecision = payment.getRiskDecision();

        if (riskDecision == null) {
            return null;
        }

        return PaymentRiskDecisionRow.builder()
                .paymentRiskDecisionId(idGenerator.paymentRiskDecisionId())
                .paymentId(payment.getId().value())
                .decision(riskDecision.decision().name())
                .score(riskDecision.score())
                .reasonCodesJson(writeReasonCodes(riskDecision.reasonCodes()))
                .ruleVersion(riskDecision.ruleVersion())
                .decidedAt(riskDecision.decidedAt())
                .createdAt(Instant.now())
                .build();
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

}
