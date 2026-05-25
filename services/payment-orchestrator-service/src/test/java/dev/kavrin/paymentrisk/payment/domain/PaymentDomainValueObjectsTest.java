package dev.kavrin.paymentrisk.payment.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentDomainValueObjectsTest {

    @Test
    void paymentIdCanBeGenerated() {
        PaymentId paymentId = PaymentId.generate();

        assertThat(paymentId.value()).startsWith("pay_");
    }

    @Test
    void requiredIdentifiersRejectBlankValues() {
        assertThatThrownBy(() -> MerchantId.of(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("merchantId is required.");

        assertThatThrownBy(() -> CustomerId.of(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("customerId is required.");
    }

    @Test
    void authorizationCodeCanBeGenerated() {
        AuthorizationCode authorizationCode = AuthorizationCode.generate();

        assertThat(authorizationCode.value()).startsWith("AUTH-");
        assertThat(authorizationCode.value()).hasSize(17);
    }

    @Test
    void moneyRequiresPositiveMinorAmountAndIsoCurrency() {
        Money money = Money.of(1299, "usd");

        assertThat(money.amountMinor()).isEqualTo(1299);
        assertThat(money.currencyCode()).isEqualTo("USD");

        assertThatThrownBy(() -> Money.of(0, "USD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("amountMinor must be positive.");

        assertThatThrownBy(() -> Money.of(100, "BAD"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sensitiveValueObjectsExposeMaskedValues() {
        PaymentMethodToken token = PaymentMethodToken.of("tok_1234567890");
        DeviceFingerprint fingerprint = DeviceFingerprint.of("device-fingerprint-abcdef");

        assertThat(token.masked()).isEqualTo("****7890");
        assertThat(fingerprint.masked()).isEqualTo("device...cdef");
    }

    @Test
    void externalReferenceIsOptionalWhenBlank() {
        assertThat(ExternalReference.optional(null)).isEmpty();
        assertThat(ExternalReference.optional(" ")).isEmpty();
        assertThat(ExternalReference.optional("merchant-order-123"))
                .contains(ExternalReference.of("merchant-order-123"));
    }

    @Test
    void idempotencyKeyRequiresSafeStableValue() {
        IdempotencyKey key = IdempotencyKey.of("merchant-123:request-456");

        assertThat(key.value()).isEqualTo("merchant-123:request-456");

        assertThatThrownBy(() -> IdempotencyKey.of("too-short"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("idempotencyKey must be at least 16 characters.");

        assertThatThrownBy(() -> IdempotencyKey.of("merchant 123 request 456"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("idempotencyKey contains unsupported characters.");
    }
}
