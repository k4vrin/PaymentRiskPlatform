package dev.kavrin.paymentrisk.payment.domain.model;

import java.time.Instant;

public sealed interface PaymentAuthorization
        permits PaymentAuthorization.Requested,
        PaymentAuthorization.RiskPending,
        PaymentAuthorization.Authorized,
        PaymentAuthorization.Declined,
        PaymentAuthorization.Failed {

    Instant requestedAt();

    record Requested(
            Instant requestedAt
    ) implements PaymentAuthorization {
    }

    record RiskPending(
            Instant requestedAt,
            Instant riskPendingAt
    ) implements PaymentAuthorization {
    }

    record Authorized(
            Instant requestedAt,
            Instant riskPendingAt,
            AuthorizationCode authorizationCode,
            Instant authorizedAt
    ) implements PaymentAuthorization {
    }

    record Declined(
            Instant requestedAt,
            Instant riskPendingAt,
            Instant declinedAt
    ) implements PaymentAuthorization {
    }

    record Failed(
            Instant requestedAt,
            String failureReason,
            Instant failedAt
    ) implements PaymentAuthorization {
    }
}