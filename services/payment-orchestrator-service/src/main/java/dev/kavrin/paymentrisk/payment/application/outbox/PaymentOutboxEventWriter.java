package dev.kavrin.paymentrisk.payment.application.outbox;

import dev.kavrin.paymentrisk.payment.domain.model.Payment;
import reactor.core.publisher.Mono;

public interface PaymentOutboxEventWriter {

    Mono<Void> writeAuthorizationEvents(
            Payment payment,
            String correlationId
    );
}
