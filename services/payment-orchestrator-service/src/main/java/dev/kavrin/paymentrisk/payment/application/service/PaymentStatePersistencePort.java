package dev.kavrin.paymentrisk.payment.application.service;

import dev.kavrin.paymentrisk.payment.domain.model.Payment;
import reactor.core.publisher.Mono;

public interface PaymentStatePersistencePort {

    Mono<Payment> save(Payment payment);
}