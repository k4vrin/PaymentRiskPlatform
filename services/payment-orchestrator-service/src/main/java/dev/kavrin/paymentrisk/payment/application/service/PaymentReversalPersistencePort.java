package dev.kavrin.paymentrisk.payment.application.service;

import dev.kavrin.paymentrisk.payment.application.command.ReversePaymentResult;
import reactor.core.publisher.Mono;

public interface PaymentReversalPersistencePort {

    Mono<ReversePaymentResult> saveReversal(ReversePaymentResult result);
}
