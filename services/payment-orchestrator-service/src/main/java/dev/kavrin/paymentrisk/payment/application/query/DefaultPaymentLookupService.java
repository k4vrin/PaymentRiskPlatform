package dev.kavrin.paymentrisk.payment.application.query;

import dev.kavrin.paymentrisk.payment.domain.model.PaymentId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class DefaultPaymentLookupService implements PaymentLookupService {

    private final PaymentDetailsLookupPort paymentDetailsLookupPort;

    @Override
    public Mono<PaymentDetailsResult> getPaymentDetails(PaymentId paymentId) {
        return paymentDetailsLookupPort.findByPaymentId(paymentId);
    }
}
