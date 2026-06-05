package dev.kavrin.paymentrisk.settlement.application;

/**
 * Raised when a settlement event envelope uses an unsupported schema version.
 */
public class UnsupportedSettlementEventSchemaException extends RuntimeException {

    public UnsupportedSettlementEventSchemaException(String eventId, String schemaVersion) {
        super("Unsupported settlement event schemaVersion=%s for eventId=%s"
                .formatted(schemaVersion, eventId));
    }
}