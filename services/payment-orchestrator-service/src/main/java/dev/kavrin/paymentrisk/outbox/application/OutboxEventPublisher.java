package dev.kavrin.paymentrisk.outbox.application;

import dev.kavrin.paymentrisk.outbox.domain.OutboxEvent;
import reactor.core.publisher.Mono;

public interface OutboxEventPublisher {

    Mono<Void> publish(OutboxEvent event);
}
