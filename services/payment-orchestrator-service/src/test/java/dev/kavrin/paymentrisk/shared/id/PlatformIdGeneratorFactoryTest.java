package dev.kavrin.paymentrisk.shared.id;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformIdGeneratorFactoryTest {

    private final PlatformIdGeneratorFactory generator = new PlatformIdGeneratorFactory();

    @Test
    void generatesReadableDomainAndCorrelationIds() {
        assertThat(generator.paymentId()).startsWith("pay_");
        assertThat(UUID.fromString(generator.paymentId().substring("pay_".length()))).isNotNull();
        assertThat(UUID.fromString(generator.correlationId())).isNotNull();
    }

    @Test
    void generatesCompactPersistenceAndEventIds() {
        assertCompactPrefixedId(generator.paymentAuthorizationId(), "pauth_");
        assertCompactPrefixedId(generator.paymentRiskDecisionId(), "prd_");
        assertCompactPrefixedId(generator.idempotencyRecordId(), "idem_rec_");
        assertCompactPrefixedId(generator.outboxEventId(), "evt_");
    }

    private static void assertCompactPrefixedId(String id, String prefix) {
        assertThat(id).startsWith(prefix);
        assertThat(id.substring(prefix.length()))
                .hasSize(32)
                .matches("[0-9a-f]{32}");
        assertThat(id.substring(prefix.length())).doesNotContain("-");
    }
}
