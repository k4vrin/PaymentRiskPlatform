package dev.kavrin.paymentrisk.payment.application.service;

import dev.kavrin.paymentrisk.payment.application.command.ReversePaymentCommand;
import dev.kavrin.paymentrisk.payment.application.command.ReversePaymentResult;
import reactor.core.publisher.Mono;

public interface PaymentReversalService {

    Mono<ReversePaymentResult> reverse(ReversePaymentCommand command);
}
