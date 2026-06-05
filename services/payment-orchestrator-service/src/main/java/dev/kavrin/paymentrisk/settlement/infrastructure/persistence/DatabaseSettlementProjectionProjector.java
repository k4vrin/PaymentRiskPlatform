package dev.kavrin.paymentrisk.settlement.infrastructure.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import dev.kavrin.paymentrisk.settlement.application.SettlementProjectionEvent;
import dev.kavrin.paymentrisk.settlement.application.SettlementProjectionProjector;
import dev.kavrin.paymentrisk.settlement.domain.SettlementProjectionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Persists settlement projections from consumed payment outcome events.
 *
 * <p>Authorized payments become settlement-ready, declined payments are tracked
 * as not settled, and reversal events move existing settlement rows to reversed.</p>
 */
@Repository
@RequiredArgsConstructor
public class DatabaseSettlementProjectionProjector implements SettlementProjectionProjector {

    private final SettlementProjectionRepository repository;
    private final Clock clock;

    @Override
    public Mono<Void> project(SettlementProjectionEvent event) {
        return switch (event.eventType()) {
            case "PaymentAuthorized" -> upsertAuthorizationOutcome(
                    event,
                    SettlementProjectionStatus.SETTLEMENT_READY,
                    event.occurredAt(),
                    null
            );

            case "PaymentDeclined" -> upsertAuthorizationOutcome(
                    event,
                    SettlementProjectionStatus.NOT_SETTLED,
                    null,
                    event.occurredAt()
            );

            case "PaymentReversed" -> markReversed(event);

            default -> Mono.error(new IllegalArgumentException(
                    "Unsupported settlement event type: " + event.eventType()
            ));
        };
    }

    private Mono<Void> upsertAuthorizationOutcome(
            SettlementProjectionEvent event,
            SettlementProjectionStatus status,
            Instant authorizedAt,
            Instant declinedAt
    ) {
        var now = Instant.now(clock);
        var businessDate = businessDate(event);

        return repository.findByPaymentId(event.paymentId())
                .map(existing -> existing.toBuilder()
                        .merchantId(text(event.payload(), "merchantId"))
                        .customerId(text(event.payload(), "customerId"))
                        .amountMinor(longValue(event.payload(), "amountMinor"))
                        .currency(text(event.payload(), "currency"))
                        .status(status.name())
                        .businessDate(businessDate)
                        .lastEventId(event.eventId())
                        .lastEventType(event.eventType())
                        .correlationId(event.correlationId())
                        .authorizedAt(authorizedAt)
                        .declinedAt(declinedAt)
                        .updatedAt(now)
                        .build())
                .switchIfEmpty(Mono.fromSupplier(() -> SettlementProjectionEntity.builder()
                        .paymentId(event.paymentId())
                        .merchantId(text(event.payload(), "merchantId"))
                        .customerId(text(event.payload(), "customerId"))
                        .amountMinor(longValue(event.payload(), "amountMinor"))
                        .currency(text(event.payload(), "currency"))
                        .status(status.name())
                        .businessDate(businessDate)
                        .lastEventId(event.eventId())
                        .lastEventType(event.eventType())
                        .correlationId(event.correlationId())
                        .authorizedAt(authorizedAt)
                        .declinedAt(declinedAt)
                        .createdAt(now)
                        .updatedAt(now)
                        .build()))
                .flatMap(repository::save)
                .then();
    }

    private Mono<Void> markReversed(SettlementProjectionEvent event) {
        var now = Instant.now(clock);
        var businessDate = businessDate(event);

        return repository.findByPaymentId(event.paymentId())
                .map(existing -> existing.toBuilder()
                        .status(SettlementProjectionStatus.REVERSED.name())
                        .businessDate(businessDate)
                        .lastEventId(event.eventId())
                        .lastEventType(event.eventType())
                        .correlationId(event.correlationId())
                        .reversedAt(event.occurredAt())
                        .updatedAt(now)
                        .build())
                .switchIfEmpty(Mono.fromSupplier(() -> SettlementProjectionEntity.builder()
                        .paymentId(event.paymentId())
                        .merchantId(text(event.payload(), "merchantId"))
                        .customerId(text(event.payload(), "customerId"))
                        .amountMinor(longValue(event.payload(), "amountMinor"))
                        .currency(text(event.payload(), "currency"))
                        .status(SettlementProjectionStatus.REVERSED.name())
                        .businessDate(businessDate)
                        .lastEventId(event.eventId())
                        .lastEventType(event.eventType())
                        .correlationId(event.correlationId())
                        .reversedAt(event.occurredAt())
                        .createdAt(now)
                        .updatedAt(now)
                        .build()))
                .flatMap(repository::save)
                .then();
    }

    private static String text(JsonNode payload, String fieldName) {
        var node = payload.get(fieldName);
        return node == null || node.isNull() ? null : node.asText();
    }

    private static Long longValue(JsonNode payload, String fieldName) {
        var node = payload.get(fieldName);
        return node == null || node.isNull() ? null : node.asLong();
    }

    private static LocalDate businessDate(SettlementProjectionEvent event) {
        return event.occurredAt().atZone(ZoneOffset.UTC).toLocalDate();
    }
}
