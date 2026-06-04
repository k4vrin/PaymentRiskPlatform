package dev.kavrin.paymentrisk.ops.application;

import dev.kavrin.paymentrisk.payment.domain.model.MerchantId;
import dev.kavrin.paymentrisk.payment.domain.model.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpsPaymentSearchRequestTest {

    @Test
    void createsSearchRequestWithFilters() {
        var request = OpsPaymentSearchRequest.firstPage(
                Optional.of(PaymentStatus.AUTHORIZED),
                Optional.of(MerchantId.of("merchant_123")),
                Optional.empty(),
                Optional.empty(),
                Optional.of(Instant.parse("2026-06-01T00:00:00Z")),
                Optional.of(Instant.parse("2026-06-02T00:00:00Z")),
                25
        );

        assertThat(request.status()).contains(PaymentStatus.AUTHORIZED);
        assertThat(request.merchantId()).contains(MerchantId.of("merchant_123"));
        assertThat(request.pageSize()).isEqualTo(25);
        assertThat(request.pageToken()).isEmpty();
    }

    @Test
    void usesDefaultPageSizeWhenPageSizeIsNull() {
        var request = OpsPaymentSearchRequest.firstPage(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                null
        );

        assertThat(request.pageSize())
                .isEqualTo(OpsPaymentSearchRequest.DEFAULT_PAGE_SIZE);
    }

    @Test
    void usesDefaultPageSizeWhenPageSizeIsZeroOrNegative() {
        var request = new OpsPaymentSearchRequest(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                0,
                Optional.empty()
        );

        assertThat(request.pageSize())
                .isEqualTo(OpsPaymentSearchRequest.DEFAULT_PAGE_SIZE);
    }

    @Test
    void rejectsPageSizeAboveMaximum() {
        assertThatThrownBy(() -> new OpsPaymentSearchRequest(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                OpsPaymentSearchRequest.MAX_PAGE_SIZE + 1,
                Optional.empty()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pageSize must be <=");
    }

    @Test
    void rejectsInvalidDateRange() {
        assertThatThrownBy(() -> new OpsPaymentSearchRequest(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(Instant.parse("2026-06-02T00:00:00Z")),
                Optional.of(Instant.parse("2026-06-01T00:00:00Z")),
                50,
                Optional.empty()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("createdFrom must be before or equal to createdTo");
    }
}