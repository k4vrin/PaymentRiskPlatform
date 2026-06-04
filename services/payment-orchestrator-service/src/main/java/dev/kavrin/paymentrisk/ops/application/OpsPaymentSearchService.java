package dev.kavrin.paymentrisk.ops.application;

import reactor.core.publisher.Mono;

public interface OpsPaymentSearchService {

    Mono<OpsPaymentSearchResult> search(OpsPaymentSearchRequest request);
}
