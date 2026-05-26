package dev.kavrin.paymentrisk.payment.domain.model;

import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKey;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.Objects;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Payment {

    private final PaymentId id;
    private final MerchantId merchantId;
    private final CustomerId customerId;
    private final Money amount;
    private final PaymentMethodToken paymentMethodToken;
    private final DeviceFingerprint deviceFingerprint;
    private final ExternalReference externalReference;
    private final IdempotencyKey idempotencyKey;
    private final Instant createdAt;

    private PaymentStatus status;
    private PaymentAuthorization authorization;
    private PaymentRiskDecision riskDecision;
    private Instant updatedAt;

    public static Payment newAuthorizationAttempt(
            PaymentId id,
            MerchantId merchantId,
            CustomerId customerId,
            Money amount,
            PaymentMethodToken paymentMethodToken,
            DeviceFingerprint deviceFingerprint,
            ExternalReference externalReference,
            IdempotencyKey idempotencyKey,
            Instant createdAt
    ) {
        Instant now = Objects.requireNonNull(createdAt);

        Payment payment = new Payment(
                Objects.requireNonNull(id),
                Objects.requireNonNull(merchantId),
                Objects.requireNonNull(customerId),
                Objects.requireNonNull(amount),
                Objects.requireNonNull(paymentMethodToken),
                Objects.requireNonNull(deviceFingerprint),
                externalReference,
                Objects.requireNonNull(idempotencyKey),
                now
        );

        payment.status = PaymentStatus.RECEIVED;
        payment.authorization = new PaymentAuthorization.Requested(now);
        payment.updatedAt = now;
        return payment;
    }

    public static Payment restore(
            PaymentId id,
            MerchantId merchantId,
            CustomerId customerId,
            Money amount,
            PaymentMethodToken paymentMethodToken,
            DeviceFingerprint deviceFingerprint,
            ExternalReference externalReference,
            IdempotencyKey idempotencyKey,
            PaymentStatus status,
            PaymentAuthorization authorization,
            PaymentRiskDecision riskDecision,
            Instant createdAt,
            Instant updatedAt
    ) {
        Payment payment = new Payment(
                Objects.requireNonNull(id),
                Objects.requireNonNull(merchantId),
                Objects.requireNonNull(customerId),
                Objects.requireNonNull(amount),
                Objects.requireNonNull(paymentMethodToken),
                Objects.requireNonNull(deviceFingerprint),
                externalReference,
                Objects.requireNonNull(idempotencyKey),
                Objects.requireNonNull(createdAt)
        );

        payment.status = Objects.requireNonNull(status);
        payment.authorization = Objects.requireNonNull(authorization);
        payment.riskDecision = riskDecision;
        payment.updatedAt = Objects.requireNonNull(updatedAt);
        return payment;
    }

    public void markRiskPending(Instant occurredAt) {
        requireStatus(PaymentStatus.RECEIVED);

        Instant now = Objects.requireNonNull(occurredAt);

        status = PaymentStatus.RISK_PENDING;
        this.authorization = new PaymentAuthorization.RiskPending(
                authorization.requestedAt(),
                now
        );
        updatedAt = now;
    }

    public void markAuthorized(
            PaymentRiskDecision riskDecision,
            AuthorizationCode authorizationCode,
            Instant occurredAt
    ) {
        requireStatus(PaymentStatus.RISK_PENDING);

        if (!riskDecision.isApproved()) {
            throw new PaymentStateTransitionException(
                    "Only approved risk decisions can authorize payment"
            );
        }

        Instant now = Objects.requireNonNull(occurredAt);

        this.status = PaymentStatus.AUTHORIZED;
        this.riskDecision = riskDecision;
        this.authorization = new PaymentAuthorization.Authorized(
                authorization.requestedAt(),
                ((PaymentAuthorization.RiskPending) authorization).riskPendingAt(),
                authorizationCode,
                now
        );
        this.updatedAt = now;
    }

    public void markDeclined(
            PaymentRiskDecision riskDecision,
            Instant occurredAt
    ) {
        requireStatus(PaymentStatus.RISK_PENDING);

        if (!riskDecision.isDeclined()) {
            throw new PaymentStateTransitionException(
                    "Only declined risk decisions can decline payment"
            );
        }

        Instant now = Objects.requireNonNull(occurredAt);

        this.status = PaymentStatus.DECLINED;
        this.riskDecision = riskDecision;
        this.authorization = new PaymentAuthorization.Declined(
                authorization.requestedAt(),
                ((PaymentAuthorization.RiskPending) authorization).riskPendingAt(),
                now
        );
        this.updatedAt = now;
    }

    public void markFailed(String reason, Instant occurredAt) {
        if (status == PaymentStatus.AUTHORIZED
                || status == PaymentStatus.DECLINED
                || status == PaymentStatus.REVERSED) {
            throw new PaymentStateTransitionException(
                    "Terminal payment cannot be failed: " + status
            );
        }

        Instant now = Objects.requireNonNull(occurredAt);

        this.status = PaymentStatus.FAILED;
        this.authorization = new PaymentAuthorization.Failed(
                authorization.requestedAt(),
                reason,
                now
        );
        this.updatedAt = now;
    }

    private void requireStatus(PaymentStatus expected) {
        if (status != expected) {
            throw new PaymentStateTransitionException(
                    "Invalid payment state transition. Expected " + expected + " but was " + status
            );
        }
    }
}