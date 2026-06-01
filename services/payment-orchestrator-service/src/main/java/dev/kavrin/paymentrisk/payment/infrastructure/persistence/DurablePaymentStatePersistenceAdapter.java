package dev.kavrin.paymentrisk.payment.infrastructure.persistence;

import dev.kavrin.paymentrisk.payment.application.service.PaymentStatePersistencePort;
import dev.kavrin.paymentrisk.payment.domain.model.Payment;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentAuthorizationEntity;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentEntity;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentRiskDecisionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class DurablePaymentStatePersistenceAdapter implements PaymentStatePersistencePort {

    private final R2dbcEntityTemplate entityTemplate;
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
                        : entityTemplate.insert(PaymentRiskDecisionEntity.class).using(
                                mapper.toRiskDecisionEntity(payment)
                        );

        return entityTemplate.insert(PaymentEntity.class).using(paymentEntity)
                .then(entityTemplate.insert(PaymentAuthorizationEntity.class)
                        .using(authorizationEntity))
                .then(riskDecisionSave)
                .thenReturn(payment);
    }
}
