package dev.kavrin.paymentrisk.payment.application.service;

import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKey;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyScope;
import dev.kavrin.paymentrisk.idempotency.infrastructure.persistence.DatabaseIdempotencyResultOperations;
import dev.kavrin.paymentrisk.idempotency.infrastructure.redis.RedisIdempotencySnapshotCache;
import dev.kavrin.paymentrisk.payment.application.command.AuthorizePaymentCommand;
import dev.kavrin.paymentrisk.payment.application.command.AuthorizePaymentResult;
import dev.kavrin.paymentrisk.payment.application.outbox.PaymentOutboxEventWriter;
import dev.kavrin.paymentrisk.payment.domain.model.*;
import dev.kavrin.paymentrisk.risk.application.RiskScoringClient;
import dev.kavrin.paymentrisk.risk.application.dto.RiskScoringRequest;
import dev.kavrin.paymentrisk.risk.application.dto.RiskScoringResponse;
import dev.kavrin.paymentrisk.shared.observability.metrics.PaymentAuthorizationMetrics;
import dev.kavrin.paymentrisk.shared.observability.metrics.IdempotencyCacheMetrics;
import dev.kavrin.paymentrisk.shared.api.error.DownstreamTimeoutException;
import dev.kavrin.paymentrisk.shared.api.error.DownstreamUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DefaultAuthorizePaymentService implements AuthorizePaymentService {

    private final Clock clock;
    private final DatabaseIdempotencyResultOperations idempotencyStore;
    private final PaymentStatePersistencePort paymentStatePersistence;
    private final RiskScoringClient riskScoringClient;
    private final RiskDecisionMappingPolicy riskDecisionMappingPolicy;
    private final PaymentOutboxEventWriter paymentOutboxEventWriter;
    private final TransactionalOperator transactionalOperator;
    private final Optional<RedisIdempotencySnapshotCache> idempotencySnapshotCache;
    private final AuthorizePaymentResultSnapshotSerializer snapshotSerializer;
    private final PaymentAuthorizationMetrics authorizationMetrics;
    private final IdempotencyCacheMetrics idempotencyCacheMetrics;

    @Override
    public Mono<AuthorizePaymentResult> authorize(AuthorizePaymentCommand command) {
        IdempotencyScope scope = IdempotencyScope.PAYMENT_AUTHORIZATION;
        IdempotencyKey idempotencyKey = IdempotencyKey.of(command.idempotencyKey());
        String fingerprint = requestFingerprint(command);

        Instant now = clock.instant();
        Instant expiresAt = now.plus(Duration.ofHours(24));

        authorizationMetrics.recordAuthorizationAttempt();

        return findCompletedCachedResult(scope, idempotencyKey, fingerprint)
                .switchIfEmpty(Mono.defer(() -> findCompletedDatabaseResultAndRefreshCache(
                        scope,
                        idempotencyKey,
                        fingerprint,
                        now
                )))
                .switchIfEmpty(Mono.defer(() ->
                        idempotencyStore.insertStarted(
                                        scope,
                                        idempotencyKey,
                                        fingerprint,
                                        now,
                                        expiresAt
                                )
                                .then(createAndCompleteAuthorization(
                                        command,
                                        scope,
                                        idempotencyKey,
                                        fingerprint,
                                        expiresAt
                                ))
                ));
    }

    private Mono<AuthorizePaymentResult> createAndCompleteAuthorization(
            AuthorizePaymentCommand command,
            IdempotencyScope scope,
            IdempotencyKey idempotencyKey,
            String fingerprint,
            Instant expiresAt
    ) {
        return Mono.fromSupplier(() -> createPayment(command, idempotencyKey))
                .flatMap(payment ->
                        scoreRisk(command, payment)
                                .map(riskDecisionMappingPolicy::map)
                                .map(riskDecision -> applyRiskDecision(payment, riskDecision))
                )
                .flatMap(payment ->
                        persistAuthorizationTransactionally(
                                payment,
                                command,
                                scope,
                                idempotencyKey,
                                fingerprint
                        )
                )
                .flatMap(result -> cacheCompletedResult(
                                scope,
                                idempotencyKey,
                                fingerprint,
                                result,
                                expiresAt
                        )
                        .thenReturn(result)
                )
                .onErrorResume(error ->
                        idempotencyStore.markFailedAndExpire(
                                        scope,
                                        idempotencyKey,
                                        fingerprint,
                                        clock.instant()
                                )
                                .then(Mono.error(error))
                );
    }

    private Mono<AuthorizePaymentResult> persistAuthorizationTransactionally(
            Payment payment,
            AuthorizePaymentCommand command,
            IdempotencyScope scope,
            IdempotencyKey idempotencyKey,
            String fingerprint
    ) {
        return paymentStatePersistence.save(payment)
                .flatMap(savedPayment ->
                        paymentOutboxEventWriter.writeAuthorizationEvents(
                                        savedPayment,
                                        command.correlationId()
                                )
                                .thenReturn(savedPayment)
                )
                .map(savedPayment -> toResult(savedPayment, command.correlationId()))
                .flatMap(result ->
                        idempotencyStore.markCompleted(
                                        scope,
                                        idempotencyKey,
                                        fingerprint,
                                        result,
                                        200,
                                clock.instant()
                        )
                                .thenReturn(result)
                )
                .doOnNext(this::recordAuthorizationResultMetrics)
                .as(transactionalOperator::transactional);
    }

    private Mono<RiskScoringResponse> scoreRisk(
            AuthorizePaymentCommand command,
            Payment payment
    ) {
        long startNanos = System.nanoTime();

        return riskScoringClient.score(toRiskScoringRequest(command, payment))
                .doOnNext(response -> recordRiskResponseMetrics(response, startNanos))
                .doOnError(error -> recordRiskErrorMetrics(error, startNanos));
    }

    private void recordRiskResponseMetrics(RiskScoringResponse response, long startNanos) {
        authorizationMetrics.recordRiskServiceLatency(elapsed(startNanos));

        switch (response.outcome()) {
            case TIMEOUT -> authorizationMetrics.recordRiskTimeout();
            case UNAVAILABLE -> authorizationMetrics.recordRiskUnavailable();
            default -> {
            }
        }
    }

    private void recordRiskErrorMetrics(Throwable error, long startNanos) {
        authorizationMetrics.recordRiskServiceLatency(elapsed(startNanos));

        if (error instanceof DownstreamTimeoutException) {
            authorizationMetrics.recordRiskTimeout();
        }

        if (error instanceof DownstreamUnavailableException) {
            authorizationMetrics.recordRiskUnavailable();
        }
    }

    private static Duration elapsed(long startNanos) {
        return Duration.ofNanos(System.nanoTime() - startNanos);
    }

    private Mono<AuthorizePaymentResult> findCompletedCachedResult(
            IdempotencyScope scope,
            IdempotencyKey idempotencyKey,
            String fingerprint
    ) {
        return idempotencySnapshotCache
                .map(cache -> cache.getCompletedSnapshot(scope, idempotencyKey)
                        .flatMap(snapshot -> {
                            if (!fingerprint.equals(snapshot.requestFingerprint())) {
                                return Mono.empty();
                            }

                            authorizationMetrics.recordDuplicateIdempotencyReplay();

                            return Mono.just(snapshotSerializer.deserialize(
                                    snapshot.responseBodyJson(),
                                    AuthorizePaymentResult.class
                            ));
                        })
                        .onErrorResume(ignored -> Mono.empty()))
                .orElseGet(Mono::empty);
    }

    private Mono<AuthorizePaymentResult> findCompletedDatabaseResultAndRefreshCache(
            IdempotencyScope scope,
            IdempotencyKey idempotencyKey,
            String fingerprint,
            Instant now
    ) {
        return idempotencyStore
                .findCompletedResultWithMetadata(
                        scope,
                        idempotencyKey,
                        fingerprint,
                        now,
                        AuthorizePaymentResult.class
                )
                .flatMap(storedResult -> {
                    authorizationMetrics.recordDuplicateIdempotencyReplay();
                    idempotencyCacheMetrics.recordDatabaseFallbackHit(scope.name());

                    return cacheCompletedResult(
                                    scope,
                                    idempotencyKey,
                                    fingerprint,
                                    storedResult.response(),
                                    storedResult.expiresAt()
                            )
                            .thenReturn(storedResult.response());
                });
    }

    private Mono<Void> cacheCompletedResult(
            IdempotencyScope scope,
            IdempotencyKey idempotencyKey,
            String fingerprint,
            AuthorizePaymentResult result,
            Instant expiresAt
    ) {
        Duration ttl = Duration.between(clock.instant(), expiresAt);

        if (ttl.isZero() || ttl.isNegative()) {
            return Mono.empty();
        }

        return idempotencySnapshotCache
                .map(cache -> cache.putCompletedSnapshot(
                                scope,
                                idempotencyKey,
                                fingerprint,
                                snapshotSerializer.serialize(result),
                                ttl
                        )
                        .onErrorResume(ignored -> Mono.empty()))
                .orElseGet(Mono::empty);
    }

    private Payment applyRiskDecision(
            Payment payment,
            PaymentRiskDecision riskDecision
    ) {
        if (riskDecision.decision() == RiskDecision.APPROVED) {
            payment.markAuthorized(
                    riskDecision,
                    AuthorizationCode.generate(),
                    clock.instant()
            );
            return payment;
        }

        if (riskDecision.decision() == RiskDecision.DECLINED) {
            payment.markDeclined(
                    riskDecision,
                    clock.instant()
            );
            return payment;
        }

        throw new IllegalStateException(
                "Unsupported mapped risk decision: " + riskDecision.decision()
        );
    }

    private static RiskScoringRequest toRiskScoringRequest(
            AuthorizePaymentCommand command,
            Payment payment
    ) {
        return new RiskScoringRequest(
                payment.getId().value(),
                command.amountMinor(),
                command.currency(),
                command.merchantId(),
                command.customerId(),
                command.deviceFingerprint(),
                command.correlationId()
        );
    }

    private Payment createPayment(
            AuthorizePaymentCommand command,
            IdempotencyKey idempotencyKey
    ) {
        Instant now = clock.instant();
        Payment payment = Payment.newAuthorizationAttempt(
                PaymentId.generate(),
                MerchantId.of(command.merchantId()),
                CustomerId.of(command.customerId()),
                Money.of(command.amountMinor(), command.currency()),
                PaymentMethodToken.of(command.paymentMethodToken()),
                DeviceFingerprint.of(command.deviceFingerprint()),
                ExternalReference.ofNullable(command.externalReference()),
                idempotencyKey,
                now
        );

        payment.markRiskPending(now);
        return payment;
    }

    private static AuthorizePaymentResult toResult(
            Payment payment,
            String correlationId
    ) {
        var riskDecision = payment.getRiskDecision();

        if (riskDecision == null) {
            throw new IllegalStateException("Payment is missing risk decision");
        }

        var authorizationCode = switch (payment.getAuthorization()) {
            case PaymentAuthorization.Authorized auth -> auth.authorizationCode().value();
            case PaymentAuthorization.Requested ignored -> null;
            case PaymentAuthorization.RiskPending ignored -> null;
            case PaymentAuthorization.Declined ignored -> null;
            case PaymentAuthorization.Failed ignored -> null;
        };


        return new AuthorizePaymentResult(
                payment.getId().value(),
                payment.getStatus().name(),
                authorizationCode,
                riskDecision.decision().name(),
                riskDecision.reasonCodes(),
                correlationId,
                riskDecision.score(),
                riskDecision.ruleVersion(),
                payment.getCreatedAt()
        );
    }

    private void recordAuthorizationResultMetrics(AuthorizePaymentResult result) {
        String outcome = result.reasonCodes().contains("REVIEW_REQUIRED")
                ? "REVIEW_REQUIRED"
                : result.status();

        authorizationMetrics.recordAuthorizationOutcome(outcome);

        if ("DECLINED".equals(result.status())) {
            result.reasonCodes().forEach(authorizationMetrics::recordDeclineReason);
        }
    }

    private static String requestFingerprint(AuthorizePaymentCommand command) {
        String canonicalRequest = String.join("\n",
                command.merchantId(),
                command.customerId(),
                Long.toString(command.amountMinor()),
                command.currency(),
                command.paymentMethodToken(),
                command.deviceFingerprint(),
                command.externalReference() == null ? "" : command.externalReference()
        );

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonicalRequest.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
