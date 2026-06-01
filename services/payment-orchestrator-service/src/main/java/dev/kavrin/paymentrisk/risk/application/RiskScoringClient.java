package dev.kavrin.paymentrisk.risk.application;

import dev.kavrin.paymentrisk.risk.application.dto.RiskScoringRequest;
import dev.kavrin.paymentrisk.risk.application.dto.RiskScoringResponse;
import reactor.core.publisher.Mono;

public interface RiskScoringClient {

    Mono<RiskScoringResponse> score(RiskScoringRequest request);
}