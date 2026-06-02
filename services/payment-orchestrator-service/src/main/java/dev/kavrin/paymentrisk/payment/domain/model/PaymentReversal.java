package dev.kavrin.paymentrisk.payment.domain.model;

import java.time.Instant;
import java.util.Objects;

public record PaymentReversal(
        ReversalId reversalId,
        PaymentId paymentId,
        ReversalReason reason,
        ReversalStatus status,
        Instant requestedAt,
        Instant reversedAt
) {

    public PaymentReversal {
        Objects.requireNonNull(reversalId, "reversalId must not be null");
        Objects.requireNonNull(paymentId, "paymentId must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(requestedAt, "requestedAt must not be null");

        if (status == ReversalStatus.REVERSED && reversedAt == null) {
            throw new IllegalArgumentException(
                    "reversedAt is required when reversal status is REVERSED"
            );
        }
    }

    public static PaymentReversal reversed(
            ReversalId reversalId,
            PaymentId paymentId,
            ReversalReason reason,
            Instant requestedAt,
            Instant reversedAt
    ) {
        return new PaymentReversal(
                reversalId,
                paymentId,
                reason,
                ReversalStatus.REVERSED,
                requestedAt,
                reversedAt
        );
    }
}