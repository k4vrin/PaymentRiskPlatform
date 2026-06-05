package dev.kavrin.paymentrisk.callback.infrastructure.http;

import dev.kavrin.paymentrisk.callback.application.PartnerWebhookClient;
import dev.kavrin.paymentrisk.callback.application.command.CallPartnerWebhookCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "payment-risk.rabbitmq.callback", name = "enabled", havingValue = "true")
public class WebClientPartnerWebhookClient implements PartnerWebhookClient {

    private final WebClient.Builder webClientBuilder;

    @Override
    public Mono<Void> call(CallPartnerWebhookCommand command) {
        return webClientBuilder.build()
                .post()
                .uri(command.targetUrl())
                .header("X-Correlation-Id", command.correlationId())
                .bodyValue(command)
                .retrieve()
                .toBodilessEntity()
                .then();
    }
}
