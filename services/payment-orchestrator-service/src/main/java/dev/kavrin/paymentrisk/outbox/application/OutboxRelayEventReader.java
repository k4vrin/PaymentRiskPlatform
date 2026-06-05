package dev.kavrin.paymentrisk.outbox.application;

import dev.kavrin.paymentrisk.outbox.domain.OutboxEvent;
import reactor.core.publisher.Flux;

public interface OutboxRelayEventReader {

    Flux<OutboxEvent> findRelayCandidates(OutboxRelayQuery query);
}