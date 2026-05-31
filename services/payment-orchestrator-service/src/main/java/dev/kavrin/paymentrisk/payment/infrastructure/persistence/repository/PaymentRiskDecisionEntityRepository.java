package dev.kavrin.paymentrisk.payment.infrastructure.persistence.repository;

import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentRiskDecisionEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface PaymentRiskDecisionEntityRepository
        extends ReactiveCrudRepository<PaymentRiskDecisionEntity, String> {

    Mono<PaymentRiskDecisionEntity> findByPaymentId(String paymentId);
}