package dev.kavrin.paymentrisk.payment.application.service;

import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKey;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKeyConflictException;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyScope;
import dev.kavrin.paymentrisk.idempotency.infrastructure.persistence.CompletedIdempotencyResult;
import dev.kavrin.paymentrisk.idempotency.infrastructure.persistence.DatabaseIdempotencyResultOperations;
import dev.kavrin.paymentrisk.payment.application.command.ReversePaymentCommand;
import dev.kavrin.paymentrisk.payment.application.command.ReversePaymentRequestFingerprint;
import dev.kavrin.paymentrisk.payment.application.command.ReversePaymentResult;
import dev.kavrin.paymentrisk.payment.application.outbox.PaymentOutboxEventWriter;
import dev.kavrin.paymentrisk.payment.domain.model.*;
import dev.kavrin.paymentrisk.shared.api.error.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
class DefaultPaymentReversalServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-01T10:05:00Z");

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final FakeDatabaseIdempotencyResultOperations idempotencyStore =
            new FakeDatabaseIdempotencyResultOperations();
    private final FakePaymentStatePersistencePort paymentStatePersistence =
            new FakePaymentStatePersistencePort();
    private final FakePaymentOutboxEventWriter outboxEventWriter =
            new FakePaymentOutboxEventWriter();
    private final TransactionalOperator transactionalOperator =
            mock(TransactionalOperator.class);
    private final DefaultPaymentReversalService service = new DefaultPaymentReversalService(
            clock,
            idempotencyStore,
            paymentStatePersistence,
            transactionalOperator,
            outboxEventWriter
    );

    @BeforeEach
    void resetFakes() {
        reset(transactionalOperator);
        when(transactionalOperator.transactional(any(Mono.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        idempotencyStore.reset();
        paymentStatePersistence.reset();
        outboxEventWriter.reset();
    }

    @Test
    void reverseAuthorizedPaymentAndCompletesIdempotencyRecord() {
        paymentStatePersistence.payment = authorizedPayment();

        ReversePaymentResult result = service.reverse(validCommand()).block();

        assertThat(result).isNotNull();
        assertThat(result.paymentId()).isEqualTo("pay_123");
        assertThat(result.reversalId()).startsWith("rev_");
        assertThat(result.status()).isEqualTo("REVERSED");
        assertThat(result.reason()).isEqualTo("merchant_requested");
        assertThat(result.correlationId()).isEqualTo("corr-reversal");
        assertThat(result.reversedAt()).isEqualTo(NOW);

        assertThat(idempotencyStore.findCount).isEqualTo(1);
        assertThat(idempotencyStore.insertStartedCount).isEqualTo(1);
        assertThat(idempotencyStore.markCompletedCount).isEqualTo(1);
        assertThat(idempotencyStore.markFailedAndExpireCount).isZero();
        assertThat(idempotencyStore.lastScope).isEqualTo(IdempotencyScope.PAYMENT_REVERSAL);
        assertThat(idempotencyStore.lastKey).isEqualTo(IdempotencyKey.of(validCommand().idempotencyKey()));
        assertThat(idempotencyStore.lastRequestFingerprint)
                .isEqualTo(ReversePaymentRequestFingerprint.from(validCommand()));
        assertThat(idempotencyStore.lastExpiresAt).isEqualTo(NOW.plus(Duration.ofHours(24)));
        assertThat(idempotencyStore.lastCompletedResponse).isEqualTo(result);
        assertThat(idempotencyStore.lastResponseStatus).isEqualTo(200);

        assertThat(paymentStatePersistence.findCount).isEqualTo(1);
        assertThat(paymentStatePersistence.lastFindPaymentId).isEqualTo(PaymentId.of("pay_123"));
        assertThat(paymentStatePersistence.saveReversalCount).isEqualTo(1);
        assertThat(paymentStatePersistence.lastReversalIdempotencyKey)
                .isEqualTo(IdempotencyKey.of(validCommand().idempotencyKey()));
        assertThat(paymentStatePersistence.lastSavedPayment.getStatus()).isEqualTo(PaymentStatus.REVERSED);
        assertThat(paymentStatePersistence.lastSavedPayment.reversal()).isPresent();
        assertThat(outboxEventWriter.writeReversedCount).isEqualTo(1);
        assertThat(outboxEventWriter.lastReversedPayment).isEqualTo(paymentStatePersistence.lastSavedPayment);
        assertThat(outboxEventWriter.lastCorrelationId).isEqualTo("corr-reversal");
        verify(transactionalOperator).transactional(any(Mono.class));
    }

    @Test
    void reverseUsesDefaultReasonForBlankReasonInCommand() {
        paymentStatePersistence.payment = authorizedPayment();

        ReversePaymentResult result = service.reverse(new ReversePaymentCommand(
                "pay_123",
                "idem_reversal_123",
                " ",
                "corr-reversal"
        )).block();

        assertThat(result).isNotNull();
        assertThat(result.reason()).isEqualTo("merchant_requested");
    }

    @Test
    void reverseReturnsStoredResultForDuplicateSameFingerprintRequest() {
        ReversePaymentResult storedResult = storedResult();
        idempotencyStore.storedResult = storedResult;

        ReversePaymentResult result = service.reverse(validCommand()).block();

        assertThat(result).isEqualTo(storedResult);
        assertThat(idempotencyStore.findCount).isEqualTo(1);
        assertThat(idempotencyStore.insertStartedCount).isZero();
        assertThat(idempotencyStore.markCompletedCount).isZero();
        assertThat(paymentStatePersistence.findCount).isZero();
        assertThat(paymentStatePersistence.saveReversalCount).isZero();
        assertThat(outboxEventWriter.writeReversedCount).isZero();
    }

    @Test
    void reverseRejectsDuplicateIdempotencyKeyWithDifferentFingerprint() {
        idempotencyStore.findError = new IdempotencyKeyConflictException();

        assertThatThrownBy(() -> service.reverse(validCommand()).block())
                .isInstanceOf(IdempotencyKeyConflictException.class)
                .hasMessage("Idempotency key was already used for a different request");

        assertThat(idempotencyStore.findCount).isEqualTo(1);
        assertThat(idempotencyStore.insertStartedCount).isZero();
        assertThat(paymentStatePersistence.findCount).isZero();
        assertThat(outboxEventWriter.writeReversedCount).isZero();
    }

    @Test
    void reverseExpiresStartedRecordWhenPaymentIsNotReversible() {
        paymentStatePersistence.payment = declinedPayment();

        assertThatThrownBy(() -> service.reverse(validCommand()).block())
                .isInstanceOf(PaymentStateTransitionException.class)
                .hasMessage("Payment with status DECLINED cannot be reversed");

        assertThat(idempotencyStore.insertStartedCount).isEqualTo(1);
        assertThat(paymentStatePersistence.findCount).isEqualTo(1);
        assertThat(paymentStatePersistence.saveReversalCount).isZero();
        assertThat(idempotencyStore.markCompletedCount).isZero();
        assertThat(idempotencyStore.markFailedAndExpireCount).isEqualTo(1);
        assertThat(outboxEventWriter.writeReversedCount).isZero();
    }

    @Test
    void reverseExpiresStartedRecordWhenPaymentIsMissing() {
        paymentStatePersistence.findError =
                new ResourceNotFoundException("Payment not found: pay_123");

        assertThatThrownBy(() -> service.reverse(validCommand()).block())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Payment not found: pay_123");

        assertThat(idempotencyStore.insertStartedCount).isEqualTo(1);
        assertThat(paymentStatePersistence.findCount).isEqualTo(1);
        assertThat(paymentStatePersistence.saveReversalCount).isZero();
        assertThat(idempotencyStore.markCompletedCount).isZero();
        assertThat(idempotencyStore.markFailedAndExpireCount).isEqualTo(1);
        assertThat(outboxEventWriter.writeReversedCount).isZero();
    }

    @Test
    void reverseExpiresStartedRecordWhenPersistenceFails() {
        paymentStatePersistence.payment = authorizedPayment();
        paymentStatePersistence.saveReversalError =
                new IllegalStateException("reversal persistence failed");

        assertThatThrownBy(() -> service.reverse(validCommand()).block())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("reversal persistence failed");

        assertThat(idempotencyStore.insertStartedCount).isEqualTo(1);
        assertThat(paymentStatePersistence.findCount).isEqualTo(1);
        assertThat(paymentStatePersistence.saveReversalCount).isEqualTo(1);
        assertThat(idempotencyStore.markCompletedCount).isZero();
        assertThat(idempotencyStore.markFailedAndExpireCount).isEqualTo(1);
        assertThat(outboxEventWriter.writeReversedCount).isZero();
    }

    @Test
    void reverseExpiresStartedRecordWhenOutboxWriteFails() {
        paymentStatePersistence.payment = authorizedPayment();
        outboxEventWriter.writeReversedError =
                new IllegalStateException("outbox insert failed");

        assertThatThrownBy(() -> service.reverse(validCommand()).block())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("outbox insert failed");

        assertThat(idempotencyStore.insertStartedCount).isEqualTo(1);
        assertThat(paymentStatePersistence.findCount).isEqualTo(1);
        assertThat(paymentStatePersistence.saveReversalCount).isEqualTo(1);
        assertThat(outboxEventWriter.writeReversedCount).isEqualTo(1);
        assertThat(idempotencyStore.markCompletedCount).isZero();
        assertThat(idempotencyStore.markFailedAndExpireCount).isEqualTo(1);
    }

    private static ReversePaymentCommand validCommand() {
        return new ReversePaymentCommand(
                "pay_123",
                "idem_reversal_123",
                "merchant_requested",
                "corr-reversal"
        );
    }

    private static ReversePaymentResult storedResult() {
        return new ReversePaymentResult(
                "pay_123",
                "rev_stored",
                "REVERSED",
                "merchant_requested",
                "corr-reversal",
                NOW.minusSeconds(60)
        );
    }

    private static Payment authorizedPayment() {
        Payment payment = requestedPayment();
        payment.markRiskPending(NOW.minusSeconds(10));
        payment.markAuthorized(
                approvedRiskDecision(),
                AuthorizationCode.of("AUTH-ABCDEFG123"),
                NOW.minusSeconds(5)
        );
        return payment;
    }

    private static Payment declinedPayment() {
        Payment payment = requestedPayment();
        payment.markRiskPending(NOW.minusSeconds(10));
        payment.markDeclined(
                new PaymentRiskDecision(
                        RiskDecision.DECLINED,
                        91,
                        List.of("HIGH_RISK"),
                        "risk-rules-v1",
                        NOW.minusSeconds(8)
                ),
                NOW.minusSeconds(5)
        );
        return payment;
    }

    private static Payment requestedPayment() {
        return Payment.newAuthorizationAttempt(
                PaymentId.of("pay_123"),
                MerchantId.of("merchant_123"),
                CustomerId.of("customer_123"),
                Money.of(10_000, "USD"),
                PaymentMethodToken.of("pm_token_1234567890"),
                DeviceFingerprint.of("device_123"),
                ExternalReference.ofNullable("order_123"),
                IdempotencyKey.of("idem_authorization_123"),
                NOW.minusSeconds(20)
        );
    }

    private static PaymentRiskDecision approvedRiskDecision() {
        return new PaymentRiskDecision(
                RiskDecision.APPROVED,
                10,
                List.of("LOW_RISK"),
                "risk-rules-v1",
                NOW.minusSeconds(8)
        );
    }

    private static final class FakeDatabaseIdempotencyResultOperations
            implements DatabaseIdempotencyResultOperations {

        private ReversePaymentResult storedResult;
        private RuntimeException findError;
        private int findCount;
        private int insertStartedCount;
        private int markCompletedCount;
        private int markFailedAndExpireCount;
        private IdempotencyScope lastScope;
        private IdempotencyKey lastKey;
        private String lastRequestFingerprint;
        private Instant lastNow;
        private Instant lastExpiresAt;
        private Object lastCompletedResponse;
        private int lastResponseStatus;

        void reset() {
            storedResult = null;
            findError = null;
            findCount = 0;
            insertStartedCount = 0;
            markCompletedCount = 0;
            markFailedAndExpireCount = 0;
            lastScope = null;
            lastKey = null;
            lastRequestFingerprint = null;
            lastNow = null;
            lastExpiresAt = null;
            lastCompletedResponse = null;
            lastResponseStatus = 0;
        }

        @Override
        public <T> Mono<CompletedIdempotencyResult<T>> findCompletedResultWithMetadata(
                IdempotencyScope scope,
                IdempotencyKey key,
                String requestFingerprint,
                Instant now,
                Class<T> responseType
        ) {
            findCount++;
            lastScope = scope;
            lastKey = key;
            lastRequestFingerprint = requestFingerprint;
            lastNow = now;

            if (findError != null) {
                return Mono.error(findError);
            }

            if (storedResult == null) {
                return Mono.empty();
            }

            return Mono.just(new CompletedIdempotencyResult<>(
                    responseType.cast(storedResult),
                    NOW.plus(Duration.ofHours(24))
            ));
        }

        @Override
        public Mono<Void> insertStarted(
                IdempotencyScope scope,
                IdempotencyKey key,
                String requestFingerprint,
                Instant now,
                Instant expiresAt
        ) {
            insertStartedCount++;
            lastScope = scope;
            lastKey = key;
            lastRequestFingerprint = requestFingerprint;
            lastNow = now;
            lastExpiresAt = expiresAt;
            return Mono.empty();
        }

        @Override
        public <T> Mono<Void> markCompleted(
                IdempotencyScope scope,
                IdempotencyKey key,
                String requestFingerprint,
                T response,
                int responseStatus,
                Instant now
        ) {
            markCompletedCount++;
            lastScope = scope;
            lastKey = key;
            lastRequestFingerprint = requestFingerprint;
            lastNow = now;
            lastCompletedResponse = response;
            lastResponseStatus = responseStatus;
            return Mono.empty();
        }

        @Override
        public Mono<Void> markFailedAndExpire(
                IdempotencyScope scope,
                IdempotencyKey key,
                String requestFingerprint,
                Instant now
        ) {
            markFailedAndExpireCount++;
            lastScope = scope;
            lastKey = key;
            lastRequestFingerprint = requestFingerprint;
            lastNow = now;
            return Mono.empty();
        }
    }

    private static final class FakePaymentStatePersistencePort
            implements PaymentStatePersistencePort {

        private Payment payment;
        private RuntimeException findError;
        private RuntimeException saveReversalError;
        private int findCount;
        private int saveReversalCount;
        private PaymentId lastFindPaymentId;
        private Payment lastSavedPayment;
        private IdempotencyKey lastReversalIdempotencyKey;

        void reset() {
            payment = null;
            findError = null;
            saveReversalError = null;
            findCount = 0;
            saveReversalCount = 0;
            lastFindPaymentId = null;
            lastSavedPayment = null;
            lastReversalIdempotencyKey = null;
        }

        @Override
        public Mono<Payment> save(Payment payment) {
            return Mono.error(new UnsupportedOperationException("save is not used by reversal"));
        }

        @Override
        public Mono<Payment> findByPaymentId(PaymentId paymentId) {
            findCount++;
            lastFindPaymentId = paymentId;

            if (findError != null) {
                return Mono.error(findError);
            }

            return Mono.just(payment);
        }

        @Override
        public Mono<Payment> saveReversal(
                Payment payment,
                IdempotencyKey reversalIdempotencyKey
        ) {
            saveReversalCount++;
            lastSavedPayment = payment;
            lastReversalIdempotencyKey = reversalIdempotencyKey;

            if (saveReversalError != null) {
                return Mono.error(saveReversalError);
            }

            return Mono.just(payment);
        }
    }

    private static final class FakePaymentOutboxEventWriter implements PaymentOutboxEventWriter {

        private RuntimeException writeReversedError;
        private int writeReversedCount;
        private Payment lastReversedPayment;
        private String lastCorrelationId;

        void reset() {
            writeReversedError = null;
            writeReversedCount = 0;
            lastReversedPayment = null;
            lastCorrelationId = null;
        }

        @Override
        public Mono<Void> writeAuthorizationEvents(
                Payment payment,
                String correlationId
        ) {
            return Mono.error(new UnsupportedOperationException("authorization outbox is not used by reversal"));
        }

        @Override
        public Mono<Void> writePaymentReversedEvents(
                Payment payment,
                String correlationId
        ) {
            writeReversedCount++;
            lastReversedPayment = payment;
            lastCorrelationId = correlationId;

            if (writeReversedError != null) {
                return Mono.error(writeReversedError);
            }

            return Mono.empty();
        }
    }
}
