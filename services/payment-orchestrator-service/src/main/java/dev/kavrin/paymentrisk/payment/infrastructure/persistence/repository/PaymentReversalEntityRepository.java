package dev.kavrin.paymentrisk.payment.infrastructure.persistence.repository;

import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentReversalEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface PaymentReversalEntityRepository
        extends ReactiveCrudRepository<PaymentReversalEntity, String> {

    Mono<PaymentReversalEntity> findByPaymentId(String paymentId);

    Mono<PaymentReversalEntity> findByIdempotencyKey(String idempotencyKey);
}
