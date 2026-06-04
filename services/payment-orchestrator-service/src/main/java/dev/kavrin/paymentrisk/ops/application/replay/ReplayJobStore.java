package dev.kavrin.paymentrisk.ops.application.replay;

import reactor.core.publisher.Mono;

public interface ReplayJobStore {

    Mono<Boolean> hasActiveReplay(String source, String targetId);

    Mono<ReplayJobResult> saveRequested(ReplayRequestCommand command, String replayJobId);
}
