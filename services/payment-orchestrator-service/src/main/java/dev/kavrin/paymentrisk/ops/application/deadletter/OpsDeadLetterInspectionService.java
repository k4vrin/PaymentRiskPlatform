package dev.kavrin.paymentrisk.ops.application.deadletter;

import reactor.core.publisher.Mono;

public interface OpsDeadLetterInspectionService {

    Mono<OpsDeadLetterResult> inspect(OpsDeadLetterInspectionRequest request);
}
