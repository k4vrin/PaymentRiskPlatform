package dev.kavrin.paymentrisk;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class PaymentOrchestratorServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentOrchestratorServiceApplication.class, args);
    }

    @Bean
    OpenAPI paymentRiskPlatformOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Reactive Payment Risk Platform API")
                        .version("v1")
                        .description("REST API contract for the payment orchestrator service."))
                .servers(List.of(new Server()
                        .url("http://localhost:8080")
                        .description("Local development server")));
    }

}
