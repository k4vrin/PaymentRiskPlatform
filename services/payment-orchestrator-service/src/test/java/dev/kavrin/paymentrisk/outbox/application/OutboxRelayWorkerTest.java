package dev.kavrin.paymentrisk.outbox.application;

import dev.kavrin.paymentrisk.outbox.domain.OutboxEvent;
import dev.kavrin.paymentrisk.shared.messaging.MessagingObservability;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxRelayWorkerTest {

    @Test
    void shouldPublishClaimedEventsAndMarkSuccess() {
        var properties = enabledProperties();
        var claimer = new FakeClaimer(List.of(event("evt_1"), event("evt_2")));
        var publisher = new FakePublisher();
        var statusUpdater = new FakeStatusUpdater();
        var worker = worker(properties, claimer, publisher, statusUpdater);

        StepVerifier.create(worker.runOnce())
                .expectNext(2L)
                .verifyComplete();

        assertThat(claimer.relayInstanceId).isEqualTo("relay-test");
        assertThat(publisher.published).containsExactly("evt_1", "evt_2");
        assertThat(statusUpdater.published).containsExactly("evt_1", "evt_2");
        assertThat(statusUpdater.failed).isEmpty();
    }

    @Test
    void shouldContinueBatchAfterPublishFailure() {
        var properties = enabledProperties();
        var claimer = new FakeClaimer(List.of(event("evt_ok"), event("evt_fail"), event("evt_after")));
        var publisher = new FakePublisher("evt_fail");
        var statusUpdater = new FakeStatusUpdater();
        var worker = worker(properties, claimer, publisher, statusUpdater);

        StepVerifier.create(worker.runOnce())
                .expectNext(3L)
                .verifyComplete();

        assertThat(statusUpdater.published).containsExactly("evt_ok", "evt_after");
        assertThat(statusUpdater.failed).containsExactly("evt_fail");
        assertThat(statusUpdater.failureMessages).containsExactly("send failed");
    }

    @Test
    void shouldReturnZeroWhenRelayDisabled() {
        var properties = new OutboxRelayProperties();
        var worker = new OutboxRelayWorker(
                properties,
                new FakeClaimer(List.of(event("evt_1"))),
                new FakePublisher(),
                new FakeStatusUpdater(),
                new OutboxProducerRetryPolicy(enabledProperties()),
                observability()
        );

        StepVerifier.create(worker.runOnce())
                .expectNext(0L)
                .verifyComplete();
    }

    @Test
    void shouldReturnZeroForEmptyBatch() {
        var worker = new OutboxRelayWorker(
                enabledProperties(),
                new FakeClaimer(List.of()),
                new FakePublisher(),
                new FakeStatusUpdater(),
                new OutboxProducerRetryPolicy(enabledProperties()),
                observability()
        );

        StepVerifier.create(worker.runOnce())
                .expectNext(0L)
                .verifyComplete();
    }

    private static OutboxRelayProperties enabledProperties() {
        var properties = new OutboxRelayProperties();
        properties.setEnabled(true);
        properties.setBatchSize(10);
        properties.setInstanceId("relay-test");
        return properties;
    }

    private static OutboxRelayWorker worker(
            OutboxRelayProperties properties,
            FakeClaimer claimer,
            FakePublisher publisher,
            FakeStatusUpdater statusUpdater
    ) {
        return new OutboxRelayWorker(
                properties,
                claimer,
                publisher,
                statusUpdater,
                new OutboxProducerRetryPolicy(properties),
                observability()
        );
    }

    private static MessagingObservability observability() {
        return new MessagingObservability(new SimpleMeterRegistry());
    }

    private static OutboxEvent event(String eventId) {
        var now = Instant.parse("2026-06-05T08:00:00Z");
        return new OutboxEvent(
                eventId,
                "PAYMENT",
                "pay_" + eventId,
                "PaymentAuthorized",
                "v1",
                "payment-orchestrator-service",
                "corr_" + eventId,
                "{\"eventId\":\"" + eventId + "\"}",
                "PUBLISHING",
                0,
                now,
                null,
                now,
                now,
                null,
                now,
                "relay-test"
        );
    }

    private static final class FakeClaimer implements OutboxRelayEventClaimer {

        private final List<OutboxEvent> events;
        private String relayInstanceId;

        private FakeClaimer(List<OutboxEvent> events) {
            this.events = events;
        }

        @Override
        public Flux<OutboxEvent> claimRelayCandidates(OutboxRelayQuery query, String relayInstanceId) {
            this.relayInstanceId = relayInstanceId;
            return Flux.fromIterable(events);
        }
    }

    private static final class FakePublisher implements OutboxEventPublisher {

        private final String failingEventId;
        private final List<String> published = new ArrayList<>();

        private FakePublisher() {
            this(null);
        }

        private FakePublisher(String failingEventId) {
            this.failingEventId = failingEventId;
        }

        @Override
        public Mono<Void> publish(OutboxEvent event) {
            if (event.eventId().equals(failingEventId)) {
                return Mono.error(new IllegalStateException("send failed"));
            }

            published.add(event.eventId());
            return Mono.empty();
        }
    }

    private static final class FakeStatusUpdater implements OutboxRelayStatusUpdater {

        private final List<String> published = new ArrayList<>();
        private final List<String> failed = new ArrayList<>();
        private final List<String> failureMessages = new ArrayList<>();

        @Override
        public Mono<Void> markPublished(String eventId) {
            published.add(eventId);
            return Mono.empty();
        }

        @Override
        public Mono<Void> markFailure(
                String eventId,
                OutboxProducerRetryDecision decision,
                String errorMessage
        ) {
            failed.add(eventId);
            failureMessages.add(errorMessage);
            return Mono.empty();
        }
    }
}
