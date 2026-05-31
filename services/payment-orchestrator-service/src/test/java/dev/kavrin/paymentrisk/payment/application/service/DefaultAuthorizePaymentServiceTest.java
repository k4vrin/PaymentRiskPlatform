package dev.kavrin.paymentrisk.payment.application.service;

import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKey;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKeyConflictException;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyScope;
import dev.kavrin.paymentrisk.idempotency.infrastructure.persistence.DatabaseIdempotencyResultOperations;
import dev.kavrin.paymentrisk.payment.application.command.AuthorizePaymentCommand;
import dev.kavrin.paymentrisk.payment.application.command.AuthorizePaymentResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultAuthorizePaymentServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-25T10:15:30Z");

    private final FakeDatabaseIdempotencyResultOperations idempotencyStore =
            new FakeDatabaseIdempotencyResultOperations();
    private final DefaultAuthorizePaymentService service = new DefaultAuthorizePaymentService(
            Clock.fixed(NOW, ZoneOffset.UTC),
            idempotencyStore
    );

    @BeforeEach
    void resetIdempotencyStore() {
        idempotencyStore.reset();
    }

    @Test
    void authorizeCreatesContractOnlyAuthorizedResultAndStoresCompletedIdempotencyResult() {
        AuthorizePaymentResult result = service.authorize(validCommand()).block();

        assertThat(result).isNotNull();
        assertThat(result.paymentId()).startsWith("pay_");
        assertThat(result.status()).isEqualTo("AUTHORIZED");
        assertThat(result.authorizationCode()).startsWith("AUTH-");
        assertThat(result.authorizationCode()).hasSize(17);
        assertThat(result.riskDecision()).isEqualTo("APPROVED");
        assertThat(result.reasonCodes()).containsExactly("CONTRACT_ONLY_APPROVAL");
        assertThat(result.correlationId()).isEqualTo("corr-authorization-service");
        assertThat(result.riskScore()).isZero();
        assertThat(result.ruleVersion()).isEqualTo("contract-only-v1");
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
    }

    @Test
    void authorizeRejectsDuplicateIdempotencyKeyWithDifferentRequestFingerprint() {
        idempotencyStore.findError = new IdempotencyKeyConflictException();

        assertThatThrownBy(() -> service.authorize(validCommandWithAmount(1599)).block())
                .isInstanceOf(IdempotencyKeyConflictException.class)
                .hasMessage("Idempotency key was already used for a different request");
        assertThat(idempotencyStore.findCount).isEqualTo(1);
        assertThat(idempotencyStore.insertStartedCount).isZero();
    }

    @Test
    void authorizeExpiresStartedRecordWhenCompletionFails() {
        idempotencyStore.markCompletedError = new IllegalStateException("completion failed");

        assertThatThrownBy(() -> service.authorize(validCommand()).block())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("completion failed");

        assertThat(idempotencyStore.insertStartedCount).isEqualTo(1);
        assertThat(idempotencyStore.markCompletedCount).isEqualTo(1);
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
            return Mono.empty();
        }
    }
}
