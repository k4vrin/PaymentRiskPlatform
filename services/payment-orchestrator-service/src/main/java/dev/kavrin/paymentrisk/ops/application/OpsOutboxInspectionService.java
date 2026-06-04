package dev.kavrin.paymentrisk.ops.application;

import reactor.core.publisher.Mono;

public interface OpsOutboxInspectionService {

    Mono<OpsOutboxInspectionResult> inspect(OpsOutboxInspectionRequest request);
}
