package dev.kavrin.paymentrisk.shared.api.error;

import com.fasterxml.jackson.annotation.JsonValue;

public sealed interface ApiErrorCode permits
        ApiErrorCode.Business,
        ApiErrorCode.Security,
        ApiErrorCode.Validation,
        ApiErrorCode.Infrastructure {

    @JsonValue
    default String code() {
        return ((Enum<?>) this).name();
    }

    enum Business implements ApiErrorCode {
        RESOURCE_NOT_FOUND,
        PAYMENT_NOT_FOUND,
        MERCHANT_NOT_FOUND,
        CUSTOMER_NOT_FOUND,
        DUPLICATE_IDEMPOTENCY_KEY,
        IDEMPOTENCY_KEY_CONFLICT,
        INVALID_IDEMPOTENCY_KEY,
        PAYMENT_STATE_CONFLICT,
        PAYMENT_NOT_REVERSIBLE,
        REVERSAL_ALREADY_EXISTS,
        OUTBOX_EVENT_NOT_REPLAYABLE
    }

    enum Security implements ApiErrorCode {
        UNAUTHORIZED,
        FORBIDDEN,
        AUTHENTICATION_REQUIRED,
        ACCESS_DENIED,
        INVALID_ACCESS_TOKEN,
        EXPIRED_ACCESS_TOKEN,
        INSUFFICIENT_SCOPE,
        SUSPICIOUS_ACTIVITY_DETECTED
    }

    enum Validation implements ApiErrorCode {
        VALIDATION_FAILED,
        INVALID_REQUEST,
        MALFORMED_REQUEST
    }

    enum Infrastructure implements ApiErrorCode {
        RISK_SERVICE_TIMEOUT,
        DOWNSTREAM_UNAVAILABLE,
        RATE_LIMIT_EXCEEDED,
        INTERNAL_ERROR
    }
}
