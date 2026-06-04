package dev.kavrin.paymentrisk.ops.application.replay;

import reactor.core.publisher.Mono;

public interface ReplayAuditPort {

    Mono<Void> recordReplayRequested(ReplayJobResult replayJob);
}
