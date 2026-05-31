package dev.kavrin.paymentrisk.payment.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKey;
import dev.kavrin.paymentrisk.payment.domain.model.*;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentAuthorizationEntity;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentEntity;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentRiskDecisionEntity;
import dev.kavrin.paymentrisk.shared.id.PlatformIdGeneratorFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

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

    public PaymentEntity toPaymentEntity(
            Payment payment,
            SensitivePaymentDataHasher.SensitivePaymentDataHashes sensitiveDataHashes
    ) {
        Objects.requireNonNull(sensitiveDataHashes, "sensitiveDataHashes must not be null");

        return toPaymentEntity(
                payment,
                sensitiveDataHashes.paymentMethodTokenHash(),
                sensitiveDataHashes.paymentMethodTokenLastFour(),
                sensitiveDataHashes.deviceFingerprintHash()
        );
    }

    public PaymentEntity toPaymentEntity(
            Payment payment,
            String paymentMethodTokenHash,
            String paymentMethodTokenLast4,
            String deviceFingerprintHash
    ) {
        return PaymentEntity.builder()
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

    public PaymentAuthorizationEntity toAuthorizationEntity(Payment payment) {
        PaymentAuthorization authorization = payment.getAuthorization();

        return switch (authorization) {

            case PaymentAuthorization.Requested requested -> PaymentAuthorizationEntity.builder()
                    .paymentAuthorizationId(idGenerator.paymentAuthorizationId())
                    .paymentId(payment.getId().value())
                    .status("REQUESTED")
                    .requestedAt(requested.requestedAt())
                    .createdAt(payment.getCreatedAt())
                    .updatedAt(payment.getUpdatedAt())
                    .build();

            case PaymentAuthorization.RiskPending riskPending -> PaymentAuthorizationEntity.builder()
                    .paymentAuthorizationId(idGenerator.paymentAuthorizationId())
                    .paymentId(payment.getId().value())
                    .status("RISK_PENDING")
                    .requestedAt(riskPending.requestedAt())
                    .riskPendingAt(riskPending.riskPendingAt())
                    .createdAt(payment.getCreatedAt())
                    .updatedAt(payment.getUpdatedAt())
                    .build();

            case PaymentAuthorization.Authorized authorized -> PaymentAuthorizationEntity.builder()
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

            case PaymentAuthorization.Declined declined -> PaymentAuthorizationEntity.builder()
                    .paymentAuthorizationId(idGenerator.paymentAuthorizationId())
                    .paymentId(payment.getId().value())
                    .status("DECLINED")
                    .requestedAt(declined.requestedAt())
                    .riskPendingAt(declined.riskPendingAt())
                    .declinedAt(declined.declinedAt())
                    .createdAt(payment.getCreatedAt())
                    .updatedAt(payment.getUpdatedAt())
                    .build();

            case PaymentAuthorization.Failed failed -> PaymentAuthorizationEntity.builder()
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

    public PaymentRiskDecisionEntity toRiskDecisionEntity(Payment payment) {

        PaymentRiskDecision riskDecision = payment.getRiskDecision();

        if (riskDecision == null) {
            return null;
        }

        return PaymentRiskDecisionEntity.builder()
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
            PaymentEntity paymentEntity,
            PaymentAuthorizationEntity authorizationEntity,
            PaymentRiskDecisionEntity riskDecisionEntity
    ) {

        PaymentAuthorization authorization =
                toDomainAuthorization(authorizationEntity);

        PaymentRiskDecision riskDecision =
                riskDecisionEntity == null
                        ? null
                        : toDomainRiskDecision(riskDecisionEntity);

        return Payment.restore(
                PaymentId.of(paymentEntity.getPaymentId()),
                MerchantId.of(paymentEntity.getMerchantId()),
                CustomerId.of(paymentEntity.getCustomerId()),
                Money.of(
                        paymentEntity.getAmountMinor(),
                        paymentEntity.getCurrency()
                ),
                PaymentMethodToken.restoredMasked(),
                DeviceFingerprint.restoredMasked(),
                ExternalReference.ofNullable(
                        paymentEntity.getExternalReference()
                ),
                IdempotencyKey.of(paymentEntity.getIdempotencyKey()),
                PaymentStatus.valueOf(paymentEntity.getStatus()),
                authorization,
                riskDecision,
                paymentEntity.getCreatedAt(),
                paymentEntity.getUpdatedAt()
        );
    }

    private PaymentAuthorization toDomainAuthorization(
            PaymentAuthorizationEntity entity
    ) {

        return switch (entity.getStatus()) {

            case "REQUESTED" -> new PaymentAuthorization.Requested(
                    entity.getRequestedAt()
            );

            case "RISK_PENDING" -> new PaymentAuthorization.RiskPending(
                    entity.getRequestedAt(),
                    entity.getRiskPendingAt()
            );

            case "AUTHORIZED" -> new PaymentAuthorization.Authorized(
                    entity.getRequestedAt(),
                    entity.getRiskPendingAt(),
                    AuthorizationCode.of(
                            entity.getAuthorizationCode()
                    ),
                    entity.getAuthorizedAt()
            );

            case "DECLINED" -> new PaymentAuthorization.Declined(
                    entity.getRequestedAt(),
                    entity.getRiskPendingAt(),
                    entity.getDeclinedAt()
            );

            case "FAILED" -> new PaymentAuthorization.Failed(
                    entity.getRequestedAt(),
                    entity.getFailureReason(),
                    entity.getFailedAt()
            );

            default -> throw new IllegalStateException(
                    "Unknown authorization status: "
                            + entity.getStatus()
            );
        };
    }

    private PaymentRiskDecision toDomainRiskDecision(
            PaymentRiskDecisionEntity entity
    ) {

        return new PaymentRiskDecision(
                RiskDecision.valueOf(entity.getDecision()),
                entity.getScore(),
                readReasonCodes(entity.getReasonCodesJson()),
                entity.getRuleVersion(),
                entity.getDecidedAt()
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
