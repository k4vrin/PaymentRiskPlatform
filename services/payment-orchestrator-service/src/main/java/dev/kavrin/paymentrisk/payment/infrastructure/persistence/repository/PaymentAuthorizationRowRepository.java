package dev.kavrin.paymentrisk.payment.infrastructure.persistence.repository;

import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentAuthorizationRow;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface PaymentAuthorizationRowRepository
        extends ReactiveCrudRepository<PaymentAuthorizationRow, String> {

    Mono<PaymentAuthorizationRow> findByPaymentId(String paymentId);
}