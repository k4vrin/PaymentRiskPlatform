package dev.kavrin.paymentrisk.payment.application.service;

import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKey;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyScope;
import dev.kavrin.paymentrisk.idempotency.infrastructure.persistence.DatabaseIdempotencyResultOperations;
import dev.kavrin.paymentrisk.payment.application.command.AuthorizePaymentCommand;
import dev.kavrin.paymentrisk.payment.application.command.AuthorizePaymentResult;
import dev.kavrin.paymentrisk.payment.domain.model.*;
import dev.kavrin.paymentrisk.risk.application.RiskScoringClient;
import dev.kavrin.paymentrisk.risk.application.dto.RiskScoringRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class DefaultAuthorizePaymentService implements AuthorizePaymentService {

    private final Clock clock;
    private final DatabaseIdempotencyResultOperations idempotencyStore;
    private final PaymentStatePersistencePort paymentStatePersistence;
    private final RiskScoringClient riskScoringClient;
    private final RiskDecisionMappingPolicy riskDecisionMappingPolicy;

    @Override
    public Mono<AuthorizePaymentResult> authorize(AuthorizePaymentCommand command) {
        IdempotencyScope scope = IdempotencyScope.PAYMENT_AUTHORIZATION;
        IdempotencyKey idempotencyKey = IdempotencyKey.of(command.idempotencyKey());
        String fingerprint = requestFingerprint(command);

        Instant now = clock.instant();
        Instant expiresAt = now.plus(Duration.ofHours(24));

        return idempotencyStore
                .findCompletedResult(
                        scope,
                        idempotencyKey,
                        fingerprint,
                        now,
                        AuthorizePaymentResult.class
                )
                .switchIfEmpty(Mono.defer(() ->
                        idempotencyStore.insertStarted(
                                        scope,
                                        idempotencyKey,
                                        fingerprint,
                                        now,
                                        expiresAt
                                )
                                .then(createAndCompleteAuthorization(command, scope, idempotencyKey, fingerprint))
                ));
    }

    private Mono<AuthorizePaymentResult> createAndCompleteAuthorization(
            AuthorizePaymentCommand command,
            IdempotencyScope scope,
            IdempotencyKey idempotencyKey,
            String fingerprint
    ) {
        return Mono.fromSupplier(() -> createPayment(command, idempotencyKey))
                .flatMap(payment ->
                        riskScoringClient.score(toRiskScoringRequest(command, payment))
                                .map(riskDecisionMappingPolicy::map)
                                .map(riskDecision -> applyRiskDecision(payment, riskDecision))
                )
                .flatMap(paymentStatePersistence::save)
                .map(payment -> toResult(payment, command.correlationId()))
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
            case PaymentAuthorization.Authorized auth ->
                    auth.authorizationCode().value();
            case PaymentAuthorization.Requested ignored ->
                    null;
            case PaymentAuthorization.RiskPending ignored ->
                    null;
            case PaymentAuthorization.Declined ignored ->
                    null;
            case PaymentAuthorization.Failed ignored ->
                    null;
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
