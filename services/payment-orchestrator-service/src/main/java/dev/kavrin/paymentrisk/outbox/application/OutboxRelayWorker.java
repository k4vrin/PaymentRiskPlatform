package dev.kavrin.paymentrisk.outbox.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "payment-risk.outbox.relay", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class OutboxRelayWorker {

    private final OutboxRelayProperties properties;
    private final OutboxRelayEventClaimer claimer;
    private final OutboxEventPublisher publisher;
    private final OutboxRelayStatusUpdater statusUpdater;

    @Scheduled(fixedDelayString = "${payment-risk.outbox.relay.fixed-delay-millis:5000}")
    public void relayBatch() {
        runOnce().subscribe();
    }

    public Mono<Long> runOnce() {
        if (!properties.isEnabled()) {
            return Mono.just(0L);
        }

        var query = OutboxRelayQuery.builder()
                .now(Instant.now())
                .batchSize(properties.getBatchSize())
                .skipLocked(true)
                .build();

        return claimer.claimRelayCandidates(query, properties.getInstanceId())
                .concatMap(event ->
                        publisher.publish(event)
                                .then(Mono.defer(() -> statusUpdater.markPublished(event.eventId())))
                                .doOnSuccess(ignored -> log.info(
                                        "Published outbox event eventId={} eventType={} aggregateId={}",
                                        event.eventId(),
                                        event.eventType(),
                                        event.aggregateId()
                                ))
                                .onErrorResume(error ->
                                        Mono.defer(() -> statusUpdater.markFailed(event.eventId(), error.getMessage()))
                                                .then(Mono.fromRunnable(() -> log.warn(
                                                        "Failed to publish outbox event eventId={} eventType={} error={}",
                                                        event.eventId(),
                                                        event.eventType(),
                                                        error.getMessage()
                                                )))
                                )
                                .thenReturn(1L)
                )
                .reduce(0L, Long::sum);
    }
}
