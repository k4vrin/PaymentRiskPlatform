package dev.kavrin.paymentrisk.settlement.infrastructure.persistence;

import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Settlement read model built from payment outcome events.
 *
 * <p>This table exists for fast settlement queries and reports. It is updated
 * by Kafka consumers and can be rebuilt from the event stream if needed.</p>
 */
@Builder(toBuilder = true)
@Table("settlement_projections")
public record SettlementProjectionEntity(
        @Id Long id,
        String paymentId,
        String merchantId,
        String customerId,
        Long amountMinor,
        String currency,
        String status,
        LocalDate businessDate,
        String lastEventId,
        String lastEventType,
        String correlationId,
        Instant authorizedAt,
        Instant declinedAt,
        Instant reversedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
