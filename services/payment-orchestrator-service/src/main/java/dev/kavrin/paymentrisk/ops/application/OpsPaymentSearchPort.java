package dev.kavrin.paymentrisk.ops.application;

import reactor.core.publisher.Mono;

public interface OpsPaymentSearchPort {

    Mono<OpsPaymentSearchResult> search(OpsPaymentSearchRequest request);
}