package dev.kavrin.paymentrisk.ops.application.payment;

import reactor.core.publisher.Mono;

public interface OpsPaymentSearchService {

    Mono<OpsPaymentSearchResult> search(OpsPaymentSearchRequest request);
}
