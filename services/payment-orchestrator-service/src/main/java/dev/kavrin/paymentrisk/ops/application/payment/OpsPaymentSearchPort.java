package dev.kavrin.paymentrisk.ops.application.payment;

import reactor.core.publisher.Mono;

public interface OpsPaymentSearchPort {

    Mono<OpsPaymentSearchResult> search(OpsPaymentSearchRequest request);
}