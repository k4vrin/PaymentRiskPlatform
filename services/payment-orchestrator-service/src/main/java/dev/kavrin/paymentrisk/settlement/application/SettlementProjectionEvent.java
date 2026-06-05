package dev.kavrin.paymentrisk.settlement.application;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

/**
 * Normalized payment outcome event consumed by the settlement projection.
 */
public record SettlementProjectionEvent(
        String eventId,
        String eventType,
        String paymentId,
        String aggregateType,
        String schemaVersion,
        String correlationId,
        Instant occurredAt,
        JsonNode payload
) {
}