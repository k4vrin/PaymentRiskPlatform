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
    /**
     * repositories are convenient when the framework owns identity detection. But when your app generates IDs before persistence,
     * explicit insert(...) avoids accidental update behavior and makes transaction tests more deterministic.
     */
    private final R2dbcEntityTemplate entityTemplate;
    private final ObjectMapper objectMapper;


    /**
     * Payload mapping and JSON serialization are synchronous operations.
     * <p>
     * We use Mono.fromCallable(...) so serialization happens lazily when the
     * reactive pipeline is subscribed to, and any serialization failure is
     * propagated as Mono.error(...) instead of being thrown immediately during
     * pipeline construction.
     * <p>
     * This keeps serialization inside the reactive transaction flow and allows
     * Reactor to handle errors consistently.
     */
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

    @Override
    public Mono<Void> writePaymentReversedEvents(Payment payment, String correlationId) {
        return Mono.fromCallable(() -> {
                    String payloadJson = serialize(
                            mapper.toPaymentReversedPayload(payment)
                    );

                    return mapper.toPaymentReversedEvent(
                            payment,
                            correlationId,
                            payloadJson
                    );
                })
                .flatMap(event -> entityTemplate.insert(OutboxEventEntity.class)
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
