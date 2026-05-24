package dev.kavrin.paymentrisk.shared.api.contract;

import dev.kavrin.paymentrisk.shared.api.version.ApiPaths;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(
        properties = {
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.jdbc.autoconfigure.DataSourceInitializationAutoConfiguration,"
                        + "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration,"
                        + "org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration,"
                        + "org.springframework.boot.r2dbc.autoconfigure.R2dbcAutoConfiguration,"
                        + "org.springframework.boot.data.r2dbc.autoconfigure.DataR2dbcAutoConfiguration,"
                        + "org.springframework.boot.data.r2dbc.autoconfigure.DataR2dbcRepositoriesAutoConfiguration,"
                        + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration,"
                        + "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration,"
                        + "org.springframework.boot.data.redis.autoconfigure.DataRedisReactiveAutoConfiguration,"
                        + "org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration,"
                        + "org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration,"
                        + "org.springframework.boot.security.autoconfigure.web.reactive.ReactiveWebSecurityAutoConfiguration,"
                        + "org.springframework.boot.security.autoconfigure.actuate.web.reactive.ReactiveManagementWebSecurityAutoConfiguration"
        }
)
class OpenApiContractTest {

    private final WebTestClient webTestClient;

    OpenApiContractTest(ApplicationContext applicationContext) {
        this.webTestClient = WebTestClient.bindToApplicationContext(applicationContext).build();
    }

    @Test
    void openApiJsonExposesContractPingEndpoint() {
        webTestClient.get()
                .uri("/v3/api-docs")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.paths['" + ApiPaths.API_V1 + "/contract/ping']").exists();
    }
}
