package dev.kavrin.paymentrisk.audit.application;

/**
 * Raised when the audit consumer receives an event envelope with a schema
 * version it does not understand.
 *
 * <p>Later, poison-message handling can catch this kind of failure and persist
 * a dead-letter record.</p>
 */
public class UnsupportedPaymentAuditEventSchemaException extends RuntimeException {

    public UnsupportedPaymentAuditEventSchemaException(String eventId, String schemaVersion) {
        super("Unsupported payment audit event schemaVersion=%s for eventId=%s"
                .formatted(schemaVersion, eventId));
    }
}