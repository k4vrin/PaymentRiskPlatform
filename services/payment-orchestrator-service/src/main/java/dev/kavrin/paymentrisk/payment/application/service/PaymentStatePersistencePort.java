package dev.kavrin.paymentrisk.payment.application.service;

import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKey;
import dev.kavrin.paymentrisk.payment.domain.model.Payment;
import dev.kavrin.paymentrisk.payment.domain.model.PaymentId;
import reactor.core.publisher.Mono;

public interface PaymentStatePersistencePort {

    Mono<Payment> save(Payment payment);

    Mono<Payment> findByPaymentId(PaymentId paymentId);

    Mono<Payment> saveReversal(Payment payment, IdempotencyKey reversalIdempotencyKey);
}
