package dev.kavrin.paymentrisk.ops.infrastructure.persistence;

import dev.kavrin.paymentrisk.ops.application.metrics.OpsMetricsEvent;
import dev.kavrin.paymentrisk.ops.application.metrics.OpsMetricsProjector;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Updates durable ops counters from payment and platform events.
 *
 * <p>The projection is intentionally simple: every event increments a generic
 * event-type counter, and selected business events increment more specific
 * counters useful for dashboards.</p>
 */
@Repository
@RequiredArgsConstructor
public class DatabaseOpsMetricsProjector implements OpsMetricsProjector {

    private final OpsEventMetricRepository repository;
    private final Clock clock;

    @Override
    public Mono<Void> project(OpsMetricsEvent event) {
        return Flux.fromIterable(metricKeys(event))
                .concatMap(metricKey -> increment(metricKey, event))
                .then();
    }

    private Mono<Void> increment(String metricKey, OpsMetricsEvent event) {
        var now = Instant.now(clock);

        return repository.findByMetricKey(metricKey)
                .map(existing -> existing.toBuilder()
                        .metricValue(existing.metricValue() + 1)
                        .lastEventId(event.eventId())
                        .lastEventType(event.eventType())
                        .lastCorrelationId(event.correlationId())
                        .lastObservedAt(event.occurredAt())
                        .updatedAt(now)
                        .build())
                .switchIfEmpty(Mono.fromSupplier(() -> OpsEventMetricEntity.builder()
                        .metricKey(metricKey)
                        .metricValue(1L)
                        .lastEventId(event.eventId())
                        .lastEventType(event.eventType())
                        .lastCorrelationId(event.correlationId())
                        .lastObservedAt(event.occurredAt())
                        .createdAt(now)
                        .updatedAt(now)
                        .build()))
                .flatMap(repository::save)
                .then();
    }

    private static List<String> metricKeys(OpsMetricsEvent event) {
        return switch (event.eventType()) {
            case "PaymentAuthorizationRequested" -> List.of(
                    "events.total",
                    "events.payment.authorization.requested"
            );

            case "RiskScoreCompleted" -> List.of(
                    "events.total",
                    "events.risk.score.completed"
            );

            case "PaymentAuthorized" -> List.of(
                    "events.total",
                    "events.payment.authorization.completed",
                    "payments.authorized"
            );

            case "PaymentDeclined" -> List.of(
                    "events.total",
                    "events.payment.authorization.completed",
                    "payments.declined"
            );

            case "PaymentReversed" -> List.of(
                    "events.total",
                    "events.payment.reversal.completed",
                    "payments.reversed"
            );

            case "DeadLetterRecorded" -> List.of(
                    "events.total",
                    "events.platform.dead-letter.recorded",
                    "dead_letters.recorded"
            );

            default -> List.of(
                    "events.total",
                    "events.unknown"
            );
        };
    }
}