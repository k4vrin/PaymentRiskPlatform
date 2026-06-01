package dev.kavrin.paymentrisk.payment.application.query;

import dev.kavrin.paymentrisk.payment.domain.model.PaymentId;
import reactor.core.publisher.Mono;

public interface PaymentLookupService {

    Mono<PaymentDetailsResult> getPaymentDetails(PaymentId paymentId);
}
