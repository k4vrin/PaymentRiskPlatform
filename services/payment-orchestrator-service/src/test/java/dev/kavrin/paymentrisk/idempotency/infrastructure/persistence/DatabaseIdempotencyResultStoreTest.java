package dev.kavrin.paymentrisk.idempotency.infrastructure.persistence;

import dev.kavrin.paymentrisk.TestPostgresConfiguration;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKey;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKeyConflictException;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyScope;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyStatus;
import dev.kavrin.paymentrisk.payment.application.command.AuthorizePaymentResult;
import dev.kavrin.paymentrisk.payment.application.service.AuthorizePaymentResultSnapshotSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration,"
                + "org.springframework.boot.data.redis.autoconfigure.DataRedisReactiveAutoConfiguration,"
                + "org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration,"
                + "org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration,"
                + "org.springframework.boot.security.autoconfigure.web.reactive.ReactiveWebSecurityAutoConfiguration,"
                + "org.springframework.boot.security.autoconfigure.actuate.web.reactive.ReactiveManagementWebSecurityAutoConfiguration"
})
@ActiveProfiles("test")
@Import(TestPostgresConfiguration.class)
class DatabaseIdempotencyResultStoreTest {

    private static final Instant NOW = Instant.parse("2026-05-26T10:00:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-05-26T09:00:00Z");
    private static final String FINGERPRINT = "fingerprint-sha256";
    private static final IdempotencyKey IDEMPOTENCY_KEY = IdempotencyKey.of("idem_01HX7QK9JP7E5W5NRZ6T5Q3R1A");

    @Autowired
    private IdempotencyRecordEntityRepository repository;

    @Autowired
    private DatabaseIdempotencyResultStore store;

    @Autowired
    private AuthorizePaymentResultSnapshotSerializer snapshotSerializer;

    @BeforeEach
    void deleteExistingRecords() {
        repository.deleteAll().block();
    }

    @Test
    void missingRecordReturnsMiss() {
        Optional<AuthorizePaymentResult> result = findCompletedResult().blockOptional();

        assertThat(result).isEmpty();
    }

    @Test
    void expiredRecordReturnsMissWithoutCheckingFingerprint() {
        repository.save(completedEntity()
                .requestFingerprint("different-fingerprint")
                .expiresAt(NOW)
                .build()).block();

        Optional<AuthorizePaymentResult> result = findCompletedResult().blockOptional();

        assertThat(result).isEmpty();
    }

    @Test
    void completedRecordWithMatchingFingerprintReturnsStoredResponse() {
        AuthorizePaymentResult storedResponse = storedResponse();
        repository.save(completedEntity()
                .responseBodyJson(snapshotSerializer.serialize(storedResponse))
                .build()).block();

        AuthorizePaymentResult result = findCompletedResult().block();

        assertThat(result).isEqualTo(storedResponse);
    }

    @Test
    void completedRecordWithDifferentFingerprintThrowsConflict() {
        repository.save(completedEntity()
                .requestFingerprint("different-fingerprint")
                .build()).block();

        assertThatThrownBy(() -> findCompletedResult().block())
                .isInstanceOf(IdempotencyKeyConflictException.class)
                .hasMessage("Idempotency key was already used for a different request");
    }

    @Test
    void nonCompletedRecordWithMatchingFingerprintReturnsMiss() {
        repository.save(completedEntity()
                .status(IdempotencyStatus.STARTED.name())
                .responseBodyJson(null)
                .build()).block();

        Optional<AuthorizePaymentResult> result = findCompletedResult().blockOptional();

        assertThat(result).isEmpty();
    }

    private Mono<AuthorizePaymentResult> findCompletedResult() {
        return store.findCompletedResult(
                IdempotencyScope.PAYMENT_AUTHORIZATION,
                IDEMPOTENCY_KEY,
                FINGERPRINT,
                NOW,
                AuthorizePaymentResult.class
        );
    }

    private IdempotencyRecordEntity.IdempotencyRecordEntityBuilder completedEntity() {
        return IdempotencyRecordEntity.builder()
                .idempotencyRecordId("idem_rec_01")
                .scope("payment_authorization")
                .idempotencyKey(IDEMPOTENCY_KEY.value())
                .requestFingerprint(FINGERPRINT)
                .paymentId("pay_01HX7R0BYV9Y6CNW3HZ7R8E4P2")
                .status(IdempotencyStatus.COMPLETED.name())
                .responseStatus(200)
                .responseBodyJson(snapshotSerializer.serialize(storedResponse()))
                .expiresAt(NOW.plusSeconds(3600))
                .createdAt(CREATED_AT)
                .updatedAt(CREATED_AT);
    }

    private static AuthorizePaymentResult storedResponse() {
        return new AuthorizePaymentResult(
                "pay_01HX7R0BYV9Y6CNW3HZ7R8E4P2",
                "AUTHORIZED",
                "AUTH-ABCDEFG123",
                "APPROVED",
                List.of("LOW_RISK"),
                "corr-authorization-service",
                18,
                "risk-rules-v1",
                CREATED_AT
        );
    }
}
