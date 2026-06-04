package dev.kavrin.paymentrisk.ops.application;

import reactor.core.publisher.Mono;

public interface OpsOutboxInspectionPort {

    Mono<OpsOutboxInspectionResult> inspect(OpsOutboxInspectionRequest request);
}