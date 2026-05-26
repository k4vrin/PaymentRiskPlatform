package dev.kavrin.paymentrisk.payment.application.service;

import dev.kavrin.paymentrisk.idempotency.application.IdempotencyService;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKey;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyScope;
import dev.kavrin.paymentrisk.payment.application.command.AuthorizePaymentCommand;
import dev.kavrin.paymentrisk.payment.application.command.AuthorizePaymentResult;
import dev.kavrin.paymentrisk.payment.domain.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

@Service
public class DefaultAuthorizePaymentService implements AuthorizePaymentService {

    private static final String CONTRACT_ONLY_RULE_VERSION = "contract-only-v1";

    private final Clock clock;
    private final IdempotencyService idempotencyService;

    @Autowired
    public DefaultAuthorizePaymentService(IdempotencyService idempotencyService) {
        this(Clock.systemUTC(), idempotencyService);
    }

    DefaultAuthorizePaymentService(Clock clock) {
        this(clock, new IdempotencyService());
    }

    DefaultAuthorizePaymentService(Clock clock, IdempotencyService idempotencyService) {
        this.clock = clock;
        this.idempotencyService = idempotencyService;
    }

    @Override
    public Mono<AuthorizePaymentResult> authorize(AuthorizePaymentCommand command) {
        return Mono.fromSupplier(() -> authorizeIdempotently(command));
    }

    private AuthorizePaymentResult authorizeIdempotently(AuthorizePaymentCommand command) {
        Instant now = clock.instant();
        IdempotencyKey idempotencyKey = IdempotencyKey.of(command.idempotencyKey());
        String requestFingerprint = requestFingerprint(command);

        return idempotencyService.getOrCreateCompletedResult(
                IdempotencyScope.PAYMENT_AUTHORIZATION,
                idempotencyKey,
                requestFingerprint,
                now,
                () -> authorizeNewPayment(command, idempotencyKey, now)
        );
    }

    private AuthorizePaymentResult authorizeNewPayment(
            AuthorizePaymentCommand command,
            IdempotencyKey idempotencyKey,
            Instant now
    ) {
        Payment payment = Payment.newAuthorizationAttempt(
                PaymentId.generate(),
                MerchantId.of(command.merchantId()),
                CustomerId.of(command.customerId()),
                Money.of(command.amountMinor(), command.currency()),
                PaymentMethodToken.of(command.paymentMethodToken()),
                DeviceFingerprint.of(command.deviceFingerprint()),
                ExternalReference.optional(command.externalReference()).orElse(null),
                idempotencyKey,
                now
        );

        payment.markRiskPending(now);

        PaymentRiskDecision riskDecision = new PaymentRiskDecision(
                RiskDecision.APPROVED,
                0,
                List.of("CONTRACT_ONLY_APPROVAL"),
                CONTRACT_ONLY_RULE_VERSION,
                now
        );

        AuthorizationCode authorizationCode = AuthorizationCode.generate();
        payment.markAuthorized(riskDecision, authorizationCode, now);

        // TODO Phase 2 persistence: save payment, authorization, risk decision, and outbox event.
        // TODO Phase 2 risk integration: replace this contract-only approval with the Go gRPC risk service result.

        return new AuthorizePaymentResult(
                payment.getId().value(),
                payment.getStatus().name(),
                authorizationCode.value(),
                riskDecision.decision().name(),
                riskDecision.reasonCodes(),
                command.correlationId(),
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
