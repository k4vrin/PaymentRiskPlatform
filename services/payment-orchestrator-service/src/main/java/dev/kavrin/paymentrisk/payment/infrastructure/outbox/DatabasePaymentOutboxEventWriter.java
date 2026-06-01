package dev.kavrin.paymentrisk.payment.infrastructure.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kavrin.paymentrisk.payment.application.outbox.PaymentOutboxEventWriter;
import dev.kavrin.paymentrisk.payment.domain.model.Payment;
import dev.kavrin.paymentrisk.payment.infrastructure.persistence.entities.OutboxEventEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DatabasePaymentOutboxEventWriter implements PaymentOutboxEventWriter {

    private final PaymentOutboxEventMapper mapper;
    private final R2dbcEntityTemplate entityTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> writeAuthorizationEvents(
            Payment payment,
            String correlationId
    ) {
        return Mono.fromCallable(() -> {
                    String requestedPayloadJson = serialize(
                            mapper.toAuthorizationRequestedPayload(payment)
                    );

                    String completedPayloadJson = serialize(
                            mapper.toAuthorizationCompletedPayload(payment)
                    );

                    return List.of(
                            mapper.toAuthorizationRequestedEvent(
                                    payment,
                                    correlationId,
                                    requestedPayloadJson
                            ),
                            mapper.toAuthorizationCompletedEvent(
                                    payment,
                                    correlationId,
                                    completedPayloadJson
                            )
                    );
                })
                .flatMapMany(Flux::fromIterable)
                .concatMap(event -> entityTemplate.insert(OutboxEventEntity.class)
                        .using(event))
                .then();
    }

    private String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Failed to serialize payment outbox payload",
                    exception
            );
        }
    }
}
