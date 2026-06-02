package dev.kavrin.paymentrisk.payment.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kavrin.paymentrisk.payment.application.query.PaymentDetailsLookupPort;
import dev.kavrin.paymentrisk.payment.application.query.PaymentDetailsResult;
import dev.kavrin.paymentrisk.payment.domain.model.PaymentId;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentAuthorizationEntity;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentEntity;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentReversalEntity;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentRiskDecisionEntity;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.repository.PaymentAuthorizationEntityRepository;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.repository.PaymentEntityRepository;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.repository.PaymentReversalEntityRepository;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.repository.PaymentRiskDecisionEntityRepository;
import dev.kavrin.paymentrisk.shared.api.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DatabasePaymentDetailsLookupAdapter implements PaymentDetailsLookupPort {

    private static final TypeReference<java.util.List<String>> STRING_LIST =
            new TypeReference<>() {
            };

    private final PaymentEntityRepository paymentRepository;
    private final PaymentAuthorizationEntityRepository authorizationRepository;
    private final PaymentRiskDecisionEntityRepository riskDecisionRepository;
    private final PaymentReversalEntityRepository reversalRepository;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<PaymentDetailsResult> findByPaymentId(PaymentId paymentId) {
        return paymentRepository.findByPaymentId(paymentId.value())
                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                        "Payment not found: " + paymentId.value()
                )))
                .flatMap(payment -> {
                    var authorizationMono = authorizationRepository
                            .findByPaymentId(paymentId.value())
                            .map(this::toAuthorizationDetails)
                            .map(Optional::of)
                            .defaultIfEmpty(Optional.empty());

                    var riskMono = riskDecisionRepository
                            .findByPaymentId(paymentId.value())
                            .map(this::toRiskDetails)
                            .map(Optional::of)
                            .defaultIfEmpty(Optional.empty());

                    var reversalMono = reversalRepository
                            .findByPaymentId(paymentId.value())
                            .map(this::toReversalDetails)
                            .map(Optional::of)
                            .defaultIfEmpty(Optional.empty());

                    return Mono.zip(
                            authorizationMono,
                            riskMono,
                            reversalMono
                    ).map(tuple -> toResult(
                            payment,
                            tuple.getT1().orElse(null),
                            tuple.getT2().orElse(null),
                            tuple.getT3().orElse(null)
                    ));
                });
    }

    private static PaymentDetailsResult toResult(
            PaymentEntity payment,
            PaymentDetailsResult.AuthorizationDetails authorization,
            PaymentDetailsResult.RiskDetails risk,
            PaymentDetailsResult.ReversalDetails reversal
    ) {
        return new PaymentDetailsResult(
                payment.getPaymentId(),
                payment.getMerchantId(),
                payment.getCustomerId(),
                payment.getAmountMinor(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getExternalReference(),
                authorization,
                risk,
                reversal,
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }

    private PaymentDetailsResult.AuthorizationDetails toAuthorizationDetails(
            PaymentAuthorizationEntity entity
    ) {
        return new PaymentDetailsResult.AuthorizationDetails(
                entity.getStatus(),
                entity.getAuthorizationCode(),
                entity.getRequestedAt(),
                entity.getRiskPendingAt(),
                entity.getAuthorizedAt(),
                entity.getDeclinedAt(),
                entity.getFailedAt()
        );
    }

    private PaymentDetailsResult.RiskDetails toRiskDetails(
            PaymentRiskDecisionEntity entity
    ) {
        return new PaymentDetailsResult.RiskDetails(
                entity.getDecision(),
                entity.getScore(),
                readReasonCodes(entity.getReasonCodesJson()),
                entity.getRuleVersion(),
                entity.getDecidedAt()
        );
    }

    private PaymentDetailsResult.ReversalDetails toReversalDetails(
            PaymentReversalEntity entity
    ) {
        return new PaymentDetailsResult.ReversalDetails(
                entity.getPaymentReversalId(),
                entity.getStatus(),
                entity.getReason(),
                entity.getRequestedAt(),
                entity.getReversedAt()
        );
    }

    private List<String> readReasonCodes(String json) {
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Failed to deserialize payment risk reason codes",
                    exception
            );
        }
    }
}
