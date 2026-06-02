package dev.kavrin.paymentrisk.payment.domain;

import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKey;
import dev.kavrin.paymentrisk.payment.domain.model.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentReversalAggregateTest {

    @Test
    void authorizedPaymentCanBeMarkedReversed() {
        var payment = authorizedPayment();
        var originalAuthorization = payment.getAuthorization();
        var reversalId = ReversalId.of("rev_123");
        var reversedAt = Instant.parse("2026-06-01T10:05:00Z");

        payment.markReversed(
                reversalId,
                ReversalReason.of("merchant_requested"),
                reversedAt
        );

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REVERSED);
        assertThat(payment.getAuthorization()).isEqualTo(originalAuthorization);
        assertThat(payment.getUpdatedAt()).isEqualTo(reversedAt);

        assertThat(payment.reversal()).isPresent();
        assertThat(payment.reversal().get())
                .satisfies(reversal -> {
                    assertThat(reversal.reversalId()).isEqualTo(reversalId);
                    assertThat(reversal.paymentId()).isEqualTo(payment.getId());
                    assertThat(reversal.reason()).isEqualTo(ReversalReason.of("merchant_requested"));
                    assertThat(reversal.status()).isEqualTo(ReversalStatus.REVERSED);
                    assertThat(reversal.requestedAt()).isEqualTo(reversedAt);
                    assertThat(reversal.reversedAt()).isEqualTo(reversedAt);
                });
    }

    @Test
    void receivedPaymentCannotBeMarkedReversed() {
        var payment = newPayment();

        assertThatThrownBy(() ->
                payment.markReversed(
                        ReversalId.of("rev_received"),
                        ReversalReason.of("merchant_requested"),
                        Instant.parse("2026-06-01T10:05:00Z")
                )
        )
                .isInstanceOf(PaymentStateTransitionException.class)
                .hasMessageContaining("RECEIVED");
    }

    @Test
    void declinedPaymentCannotBeMarkedReversed() {
        var payment = newPayment();

        payment.markRiskPending(Instant.parse("2026-06-01T10:00:00Z"));
        payment.markDeclined(
                declinedRiskDecision(),
                Instant.parse("2026-06-01T10:01:00Z")
        );

        assertThatThrownBy(() ->
                payment.markReversed(
                        ReversalId.of("rev_declined"),
                        ReversalReason.of("merchant_requested"),
                        Instant.parse("2026-06-01T10:05:00Z")
                )
        )
                .isInstanceOf(PaymentStateTransitionException.class)
                .hasMessageContaining("DECLINED");
    }

    @Test
    void failedPaymentCannotBeMarkedReversed() {
        var payment = newPayment();

        payment.markFailed("gateway_timeout", Instant.parse("2026-06-01T10:01:00Z"));

        assertThatThrownBy(() ->
                payment.markReversed(
                        ReversalId.of("rev_failed"),
                        ReversalReason.of("merchant_requested"),
                        Instant.parse("2026-06-01T10:05:00Z")
                )
        )
                .isInstanceOf(PaymentStateTransitionException.class)
                .hasMessageContaining("FAILED");
    }

    @Test
    void alreadyReversedPaymentCannotBeMarkedReversedAgain() {
        var payment = authorizedPayment();

        payment.markReversed(
                ReversalId.of("rev_first"),
                ReversalReason.of("first_reversal"),
                Instant.parse("2026-06-01T10:05:00Z")
        );

        assertThatThrownBy(() ->
                payment.markReversed(
                        ReversalId.of("rev_second"),
                        ReversalReason.of("second_reversal"),
                        Instant.parse("2026-06-01T10:06:00Z")
                )
        )
                .isInstanceOf(PaymentStateTransitionException.class)
                .hasMessageContaining("REVERSED");
    }

    @Test
    void reversalInputsAreRequired() {
        var payment = authorizedPayment();
        var reversedAt = Instant.parse("2026-06-01T10:05:00Z");

        assertThatThrownBy(() ->
                payment.markReversed(
                        null,
                        ReversalReason.of("merchant_requested"),
                        reversedAt
                )
        )
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() ->
                payment.markReversed(
                        ReversalId.of("rev_123"),
                        null,
                        reversedAt
                )
        )
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() ->
                payment.markReversed(
                        ReversalId.of("rev_123"),
                        ReversalReason.of("merchant_requested"),
                        null
                )
        )
                .isInstanceOf(NullPointerException.class);
    }

    private static Payment newPayment() {
        return Payment.newAuthorizationAttempt(
                PaymentId.of("pay_123"),
                MerchantId.of("merchant_123"),
                CustomerId.of("customer_123"),
                Money.of(10_000, "USD"),
                PaymentMethodToken.of("pm_token_1234567890"),
                DeviceFingerprint.of("device_123"),
                ExternalReference.ofNullable("order_123"),
                IdempotencyKey.of("idem_123"),
                Instant.parse("2026-06-01T09:59:00Z")
        );
    }

    private static Payment authorizedPayment() {
        var payment = newPayment();

        payment.markRiskPending(Instant.parse("2026-06-01T10:00:00Z"));

        payment.markAuthorized(
                approvedRiskDecision(),
                AuthorizationCode.of("auth_123"),
                Instant.parse("2026-06-01T10:01:00Z")
        );

        return payment;
    }

    private static PaymentRiskDecision approvedRiskDecision() {
        return new PaymentRiskDecision(
                RiskDecision.APPROVED,
                10,
                List.of("LOW_RISK"),
                "rules-v1",
                Instant.parse("2026-06-01T10:00:30Z")
        );
    }

    private static PaymentRiskDecision declinedRiskDecision() {
        return new PaymentRiskDecision(
                RiskDecision.DECLINED,
                91,
                List.of("HIGH_RISK"),
                "rules-v1",
                Instant.parse("2026-06-01T10:00:30Z")
        );
    }
}
