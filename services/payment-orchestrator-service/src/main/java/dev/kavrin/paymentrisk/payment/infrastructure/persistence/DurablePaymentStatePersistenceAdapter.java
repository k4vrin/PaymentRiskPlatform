package dev.kavrin.paymentrisk.payment.infrastructure.persistence;

import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKey;
import dev.kavrin.paymentrisk.payment.application.service.PaymentStatePersistencePort;
import dev.kavrin.paymentrisk.payment.domain.model.Payment;
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
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DurablePaymentStatePersistenceAdapter implements PaymentStatePersistencePort {

    private final R2dbcEntityTemplate entityTemplate;
    private final PaymentPersistenceMapper mapper;
    private final SensitivePaymentDataHasher sensitivePaymentDataHasher;
    private final PaymentEntityRepository paymentRepository;
    private final PaymentAuthorizationEntityRepository authorizationRepository;
    private final PaymentRiskDecisionEntityRepository riskDecisionRepository;
    private final PaymentReversalEntityRepository reversalRepository;

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

    @Override
    public Mono<Payment> findByPaymentId(PaymentId paymentId) {
        return paymentRepository.findByPaymentId(paymentId.value())
                .switchIfEmpty(Mono.error(new ResourceNotFoundException(
                        "Payment not found: " + paymentId.value()
                )))
                .flatMap(paymentEntity ->
                        authorizationRepository.findByPaymentId(paymentId.value())
                                .switchIfEmpty(Mono.error(new IllegalStateException(
                                        "Payment authorization was not found: " + paymentId.value()
                                )))
                                .flatMap(authorizationEntity ->
                                        Mono.zip(
                                                        riskDecisionRepository.findByPaymentId(paymentId.value())
                                                                .map(Optional::of)
                                                                .defaultIfEmpty(Optional.empty()),
                                                        reversalRepository.findByPaymentId(paymentId.value())
                                                                .map(Optional::of)
                                                                .defaultIfEmpty(Optional.empty())
                                                )
                                                .map(tuple -> mapper.toDomain(
                                                        paymentEntity,
                                                        authorizationEntity,
                                                        tuple.getT1().orElse(null),
                                                        mapper.toDomainReversal(tuple.getT2().orElse(null))
                                                ))
                                )
                );
    }

    @Override
    public Mono<Payment> saveReversal(
            Payment payment,
            IdempotencyKey reversalIdempotencyKey
    ) {
        PaymentReversalEntity reversalEntity = mapper.toReversalEntity(
                payment,
                reversalIdempotencyKey
        );

        return entityTemplate.update(PaymentEntity.class)
                .matching(org.springframework.data.relational.core.query.Query.query(
                        org.springframework.data.relational.core.query.Criteria
                                .where("payment_id")
                                .is(payment.getId().value())
                ))
                .apply(org.springframework.data.relational.core.query.Update.update(
                                "status",
                                payment.getStatus().name()
                        )
                        .set("updated_at", payment.getUpdatedAt()))
                .then(entityTemplate.insert(PaymentReversalEntity.class)
                        .using(reversalEntity))
                .thenReturn(payment);
    }
}
