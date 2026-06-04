package dev.kavrin.paymentrisk.ops.application.outbox;

import reactor.core.publisher.Mono;

public interface OpsOutboxInspectionService {

    Mono<OpsOutboxInspectionResult> inspect(OpsOutboxInspectionRequest request);
}
