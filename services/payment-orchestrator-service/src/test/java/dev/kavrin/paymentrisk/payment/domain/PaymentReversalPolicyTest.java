package dev.kavrin.paymentrisk.payment.domain;

import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKey;
import dev.kavrin.paymentrisk.payment.domain.model.*;
import dev.kavrin.paymentrisk.payment.domain.policy.PaymentReversalPolicy;
import dev.kavrin.paymentrisk.risk.application.dto.RiskRuleHitSummary;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentReversalPolicyTest {

    @Test
    void authorizedPaymentCanBeReversed() {
        Payment payment = authorizedPayment();

        PaymentReversalPolicy.assertReversible(payment);
    }

    @Test
    void declinedPaymentCannotBeReversed() {
        Payment payment = newPayment();
        payment.markRiskPending(
                Instant.parse("2026-06-01T10:00:00Z")
        );
        payment.markDeclined(
                declinedRiskDecision(),
                Instant.parse("2026-06-01T10:00:00Z")
        );

        assertThatThrownBy(() -> PaymentReversalPolicy.assertReversible(payment))
                .isInstanceOf(PaymentStateTransitionException.class)
                .hasMessageContaining("DECLINED");
    }

    @Test
    void failedPaymentCannotBeReversed() {
        Payment payment = newPayment();
        payment.markFailed(
                "risk service unavailable",
                Instant.parse("2026-06-01T10:00:00Z")
        );

        assertThatThrownBy(() -> PaymentReversalPolicy.assertReversible(payment))
                .isInstanceOf(PaymentStateTransitionException.class)
                .hasMessageContaining("FAILED");
    }

    @Test
    void receivedPaymentCannotBeReversed() {
        Payment payment = newPayment();

        assertThatThrownBy(() -> PaymentReversalPolicy.assertReversible(payment))
                .isInstanceOf(PaymentStateTransitionException.class)
                .hasMessageContaining("RECEIVED");
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
        Payment payment = newPayment();
        payment.markRiskPending(
                Instant.parse("2026-06-01T10:00:00Z")
        );
        payment.markAuthorized(
                approvedRiskDecision(),
                AuthorizationCode.of("auth_123"),
                Instant.parse("2026-06-01T10:00:00Z")
        );

        return payment;
    }

    private static PaymentRiskDecision approvedRiskDecision() {
        return new PaymentRiskDecision(
                RiskDecision.APPROVED,
                10,
                List.of("LOW_RISK"),
                "rules-v1",
                Instant.parse("2026-06-01T10:00:00Z")
        );
    }

    private static PaymentRiskDecision declinedRiskDecision() {
        return new PaymentRiskDecision(
                RiskDecision.DECLINED,
                91,
                List.of("HIGH_RISK"),
                "rules-v1",
                Instant.parse("2026-06-01T10:00:00Z")
        );
    }
}