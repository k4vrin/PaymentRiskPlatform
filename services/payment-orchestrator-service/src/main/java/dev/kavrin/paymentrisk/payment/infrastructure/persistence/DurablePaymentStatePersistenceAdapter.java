package dev.kavrin.paymentrisk.payment.infrastructure.persistence;

import dev.kavrin.paymentrisk.payment.application.service.PaymentStatePersistencePort;
import dev.kavrin.paymentrisk.payment.domain.model.Payment;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentAuthorizationEntity;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentEntity;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentRiskDecisionEntity;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.repository.PaymentAuthorizationEntityRepository;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.repository.PaymentEntityRepository;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.repository.PaymentRiskDecisionEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class DurablePaymentStatePersistenceAdapter implements PaymentStatePersistencePort {

    private final PaymentEntityRepository paymentRepository;
    private final PaymentAuthorizationEntityRepository authorizationRepository;
    private final PaymentRiskDecisionEntityRepository riskDecisionRepository;
    private final PaymentPersistenceMapper mapper;
    private final SensitivePaymentDataHasher sensitivePaymentDataHasher;

    @Override
    public Mono<Payment> save(Payment payment) {
        SensitivePaymentDataHasher.SensitivePaymentDataHashes hashes =
                sensitivePaymentDataHasher.hash(payment);

        PaymentEntity paymentEntity = mapper.toPaymentEntity(payment, hashes);
        PaymentAuthorizationEntity authorizationEntity =
                mapper.toAuthorizationEntity(payment);

        Mono<PaymentRiskDecisionEntity> riskDecisionSave =
                payment.getRiskDecision() == null
                        ? Mono.empty()
                        : riskDecisionRepository.save(
                        mapper.toRiskDecisionEntity(payment)
                );

        return paymentRepository.save(paymentEntity)
                .then(authorizationRepository.save(authorizationEntity))
                .then(riskDecisionSave)
                .thenReturn(payment);
    }
}
