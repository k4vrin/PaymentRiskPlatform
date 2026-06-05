package dev.kavrin.paymentrisk.security.domain;

/**
 * Logical API groups used by the authorization matrix.
 */
public enum EndpointGroup {

    PAYMENT_API,
    OPS_API,
    AUDIT_READ_API,
    INTERNAL_SERVICE_API,
    HEALTH_API
}
