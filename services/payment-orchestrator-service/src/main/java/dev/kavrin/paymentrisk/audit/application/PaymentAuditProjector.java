package dev.kavrin.paymentrisk.audit.application;

import reactor.core.publisher.Mono;

/**
 * Projects consumed payment lifecycle events into audit/history storage.
 *
 * <p>The Kafka consumer depends on this port instead of a database repository
 * so the messaging layer remains independent of the final audit table shape.</p>
 */
public interface PaymentAuditProjector {

    Mono<Void> project(PaymentAuditProjection projection);
}