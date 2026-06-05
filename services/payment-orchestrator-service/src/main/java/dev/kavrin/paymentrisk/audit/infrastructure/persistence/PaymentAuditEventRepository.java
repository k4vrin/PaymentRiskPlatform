package dev.kavrin.paymentrisk.audit.infrastructure.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive repository for payment audit history rows.
 */
public interface PaymentAuditEventRepository
        extends ReactiveCrudRepository<PaymentAuditEventEntity, Long> {

    Mono<Boolean> existsByEventId(String eventId);

    Flux<PaymentAuditEventEntity> findByPaymentIdOrderByOccurredAtAsc(String paymentId);
}