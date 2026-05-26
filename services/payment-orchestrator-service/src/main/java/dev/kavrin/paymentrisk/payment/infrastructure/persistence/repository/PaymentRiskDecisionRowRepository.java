package dev.kavrin.paymentrisk.payment.infrastructure.persistence.repository;

import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.PaymentRiskDecisionRow;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface PaymentRiskDecisionRowRepository
        extends ReactiveCrudRepository<PaymentRiskDecisionRow, String> {

    Mono<PaymentRiskDecisionRow> findByPaymentId(String paymentId);
}