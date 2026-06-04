package dev.kavrin.paymentrisk.ops.application.replay;

import reactor.core.publisher.Mono;

public interface ReplayRequestService {

    Mono<ReplayJobResult> requestReplay(ReplayRequestCommand command);
}
