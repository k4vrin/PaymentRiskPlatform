package dev.kavrin.paymentrisk.ops.application.outbox;

import reactor.core.publisher.Mono;

public interface OpsOutboxInspectionPort {

    Mono<OpsOutboxInspectionResult> inspect(OpsOutboxInspectionRequest request);
}