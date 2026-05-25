package dev.kavrin.paymentrisk.payment.application.service;

import dev.kavrin.paymentrisk.payment.application.command.AuthorizePaymentCommand;
import dev.kavrin.paymentrisk.payment.application.command.AuthorizePaymentResult;
import dev.kavrin.paymentrisk.payment.domain.model.*;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class DefaultAuthorizePaymentService implements AuthorizePaymentService {

    private static final String CONTRACT_ONLY_RULE_VERSION = "contract-only-v1";

    private final Clock clock;

    public DefaultAuthorizePaymentService() {
        this(Clock.systemUTC());
    }

    DefaultAuthorizePaymentService(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Mono<AuthorizePaymentResult> authorize(AuthorizePaymentCommand command) {
        return Mono.fromSupplier(() -> authorizeSynchronously(command));
    }

    private AuthorizePaymentResult authorizeSynchronously(AuthorizePaymentCommand command) {
        Instant now = clock.instant();

        Payment payment = Payment.newAuthorizationAttempt(
                PaymentId.generate(),
                MerchantId.of(command.merchantId()),
                CustomerId.of(command.customerId()),
                Money.of(command.amountMinor(), command.currency()),
                PaymentMethodToken.of(command.paymentMethodToken()),
                DeviceFingerprint.of(command.deviceFingerprint()),
                ExternalReference.optional(command.externalReference()).orElse(null),
                IdempotencyKey.of(command.idempotencyKey()),
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

        // TODO Phase 2 persistence: save payment, authorization, risk decision, idempotency snapshot, and outbox event.
        // TODO Phase 2 risk integration: replace this contract-only approval with the Go gRPC risk service result.
        // TODO Phase 2 idempotency: check duplicate idempotency key before creating a new aggregate.

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
}
