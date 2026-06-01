package dev.kavrin.paymentrisk.idempotency.infrastructure.redis;

import dev.kavrin.paymentrisk.TestRedisConfiguration;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyKey;
import dev.kavrin.paymentrisk.idempotency.domain.IdempotencyScope;
import dev.kavrin.paymentrisk.shared.config.JacksonObjectMapperConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = SpringRedisIdempotencySnapshotCacheTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.r2dbc.autoconfigure.R2dbcAutoConfiguration,"
                        + "org.springframework.boot.r2dbc.autoconfigure.R2dbcTransactionManagerAutoConfiguration,"
                        + "org.springframework.boot.data.r2dbc.autoconfigure.DataR2dbcAutoConfiguration,"
                        + "org.springframework.boot.data.r2dbc.autoconfigure.R2dbcRepositoriesAutoConfiguration,"
                        + "org.springframework.boot.r2dbc.autoconfigure.health.ConnectionFactoryHealthContributorAutoConfiguration,"
                        + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration,"
                        + "org.springframework.boot.jdbc.autoconfigure.health.DataSourceHealthContributorAutoConfiguration,"
                        + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"
        }
)
class SpringRedisIdempotencySnapshotCacheTest {

    private static final IdempotencyKey IDEMPOTENCY_KEY =
            IdempotencyKey.of("idem_01HX7QK9JP7E5W5NRZ6T5Q3R1A");

    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;

    @Autowired
    private SpringRedisIdempotencySnapshotCache cache;

    @BeforeEach
    void clearRedis() {
        redisTemplate.getConnectionFactory()
                .getReactiveConnection()
                .serverCommands()
                .flushAll()
                .block();
    }

    @Test
    void missingSnapshotReturnsEmpty() {
        var snapshot = cache.getCompletedSnapshot(
                IdempotencyScope.PAYMENT_AUTHORIZATION,
                IDEMPOTENCY_KEY
        ).blockOptional();

        assertThat(snapshot).isEmpty();
    }

    @Test
    void storesAndReadsCompletedSnapshotWithTtl() {
        cache.putCompletedSnapshot(
                IdempotencyScope.PAYMENT_AUTHORIZATION,
                IDEMPOTENCY_KEY,
                "request-fingerprint",
                "{\"paymentId\":\"pay_123\"}",
                Duration.ofMinutes(5)
        ).block();

        var snapshot = cache.getCompletedSnapshot(
                IdempotencyScope.PAYMENT_AUTHORIZATION,
                IDEMPOTENCY_KEY
        ).block();

        assertThat(snapshot).isNotNull();
        assertThat(snapshot.requestFingerprint()).isEqualTo("request-fingerprint");
        assertThat(snapshot.responseBodyJson()).isEqualTo("{\"paymentId\":\"pay_123\"}");

        String redisKey = RedisIdempotencyKeyFormatter.completedSnapshotKey(
                IdempotencyScope.PAYMENT_AUTHORIZATION,
                IDEMPOTENCY_KEY
        );
        Duration ttl = redisTemplate.getExpire(redisKey).block();
        assertThat(ttl).isNotNull();
        assertThat(ttl).isPositive();
        assertThat(ttl).isLessThanOrEqualTo(Duration.ofMinutes(5));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({
            TestRedisConfiguration.class,
            JacksonObjectMapperConfiguration.class,
            RedisIdempotencySnapshotSerializer.class
    })
    static class TestApplication {

        @Bean
        SpringRedisIdempotencySnapshotCache springRedisIdempotencySnapshotCache(
                ReactiveStringRedisTemplate redisTemplate,
                RedisIdempotencySnapshotSerializer serializer
        ) {
            return new SpringRedisIdempotencySnapshotCache(redisTemplate, serializer);
        }
    }
}
