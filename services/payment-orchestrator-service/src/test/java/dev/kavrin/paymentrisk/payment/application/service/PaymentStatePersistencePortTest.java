package dev.kavrin.paymentrisk.payment.application.service;

import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKey;
import dev.kavrin.paymentrisk.payment.domain.model.*;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentStatePersistencePortTest {

    @Test
    void fakePersistenceShouldSaveAndReturnPaymentAggregate() {
        FakePaymentStatePersistencePort persistence =
                new FakePaymentStatePersistencePort();

        Payment payment = Payment.newAuthorizationAttempt(
                PaymentId.of("pay_01HTEST0000000000000000000"),
                MerchantId.of("merchant_123"),
                CustomerId.of("customer_123"),
                Money.of(12_500, "USD"),
                PaymentMethodToken.of("tok_test_4242"),
                DeviceFingerprint.of("device_abc"),
                ExternalReference.ofNullable("order_123"),
                IdempotencyKey.of("idem_01HTEST0000000000000000000"),
                Instant.parse("2026-05-26T10:15:30Z")
        );

        StepVerifier.create(persistence.save(payment))
                .expectNext(payment)
                .verifyComplete();

        assertThat(persistence.savedPayments()).containsExactly(payment);
    }

    private static final class FakePaymentStatePersistencePort
            implements PaymentStatePersistencePort {

        private final List<Payment> savedPayments = new ArrayList<>();

        @Override
        public Mono<Payment> save(Payment payment) {
            Objects.requireNonNull(payment, "payment must not be null");
            savedPayments.add(payment);
            return Mono.just(payment);
        }

        @Override
        public Mono<Payment> findByPaymentId(PaymentId paymentId) {
            return savedPayments.stream()
                    .filter(payment -> payment.getId().equals(paymentId))
                    .findFirst()
                    .map(Mono::just)
                    .orElseGet(Mono::empty);
        }

        @Override
        public Mono<Payment> saveReversal(
                Payment payment,
                IdempotencyKey reversalIdempotencyKey
        ) {
            Objects.requireNonNull(reversalIdempotencyKey, "reversalIdempotencyKey must not be null");
            return save(payment);
        }

        List<Payment> savedPayments() {
            return List.copyOf(savedPayments);
        }
    }
}
