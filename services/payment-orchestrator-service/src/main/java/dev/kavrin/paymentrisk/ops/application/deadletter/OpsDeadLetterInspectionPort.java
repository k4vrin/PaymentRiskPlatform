package dev.kavrin.paymentrisk.ops.application.deadletter;

import reactor.core.publisher.Mono;

public interface OpsDeadLetterInspectionPort {

    Mono<OpsDeadLetterResult> inspect(OpsDeadLetterInspectionRequest request);
}
