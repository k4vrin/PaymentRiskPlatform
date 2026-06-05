package dev.kavrin.paymentrisk.audit.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kavrin.paymentrisk.audit.application.PaymentAuditProjection;
import dev.kavrin.paymentrisk.audit.application.PaymentAuditProjector;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Instant;

/**
 * Persists payment lifecycle events into the payment audit/history projection.
 *
 * <p>The audit projection is append-only. Existing rows are not updated because
 * audit history should describe what happened over time, not only the latest state.</p>
 */
@Repository
@RequiredArgsConstructor
public class DatabasePaymentAuditProjector implements PaymentAuditProjector {

    private final PaymentAuditEventRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Override
    public Mono<Void> project(PaymentAuditProjection projection) {
        return Mono.fromCallable(() -> toEntity(projection))
                .flatMap(repository::save)
                .then();
    }

    private PaymentAuditEventEntity toEntity(PaymentAuditProjection projection) throws JsonProcessingException {
        return PaymentAuditEventEntity.builder()
                .eventId(projection.eventId())
                .eventType(projection.eventType())
                .paymentId(projection.aggregateId())
                .aggregateType(projection.aggregateType())
                .schemaVersion(projection.schemaVersion())
                .correlationId(projection.correlationId())
                .occurredAt(projection.occurredAt())
                .payloadJson(objectMapper.writeValueAsString(projection.payload()))
                .createdAt(Instant.now(clock))
                .build();
    }
}
