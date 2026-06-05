package dev.kavrin.paymentrisk.security.infrastructure.apikey;

import dev.kavrin.paymentrisk.TestPostgresConfiguration;
import dev.kavrin.paymentrisk.security.domain.MerchantApiKeyStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

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
class MerchantApiKeyRepositoryTest {

    private static final Instant CREATED_AT = Instant.parse("2026-06-05T12:00:00Z");
    private static final Instant ROTATED_AT = Instant.parse("2026-06-05T13:00:00Z");

    @Autowired
    private MerchantApiKeyRepository repository;

    @Autowired
    private R2dbcEntityTemplate entityTemplate;

    @BeforeEach
    void deleteExistingRecords() {
        repository.deleteAll().block();
    }

    @Test
    void storesAndFindsMerchantApiKeyByKeyId() {
        entityTemplate.insert(MerchantApiKeyEntity.class)
                .using(MerchantApiKeyEntity.builder()
                        .keyId("key_live_123")
                        .secretHash("hmac-sha256:storedhash")
                        .merchantId("merchant_123")
                        .status(MerchantApiKeyStatus.ACTIVE.name())
                        .createdAt(CREATED_AT)
                        .rotatedAt(ROTATED_AT)
                        .build())
                .block();

        var reloaded = repository.findByKeyId("key_live_123").block();

        assertThat(reloaded).isNotNull();
        assertThat(reloaded.id()).isNotNull();
        assertThat(reloaded.keyId()).isEqualTo("key_live_123");
        assertThat(reloaded.secretHash()).isEqualTo("hmac-sha256:storedhash");
        assertThat(reloaded.merchantId()).isEqualTo("merchant_123");
        assertThat(reloaded.status()).isEqualTo(MerchantApiKeyStatus.ACTIVE.name());
        assertThat(reloaded.createdAt()).isEqualTo(CREATED_AT);
        assertThat(reloaded.rotatedAt()).isEqualTo(ROTATED_AT);
    }
}
