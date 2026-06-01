package dev.kavrin.paymentrisk.payment.application.service;

import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKey;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKeyConflictException;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyScope;
import dev.kavrin.paymentrisk.idempotency.infrastructure.persistence.DatabaseIdempotencyResultOperations;
import dev.kavrin.paymentrisk.idempotency.infrastructure.redis.CachedIdempotencySnapshot;
import dev.kavrin.paymentrisk.idempotency.infrastructure.redis.RedisIdempotencySnapshotCache;
import dev.kavrin.paymentrisk.payment.application.command.AuthorizePaymentCommand;
import dev.kavrin.paymentrisk.payment.application.command.AuthorizePaymentResult;
import dev.kavrin.paymentrisk.payment.application.outbox.PaymentOutboxEventWriter;
import dev.kavrin.paymentrisk.payment.domain.model.Payment;
import dev.kavrin.paymentrisk.risk.application.RiskScoringClient;
import dev.kavrin.paymentrisk.risk.application.dto.RiskScoringRequest;
import dev.kavrin.paymentrisk.risk.application.dto.RiskScoringResponse;
import dev.kavrin.paymentrisk.shared.api.error.DownstreamTimeoutException;
import dev.kavrin.paymentrisk.shared.api.error.DownstreamUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class DefaultAuthorizePaymentServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-25T10:15:30Z");

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final FakeDatabaseIdempotencyResultOperations idempotencyStore =
            new FakeDatabaseIdempotencyResultOperations();
    private final FakePaymentStatePersistencePort paymentStatePersistence =
            new FakePaymentStatePersistencePort();
    private final FakeRiskScoringClient riskScoringClient =
            new FakeRiskScoringClient();
    private final FakePaymentOutboxEventWriter paymentOutboxEventWriter =
            new FakePaymentOutboxEventWriter();
    private final TransactionalOperator transactionalOperator =
            mock(TransactionalOperator.class);
    private final FakeRedisIdempotencySnapshotCache idempotencySnapshotCache =
            new FakeRedisIdempotencySnapshotCache();
    private final AuthorizePaymentResultSnapshotSerializer snapshotSerializer =
            new AuthorizePaymentResultSnapshotSerializer();
    private final DefaultAuthorizePaymentService service = new DefaultAuthorizePaymentService(
            clock,
            idempotencyStore,
            paymentStatePersistence,
            riskScoringClient,
            new RiskDecisionMappingPolicy(clock),
            paymentOutboxEventWriter,
            transactionalOperator,
            Optional.of(idempotencySnapshotCache),
            snapshotSerializer
    );

    @BeforeEach
    void resetFakes() {
        reset(transactionalOperator);
        when(transactionalOperator.transactional(any(Mono.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        idempotencyStore.reset();
        paymentStatePersistence.reset();
        riskScoringClient.reset();
        paymentOutboxEventWriter.reset();
        idempotencySnapshotCache.reset();
    }

    @Test
    void authorizeApprovesPaymentFromRiskResultAndStoresCompletedIdempotencyResult() {
        riskScoringClient.response = RiskScoringResponse.approved(
                12,
                List.of("LOW_RISK_PAYMENT"),
                "risk-rules-v1"
        );

        AuthorizePaymentResult result = service.authorize(validCommand()).block();

        assertThat(result).isNotNull();
        assertThat(result.paymentId()).startsWith("pay_");
        assertThat(result.status()).isEqualTo("AUTHORIZED");
        assertThat(result.authorizationCode()).startsWith("AUTH-");
        assertThat(result.authorizationCode()).hasSize(17);
        assertThat(result.riskDecision()).isEqualTo("APPROVED");
        assertThat(result.reasonCodes()).containsExactly("LOW_RISK_PAYMENT");
        assertThat(result.correlationId()).isEqualTo("corr-authorization-service");
        assertThat(result.riskScore()).isEqualTo(12);
        assertThat(result.ruleVersion()).isEqualTo("risk-rules-v1");
        assertThat(result.createdAt()).isEqualTo(NOW);

        assertThat(idempotencyStore.findCount).isEqualTo(1);
        assertThat(idempotencyStore.insertStartedCount).isEqualTo(1);
        assertThat(idempotencyStore.markCompletedCount).isEqualTo(1);
        assertThat(idempotencyStore.markFailedAndExpireCount).isZero();
        assertThat(idempotencyStore.lastScope).isEqualTo(IdempotencyScope.PAYMENT_AUTHORIZATION);
        assertThat(idempotencyStore.lastKey).isEqualTo(IdempotencyKey.of(validCommand().idempotencyKey()));
        assertThat(idempotencyStore.lastNow).isEqualTo(NOW);
        assertThat(idempotencyStore.lastExpiresAt).isEqualTo(NOW.plusSeconds(86400));
        assertThat(idempotencyStore.lastCompletedResponse).isEqualTo(result);
        assertThat(idempotencyStore.lastResponseStatus).isEqualTo(200);

        assertThat(riskScoringClient.scoreCount).isEqualTo(1);
        assertThat(riskScoringClient.lastRequest).isNotNull();
        assertThat(riskScoringClient.lastRequest.paymentId()).isEqualTo(result.paymentId());
        assertThat(riskScoringClient.lastRequest.amountMinor()).isEqualTo(validCommand().amountMinor());
        assertThat(riskScoringClient.lastRequest.currency()).isEqualTo(validCommand().currency());
        assertThat(riskScoringClient.lastRequest.merchantId()).isEqualTo(validCommand().merchantId());
        assertThat(riskScoringClient.lastRequest.customerId()).isEqualTo(validCommand().customerId());
        assertThat(riskScoringClient.lastRequest.deviceFingerprint()).isEqualTo(validCommand().deviceFingerprint());
        assertThat(riskScoringClient.lastRequest.correlationId()).isEqualTo(validCommand().correlationId());

        assertThat(paymentStatePersistence.saveCount).isEqualTo(1);
        assertThat(paymentStatePersistence.lastPayment).isNotNull();
        assertThat(paymentStatePersistence.lastPayment.getId().value()).isEqualTo(result.paymentId());
        assertThat(paymentStatePersistence.lastPayment.getStatus().name()).isEqualTo(result.status());
        assertThat(paymentStatePersistence.lastPayment.getRiskDecision().decision().name())
                .isEqualTo(result.riskDecision());
        assertThat(paymentOutboxEventWriter.writeCount).isEqualTo(1);
        assertThat(paymentOutboxEventWriter.lastPayment).isSameAs(paymentStatePersistence.lastPayment);
        assertThat(paymentOutboxEventWriter.lastCorrelationId).isEqualTo(validCommand().correlationId());
        assertThat(idempotencySnapshotCache.putCount).isEqualTo(1);
        assertThat(idempotencySnapshotCache.lastPutScope).isEqualTo(IdempotencyScope.PAYMENT_AUTHORIZATION);
        assertThat(idempotencySnapshotCache.lastPutKey).isEqualTo(IdempotencyKey.of(validCommand().idempotencyKey()));
        assertThat(idempotencySnapshotCache.lastPutFingerprint).isEqualTo(idempotencyStore.lastRequestFingerprint);
        assertThat(idempotencySnapshotCache.lastPutTtl).isEqualTo(Duration.ofHours(24));
        verify(transactionalOperator).transactional(any(Mono.class));
    }

    @Test
    void authorizeDeclinesPaymentFromRiskResultAndCompletesIdempotencyResult() {
        riskScoringClient.response = RiskScoringResponse.declined(
                95,
                List.of("HIGH_AMOUNT"),
                "risk-rules-v1"
        );

        AuthorizePaymentResult result = service.authorize(validCommand()).block();

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo("DECLINED");
        assertThat(result.authorizationCode()).isNull();
        assertThat(result.riskDecision()).isEqualTo("DECLINED");
        assertThat(result.reasonCodes()).containsExactly("HIGH_AMOUNT");
        assertThat(result.riskScore()).isEqualTo(95);
        assertThat(result.ruleVersion()).isEqualTo("risk-rules-v1");

        assertThat(riskScoringClient.scoreCount).isEqualTo(1);
        assertThat(paymentStatePersistence.saveCount).isEqualTo(1);
        assertThat(paymentStatePersistence.lastPayment.getStatus().name()).isEqualTo("DECLINED");
        assertThat(paymentOutboxEventWriter.writeCount).isEqualTo(1);
        assertThat(paymentOutboxEventWriter.lastPayment.getStatus().name()).isEqualTo("DECLINED");
        assertThat(idempotencySnapshotCache.putCount).isEqualTo(1);
        assertThat(idempotencyStore.markCompletedCount).isEqualTo(1);
        assertThat(idempotencyStore.markFailedAndExpireCount).isZero();
        verify(transactionalOperator).transactional(any(Mono.class));
    }

    @Test
    void authorizeRejectsDomainInvalidIdempotencyKeyBeforeDatabaseAccess() {
        AuthorizePaymentCommand command = new AuthorizePaymentCommand(
                "mer_01HX7Q9K2V6M8P4A3B9C1D2E3F",
                "cus_01HX7QAF4CQ8YFZ3M9N2W1P0VK",
                1299,
                "USD",
                "pmt_tok_4f7b8d9c2a1e",
                "dfp_6d9f1a2b3c4e5f678901",
                "order_2026_000123",
                "invalid idempotency key",
                "corr-authorization-service"
        );

        assertThatThrownBy(() -> service.authorize(command).block())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Idempotency key may contain letters, numbers, dot, underscore, colon, and hyphen");
        assertThat(idempotencyStore.findCount).isZero();
        assertThat(idempotencyStore.insertStartedCount).isZero();
        assertThat(riskScoringClient.scoreCount).isZero();
        assertThat(paymentStatePersistence.saveCount).isZero();
        assertThat(paymentOutboxEventWriter.writeCount).isZero();
    }

    @Test
    void authorizeReturnsStoredDatabaseResultForDuplicateIdempotencyKeyAndSameRequest() {
        AuthorizePaymentResult storedResult = storedResult();
        idempotencyStore.storedResult = storedResult;

        AuthorizePaymentResult result = service.authorize(validCommand()).block();

        assertThat(result).isEqualTo(storedResult);
        assertThat(idempotencyStore.findCount).isEqualTo(1);
        assertThat(idempotencyStore.insertStartedCount).isZero();
        assertThat(idempotencyStore.markCompletedCount).isZero();
        assertThat(riskScoringClient.scoreCount).isZero();
        assertThat(paymentStatePersistence.saveCount).isZero();
        assertThat(paymentOutboxEventWriter.writeCount).isZero();
    }

    @Test
    void authorizeReturnsStoredRedisResultForDuplicateIdempotencyKeyAndSameRequest() {
        AuthorizePaymentResult storedResult = storedResult();
        idempotencySnapshotCache.cachedSnapshot = new CachedIdempotencySnapshot(
                requestFingerprint(validCommand()),
                snapshotSerializer.serialize(storedResult)
        );
        assertThat(snapshotSerializer.deserialize(
                idempotencySnapshotCache.cachedSnapshot.responseBodyJson(),
                AuthorizePaymentResult.class
        )).isEqualTo(storedResult);

        AuthorizePaymentResult result = service.authorize(validCommand()).block();

        assertThat(result).isEqualTo(storedResult);
        assertThat(idempotencySnapshotCache.getCount).isEqualTo(1);
        assertThat(idempotencyStore.findCount).isZero();
        assertThat(idempotencyStore.insertStartedCount).isZero();
        assertThat(riskScoringClient.scoreCount).isZero();
        assertThat(paymentStatePersistence.saveCount).isZero();
        assertThat(paymentOutboxEventWriter.writeCount).isZero();
    }

    @Test
    void authorizeFallsBackToDatabaseWhenRedisFingerprintDiffers() {
        AuthorizePaymentResult storedResult = storedResult();
        idempotencyStore.storedResult = storedResult;
        idempotencySnapshotCache.cachedSnapshot = new CachedIdempotencySnapshot(
                "different-fingerprint",
                snapshotSerializer.serialize(storedResult)
        );

        AuthorizePaymentResult result = service.authorize(validCommand()).block();

        assertThat(result).isEqualTo(storedResult);
        assertThat(idempotencySnapshotCache.getCount).isEqualTo(1);
        assertThat(idempotencyStore.findCount).isEqualTo(1);
        assertThat(idempotencyStore.insertStartedCount).isZero();
    }

    @Test
    void authorizeRejectsDuplicateIdempotencyKeyWithDifferentRequestFingerprint() {
        idempotencyStore.findError = new IdempotencyKeyConflictException();

        assertThatThrownBy(() -> service.authorize(validCommandWithAmount(1599)).block())
                .isInstanceOf(IdempotencyKeyConflictException.class)
                .hasMessage("Idempotency key was already used for a different request");
        assertThat(idempotencyStore.findCount).isEqualTo(1);
        assertThat(idempotencyStore.insertStartedCount).isZero();
        assertThat(riskScoringClient.scoreCount).isZero();
        assertThat(paymentStatePersistence.saveCount).isZero();
        assertThat(paymentOutboxEventWriter.writeCount).isZero();
    }

    @Test
    void authorizeExpiresStartedRecordWhenCompletionFails() {
        idempotencyStore.markCompletedError = new IllegalStateException("completion failed");

        assertThatThrownBy(() -> service.authorize(validCommand()).block())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("completion failed");

        assertThat(idempotencyStore.insertStartedCount).isEqualTo(1);
        assertThat(riskScoringClient.scoreCount).isEqualTo(1);
        assertThat(paymentStatePersistence.saveCount).isEqualTo(1);
        assertThat(paymentOutboxEventWriter.writeCount).isEqualTo(1);
        assertThat(idempotencyStore.markCompletedCount).isEqualTo(1);
        assertThat(idempotencyStore.markFailedAndExpireCount).isEqualTo(1);
    }

    @Test
    void authorizeExpiresStartedRecordWhenPaymentPersistenceFails() {
        paymentStatePersistence.saveError = new IllegalStateException("payment persistence failed");

        assertThatThrownBy(() -> service.authorize(validCommand()).block())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("payment persistence failed");

        assertThat(idempotencyStore.insertStartedCount).isEqualTo(1);
        assertThat(riskScoringClient.scoreCount).isEqualTo(1);
        assertThat(paymentStatePersistence.saveCount).isEqualTo(1);
        assertThat(paymentOutboxEventWriter.writeCount).isZero();
        assertThat(idempotencyStore.markCompletedCount).isZero();
        assertThat(idempotencyStore.markFailedAndExpireCount).isEqualTo(1);
    }

    @Test
    void authorizeExpiresStartedRecordWhenOutboxWriteFails() {
        paymentOutboxEventWriter.writeError = new IllegalStateException("outbox write failed");

        assertThatThrownBy(() -> service.authorize(validCommand()).block())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("outbox write failed");

        assertThat(idempotencyStore.insertStartedCount).isEqualTo(1);
        assertThat(riskScoringClient.scoreCount).isEqualTo(1);
        assertThat(paymentStatePersistence.saveCount).isEqualTo(1);
        assertThat(paymentOutboxEventWriter.writeCount).isEqualTo(1);
        assertThat(idempotencyStore.markCompletedCount).isZero();
        assertThat(idempotencyStore.markFailedAndExpireCount).isEqualTo(1);
        verify(transactionalOperator).transactional(any(Mono.class));
    }

    @Test
    void authorizeReturnsStableTimeoutErrorWhenRiskServiceTimesOut() {
        riskScoringClient.response = RiskScoringResponse.timeout();

        assertThatThrownBy(() -> service.authorize(validCommand()).block())
                .isInstanceOf(DownstreamTimeoutException.class)
                .hasMessage("Risk service timed out");

        assertThat(idempotencyStore.insertStartedCount).isEqualTo(1);
        assertThat(riskScoringClient.scoreCount).isEqualTo(1);
        assertThat(paymentStatePersistence.saveCount).isZero();
        assertThat(paymentOutboxEventWriter.writeCount).isZero();
        assertThat(idempotencyStore.markCompletedCount).isZero();
        assertThat(idempotencyStore.markFailedAndExpireCount).isEqualTo(1);
    }

    @Test
    void authorizeReturnsStableUnavailableErrorWhenRiskServiceIsUnavailable() {
        riskScoringClient.response = RiskScoringResponse.unavailable();

        assertThatThrownBy(() -> service.authorize(validCommand()).block())
                .isInstanceOf(DownstreamUnavailableException.class)
                .hasMessage("Risk service is unavailable");

        assertThat(idempotencyStore.insertStartedCount).isEqualTo(1);
        assertThat(riskScoringClient.scoreCount).isEqualTo(1);
        assertThat(paymentStatePersistence.saveCount).isZero();
        assertThat(paymentOutboxEventWriter.writeCount).isZero();
        assertThat(idempotencyStore.markCompletedCount).isZero();
        assertThat(idempotencyStore.markFailedAndExpireCount).isEqualTo(1);
    }

    private static AuthorizePaymentCommand validCommand() {
        return new AuthorizePaymentCommand(
                "mer_01HX7Q9K2V6M8P4A3B9C1D2E3F",
                "cus_01HX7QAF4CQ8YFZ3M9N2W1P0VK",
                1299,
                "USD",
                "pmt_tok_4f7b8d9c2a1e",
                "dfp_6d9f1a2b3c4e5f678901",
                "order_2026_000123",
                "idem_01HX7QK9JP7E5W5NRZ6T5Q3R1A",
                "corr-authorization-service"
        );
    }

    private static AuthorizePaymentCommand validCommandWithAmount(long amountMinor) {
        return new AuthorizePaymentCommand(
                "mer_01HX7Q9K2V6M8P4A3B9C1D2E3F",
                "cus_01HX7QAF4CQ8YFZ3M9N2W1P0VK",
                amountMinor,
                "USD",
                "pmt_tok_4f7b8d9c2a1e",
                "dfp_6d9f1a2b3c4e5f678901",
                "order_2026_000123",
                "idem_01HX7QK9JP7E5W5NRZ6T5Q3R1A",
                "corr-authorization-service"
        );
    }

    private static String requestFingerprint(AuthorizePaymentCommand command) {
        try {
            var method = DefaultAuthorizePaymentService.class.getDeclaredMethod(
                    "requestFingerprint",
                    AuthorizePaymentCommand.class
            );
            method.setAccessible(true);
            return (String) method.invoke(null, command);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to invoke request fingerprint helper", exception);
        }
    }

    private static AuthorizePaymentResult storedResult() {
        return new AuthorizePaymentResult(
                "pay_stored",
                "AUTHORIZED",
                "AUTH-STORED1234",
                "APPROVED",
                List.of("LOW_RISK"),
                "corr-authorization-service",
                11,
                "risk-rules-v1",
                NOW.minusSeconds(60)
        );
    }

    private static final class FakeDatabaseIdempotencyResultOperations
            implements DatabaseIdempotencyResultOperations {

        private AuthorizePaymentResult storedResult;
        private RuntimeException findError;
        private RuntimeException markCompletedError;
        private int findCount;
        private int insertStartedCount;
        private int markCompletedCount;
        private int markFailedAndExpireCount;
        private IdempotencyScope lastScope;
        private IdempotencyKey lastKey;
        private Instant lastNow;
        private Instant lastExpiresAt;
        private String lastRequestFingerprint;
        private Object lastCompletedResponse;
        private int lastResponseStatus;

        void reset() {
            storedResult = null;
            findError = null;
            markCompletedError = null;
            findCount = 0;
            insertStartedCount = 0;
            markCompletedCount = 0;
            markFailedAndExpireCount = 0;
            lastScope = null;
            lastKey = null;
            lastNow = null;
            lastExpiresAt = null;
            lastRequestFingerprint = null;
            lastCompletedResponse = null;
            lastResponseStatus = 0;
        }

        @Override
        public <T> Mono<T> findCompletedResult(
                IdempotencyScope scope,
                IdempotencyKey key,
                String requestFingerprint,
                Instant now,
                Class<T> responseType
        ) {
            findCount++;
            lastScope = scope;
            lastKey = key;
            lastNow = now;
            lastRequestFingerprint = requestFingerprint;

            if (findError != null) {
                return Mono.error(findError);
            }

            if (storedResult == null) {
                return Mono.empty();
            }

            return Mono.just(responseType.cast(storedResult));
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
            lastNow = now;
            lastExpiresAt = expiresAt;
            lastRequestFingerprint = requestFingerprint;
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
            lastNow = now;
            lastRequestFingerprint = requestFingerprint;
            lastCompletedResponse = response;
            lastResponseStatus = responseStatus;

            if (markCompletedError != null) {
                return Mono.error(markCompletedError);
            }

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
            lastNow = now;
            lastRequestFingerprint = requestFingerprint;
            return Mono.empty();
        }
    }

    private static final class FakeRedisIdempotencySnapshotCache
            implements RedisIdempotencySnapshotCache {

        private CachedIdempotencySnapshot cachedSnapshot;
        private int getCount;
        private int putCount;
        private IdempotencyScope lastPutScope;
        private IdempotencyKey lastPutKey;
        private String lastPutFingerprint;
        private String lastPutResponseBodyJson;
        private Duration lastPutTtl;

        void reset() {
            cachedSnapshot = null;
            getCount = 0;
            putCount = 0;
            lastPutScope = null;
            lastPutKey = null;
            lastPutFingerprint = null;
            lastPutResponseBodyJson = null;
            lastPutTtl = null;
        }

        @Override
        public Mono<CachedIdempotencySnapshot> getCompletedSnapshot(
                IdempotencyScope scope,
                IdempotencyKey idempotencyKey
        ) {
            getCount++;
            return cachedSnapshot == null
                    ? Mono.empty()
                    : Mono.just(cachedSnapshot);
        }

        @Override
        public Mono<Void> putCompletedSnapshot(
                IdempotencyScope scope,
                IdempotencyKey idempotencyKey,
                String requestFingerprint,
                String responseBodyJson,
                Duration ttl
        ) {
            putCount++;
            lastPutScope = scope;
            lastPutKey = idempotencyKey;
            lastPutFingerprint = requestFingerprint;
            lastPutResponseBodyJson = responseBodyJson;
            lastPutTtl = ttl;
            return Mono.empty();
        }
    }

    private static final class FakePaymentStatePersistencePort implements PaymentStatePersistencePort {

        private RuntimeException saveError;
        private int saveCount;
        private Payment lastPayment;

        void reset() {
            saveError = null;
            saveCount = 0;
            lastPayment = null;
        }

        @Override
        public Mono<Payment> save(Payment payment) {
            saveCount++;
            lastPayment = payment;

            if (saveError != null) {
                return Mono.error(saveError);
            }

            return Mono.just(payment);
        }
    }

    private static final class FakePaymentOutboxEventWriter implements PaymentOutboxEventWriter {

        private RuntimeException writeError;
        private int writeCount;
        private Payment lastPayment;
        private String lastCorrelationId;

        void reset() {
            writeError = null;
            writeCount = 0;
            lastPayment = null;
            lastCorrelationId = null;
        }

        @Override
        public Mono<Void> writeAuthorizationEvents(
                Payment payment,
                String correlationId
        ) {
            writeCount++;
            lastPayment = payment;
            lastCorrelationId = correlationId;

            if (writeError != null) {
                return Mono.error(writeError);
            }

            return Mono.empty();
        }
    }

    private static final class FakeRiskScoringClient implements RiskScoringClient {

        private RiskScoringResponse response = RiskScoringResponse.approved(
                12,
                List.of("LOW_RISK_PAYMENT"),
                "risk-rules-v1"
        );
        private int scoreCount;
        private RiskScoringRequest lastRequest;

        void reset() {
            response = RiskScoringResponse.approved(
                    12,
                    List.of("LOW_RISK_PAYMENT"),
                    "risk-rules-v1"
            );
            scoreCount = 0;
            lastRequest = null;
        }

        @Override
        public Mono<RiskScoringResponse> score(RiskScoringRequest request) {
            scoreCount++;
            lastRequest = request;
            return Mono.just(response);
        }
    }
}
