package dev.kavrin.paymentrisk.payment.application.service;

import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKeyConflictException;
import dev.kavrin.paymentrisk.payment.application.command.AuthorizePaymentCommand;
import dev.kavrin.paymentrisk.payment.application.command.AuthorizePaymentResult;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultAuthorizePaymentServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-25T10:15:30Z");

    private final DefaultAuthorizePaymentService service = new DefaultAuthorizePaymentService(
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void authorizeCreatesContractOnlyAuthorizedResult() {
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
    }

    @Test
    void authorizeRejectsDomainInvalidIdempotencyKey() {
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
    }

    @Test
    void authorizeReturnsStoredResultForDuplicateIdempotencyKeyAndSameRequest() {
        AuthorizePaymentCommand command = validCommand();

        AuthorizePaymentResult firstResult = service.authorize(command).block();
        AuthorizePaymentResult duplicateResult = service.authorize(command).block();

        assertThat(duplicateResult).isEqualTo(firstResult);
    }

    @Test
    void authorizeRejectsDuplicateIdempotencyKeyWithDifferentRequestFingerprint() {
        AuthorizePaymentCommand originalCommand = validCommand();
        AuthorizePaymentCommand conflictingCommand = validCommandWithAmount(1599);

        service.authorize(originalCommand).block();

        assertThatThrownBy(() -> service.authorize(conflictingCommand).block())
                .isInstanceOf(IdempotencyKeyConflictException.class)
                .hasMessage("Idempotency key was already used for a different request");
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
}
