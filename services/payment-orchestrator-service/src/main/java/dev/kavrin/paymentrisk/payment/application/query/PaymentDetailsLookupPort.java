package dev.kavrin.paymentrisk.payment.application.query;

import dev.kavrin.paymentrisk.payment.domain.model.PaymentId;
import reactor.core.publisher.Mono;

public interface PaymentDetailsLookupPort {

    Mono<PaymentDetailsResult> findByPaymentId(PaymentId paymentId);
}
