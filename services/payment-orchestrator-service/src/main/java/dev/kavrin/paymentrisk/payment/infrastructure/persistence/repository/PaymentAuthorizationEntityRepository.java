package dev.kavrin.paymentrisk.payment.infrastructure.persistence.repository;

import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentAuthorizationEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface PaymentAuthorizationEntityRepository
        extends ReactiveCrudRepository<PaymentAuthorizationEntity, String> {

    Mono<PaymentAuthorizationEntity> findByPaymentId(String paymentId);
}