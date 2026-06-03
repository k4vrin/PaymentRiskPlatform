package dev.kavrin.paymentrisk.payment.application.service;

import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKey;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyScope;
import dev.kavrin.paymentrisk.idempotency.infrastructure.persistence.DatabaseIdempotencyResultOperations;
import dev.kavrin.paymentrisk.payment.application.command.ReversePaymentCommand;
import dev.kavrin.paymentrisk.payment.application.command.ReversePaymentRequestFingerprint;
import dev.kavrin.paymentrisk.payment.application.command.ReversePaymentResult;
import dev.kavrin.paymentrisk.payment.domain.model.Payment;
import dev.kavrin.paymentrisk.payment.domain.model.PaymentId;
import dev.kavrin.paymentrisk.payment.domain.model.ReversalId;
import dev.kavrin.paymentrisk.payment.domain.model.ReversalReason;
import dev.kavrin.paymentrisk.payment.domain.policy.PaymentReversalPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class DefaultPaymentReversalService implements PaymentReversalService {

    private final Clock clock;
    private final DatabaseIdempotencyResultOperations idempotencyStore;
    private final PaymentStatePersistencePort paymentStatePersistence;
    private final TransactionalOperator transactionalOperator;

    @Override
    public Mono<ReversePaymentResult> reverse(ReversePaymentCommand command) {
        IdempotencyScope scope = IdempotencyScope.PAYMENT_REVERSAL;
        IdempotencyKey idempotencyKey = IdempotencyKey.of(command.idempotencyKey());
        String fingerprint = ReversePaymentRequestFingerprint.from(command);
        Instant now = clock.instant();
        Instant expiresAt = now.plus(Duration.ofHours(24));

        return idempotencyStore.findCompletedResult(
                        scope,
                        idempotencyKey,
                        fingerprint,
                        now,
                        ReversePaymentResult.class
                )
                .switchIfEmpty(Mono.defer(() ->
                        idempotencyStore.insertStarted(
                                        scope,
                                        idempotencyKey,
                                        fingerprint,
                                        now,
                                        expiresAt
                                )
                                .then(reverseAndComplete(
                                        command,
                                        scope,
                                        idempotencyKey,
                                        fingerprint
                                ))
                ));
    }

    private Mono<ReversePaymentResult> reverseAndComplete(
            ReversePaymentCommand command,
            IdempotencyScope scope,
            IdempotencyKey idempotencyKey,
            String fingerprint
    ) {
        return paymentStatePersistence.findByPaymentId(PaymentId.of(command.paymentId()))
                .map(payment -> applyReversal(payment, command))
                .flatMap(payment -> persistReversalTransactionally(
                        payment,
                        command,
                        scope,
                        idempotencyKey,
                        fingerprint
                ))
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

    private Payment applyReversal(
            Payment payment,
            ReversePaymentCommand command
    ) {
        PaymentReversalPolicy.assertReversible(payment);
        payment.markReversed(
                ReversalId.newId(),
                ReversalReason.ofNullable(command.reason()),
                clock.instant()
        );
        return payment;
    }

    private Mono<ReversePaymentResult> persistReversalTransactionally(
            Payment payment,
            ReversePaymentCommand command,
            IdempotencyScope scope,
            IdempotencyKey idempotencyKey,
            String fingerprint
    ) {
        return paymentStatePersistence.saveReversal(payment, idempotencyKey)
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
                .as(transactionalOperator::transactional);
    }

    private static ReversePaymentResult toResult(
            Payment payment,
            String correlationId
    ) {
        var reversal = payment.reversal()
                .orElseThrow(() -> new IllegalStateException("Payment is missing reversal state"));

        return new ReversePaymentResult(
                payment.getId().value(),
                reversal.reversalId().value(),
                reversal.status().name(),
                reversal.reason().value(),
                correlationId,
                reversal.reversedAt()
        );
    }
}
