package dev.kavrin.paymentrisk.ops.api.replay;

import dev.kavrin.paymentrisk.ops.api.OpsApiPaths;
import dev.kavrin.paymentrisk.ops.api.replay.dto.ReplayJobResponse;
import dev.kavrin.paymentrisk.ops.api.replay.dto.ReplayRequest;
import dev.kavrin.paymentrisk.ops.application.replay.ReplayJobResult;
import dev.kavrin.paymentrisk.ops.application.replay.ReplayRequestCommand;
import dev.kavrin.paymentrisk.ops.application.replay.ReplayRequestService;
import dev.kavrin.paymentrisk.ops.domain.ReplaySource;
import dev.kavrin.paymentrisk.shared.api.correlation.CorrelationIds;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping(OpsApiPaths.OPS_API_V1 + "/replay")
public class OpsReplayController {

    private final ReplayRequestService replayRequestService;

    @PostMapping("/{source}/{targetId}")
    public Mono<ReplayJobResponse> requestReplay(
            @PathVariable ReplaySource source,
            @PathVariable String targetId,
            @Valid @RequestBody(required = false) Mono<ReplayRequest> request,
            Authentication authentication,
            ServerWebExchange exchange
    ) {
        var requestedBy = authentication == null ? "anonymous" : authentication.getName();
        var correlationId = exchange.getAttributeOrDefault(CorrelationIds.ATTRIBUTE_NAME, "");

        return request.defaultIfEmpty(new ReplayRequest(null))
                .map(body -> new ReplayRequestCommand(
                        source,
                        targetId,
                        requestedBy,
                        Optional.ofNullable(body.reason()),
                        correlationId
                ))
                .flatMap(replayRequestService::requestReplay)
                .map(this::toResponse);
    }

    private ReplayJobResponse toResponse(ReplayJobResult result) {
        return new ReplayJobResponse(
                result.replayJobId(),
                result.targetId(),
                result.source(),
                result.requestedBy(),
                result.requestedAt(),
                result.status(),
                result.reason().orElse(null),
                result.failureReason().orElse(null),
                result.correlationId()
        );
    }
}
