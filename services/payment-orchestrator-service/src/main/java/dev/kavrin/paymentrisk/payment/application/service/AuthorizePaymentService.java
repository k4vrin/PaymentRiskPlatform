package dev.kavrin.paymentrisk.payment.application.service;

import dev.kavrin.paymentrisk.payment.application.command.AuthorizePaymentCommand;
import dev.kavrin.paymentrisk.payment.application.command.AuthorizePaymentResult;
import reactor.core.publisher.Mono;

public interface AuthorizePaymentService {

    Mono<AuthorizePaymentResult> authorize(AuthorizePaymentCommand command);
}
