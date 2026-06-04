package dev.kavrin.paymentrisk.ops.application;

import dev.kavrin.paymentrisk.payment.domain.model.CustomerId;
import dev.kavrin.paymentrisk.payment.domain.model.MerchantId;
import dev.kavrin.paymentrisk.payment.domain.model.PaymentId;
import dev.kavrin.paymentrisk.payment.domain.model.PaymentStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record OpsPaymentSearchRequest(
        Optional<PaymentStatus> status,
        Optional<MerchantId> merchantId,
        Optional<CustomerId> customerId,
        Optional<PaymentId> paymentId,
        Optional<Instant> createdFrom,
        Optional<Instant> createdTo,
        int pageSize,
        Optional<String> pageToken
) {
    public static final int DEFAULT_PAGE_SIZE = 50;
    public static final int MAX_PAGE_SIZE = 100;

    public OpsPaymentSearchRequest {
        status = normalize(status);
        merchantId = normalize(merchantId);
        customerId = normalize(customerId);
        paymentId = normalize(paymentId);
        createdFrom = normalize(createdFrom);
        createdTo = normalize(createdTo);
        pageToken = normalize(pageToken);

        if (pageSize <= 0) {
            pageSize = DEFAULT_PAGE_SIZE;
        }

        if (pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("pageSize must be <= " + MAX_PAGE_SIZE);
        }

        if (createdFrom.isPresent()
                && createdTo.isPresent()
                && createdFrom.get().isAfter(createdTo.get())) {
            throw new IllegalArgumentException("createdFrom must be before or equal to createdTo");
        }
    }

    public static OpsPaymentSearchRequest firstPage(
            Optional<PaymentStatus> status,
            Optional<MerchantId> merchantId,
            Optional<CustomerId> customerId,
            Optional<PaymentId> paymentId,
            Optional<Instant> createdFrom,
            Optional<Instant> createdTo,
            Integer pageSize
    ) {
        return new OpsPaymentSearchRequest(
                status,
                merchantId,
                customerId,
                paymentId,
                createdFrom,
                createdTo,
                pageSize == null ? DEFAULT_PAGE_SIZE : pageSize,
                Optional.empty()
        );
    }

    private static <T> Optional<T> normalize(Optional<T> value) {
        return Objects.requireNonNullElse(value, Optional.empty());
    }
}
