package dev.kavrin.paymentrisk.ops.application.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class DefaultOpsPaymentSearchService implements OpsPaymentSearchService {

    private final OpsPaymentSearchPort paymentSearchPort;

    @Override
    public Mono<OpsPaymentSearchResult> search(OpsPaymentSearchRequest request) {
        return paymentSearchPort.search(request);
    }
}
