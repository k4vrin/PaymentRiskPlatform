package dev.kavrin.paymentrisk.ops.application.payment;

import dev.kavrin.paymentrisk.payment.domain.model.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class OpsPaymentSearchResultTest {

    @Test
    void createsSearchResultItemWithSummaries() {
        var item = new OpsPaymentSearchItem(
                "pay_test_123",
                "merchant_123",
                "customer_123",
                10_000L,
                "USD",
                PaymentStatus.REVERSED,
                Optional.of("order_123"),
                Optional.of(new OpsPaymentSearchItem.AuthorizationSummary(
                        "AUTHORIZED",
                        Optional.of("AUTH123456"),
                        Optional.of(Instant.parse("2026-06-01T10:00:00Z"))
                )),
                Optional.of(new OpsPaymentSearchItem.RiskSummary(
                        "APPROVED",
                        30,
                        "risk-rules-v1",
                        Instant.parse("2026-06-01T10:00:01Z")
                )),
                Optional.of(new OpsPaymentSearchItem.ReversalSummary(
                        "rev_test_123",
                        "REVERSED",
                        "customer_requested",
                        Instant.parse("2026-06-01T10:05:00Z")
                )),
                Instant.parse("2026-06-01T09:59:59Z"),
                Instant.parse("2026-06-01T10:05:00Z")
        );

        var result = new OpsPaymentSearchResult(
                List.of(item),
                Optional.of("cursor_next_123")
        );

        assertThat(result.items()).hasSize(1);
        assertThat(result.nextPageToken()).contains("cursor_next_123");

        var first = result.items().getFirst();

        assertThat(first.paymentId()).isEqualTo("pay_test_123");
        assertThat(first.status()).isEqualTo(PaymentStatus.REVERSED);
        assertThat(first.authorization()).isPresent();
        assertThat(first.risk()).isPresent();
        assertThat(first.reversal()).isPresent();
    }

    @Test
    void normalizesNullOptionalsToEmpty() {
        var item = new OpsPaymentSearchItem(
                "pay_test_123",
                "merchant_123",
                "customer_123",
                10_000L,
                "USD",
                PaymentStatus.AUTHORIZED,
                null,
                null,
                null,
                null,
                Instant.parse("2026-06-01T09:59:59Z"),
                Instant.parse("2026-06-01T10:00:00Z")
        );

        assertThat(item.externalReference()).isEmpty();
        assertThat(item.authorization()).isEmpty();
        assertThat(item.risk()).isEmpty();
        assertThat(item.reversal()).isEmpty();
    }

    @Test
    void copiesItemsDefensively() {
        var item = new OpsPaymentSearchItem(
                "pay_test_123",
                "merchant_123",
                "customer_123",
                10_000L,
                "USD",
                PaymentStatus.AUTHORIZED,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Instant.parse("2026-06-01T09:59:59Z"),
                Instant.parse("2026-06-01T10:00:00Z")
        );

        var result = new OpsPaymentSearchResult(
                List.of(item),
                Optional.empty()
        );

        assertThat(result.items()).containsExactly(item);
    }
}
