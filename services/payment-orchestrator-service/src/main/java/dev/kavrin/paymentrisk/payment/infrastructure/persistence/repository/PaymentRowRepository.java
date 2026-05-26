package dev.kavrin.paymentrisk.payment.infrastructure.persistence.repository;

import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentRow;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PaymentRowRepository extends ReactiveCrudRepository<PaymentRow, String> {
    Mono<PaymentRow> findByPaymentId(String paymentId);

    Flux<PaymentRow> findByMerchantId(String merchantId);

    Flux<PaymentRow> findByCustomerId(String customerId);
}
