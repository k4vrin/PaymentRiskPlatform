package dev.kavrin.paymentrisk.ops.application.replay;

import dev.kavrin.paymentrisk.ops.domain.ReplaySource;
import reactor.core.publisher.Mono;

public interface ReplayTargetLookupPort {

    Mono<ReplayTarget> findTarget(ReplaySource source, String targetId);
}
