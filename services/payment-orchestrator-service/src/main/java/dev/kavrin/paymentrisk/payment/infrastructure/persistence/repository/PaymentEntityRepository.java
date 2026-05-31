package dev.kavrin.paymentrisk.payment.infrastructure.persistence.repository;

import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PaymentEntityRepository extends ReactiveCrudRepository<PaymentEntity, String> {
    Mono<PaymentEntity> findByPaymentId(String paymentId);

    Flux<PaymentEntity> findByMerchantId(String merchantId);

    Flux<PaymentEntity> findByCustomerId(String customerId);
}
