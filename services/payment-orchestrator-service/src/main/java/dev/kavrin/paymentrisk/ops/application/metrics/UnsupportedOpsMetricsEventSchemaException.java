package dev.kavrin.paymentrisk.ops.application.metrics;

/**
 * Raised when the ops metrics consumer receives an unsupported event schema.
 */
public class UnsupportedOpsMetricsEventSchemaException extends RuntimeException {

    public UnsupportedOpsMetricsEventSchemaException(String eventId, String schemaVersion) {
        super("Unsupported ops metrics event schemaVersion=%s for eventId=%s"
                .formatted(schemaVersion, eventId));
    }
}