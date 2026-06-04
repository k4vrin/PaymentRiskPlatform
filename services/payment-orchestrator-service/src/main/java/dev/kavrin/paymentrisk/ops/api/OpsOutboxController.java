package dev.kavrin.paymentrisk.ops.api;

import dev.kavrin.paymentrisk.ops.api.dto.OpsOutboxInspectionResponse;
import dev.kavrin.paymentrisk.ops.application.OpsOutboxInspectionRequest;
import dev.kavrin.paymentrisk.ops.application.OpsOutboxInspectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping(OpsApiPaths.OPS_API_V1 + "/outbox")
public class OpsOutboxController {

    private final OpsOutboxInspectionService outboxInspectionService;
    private final OpsOutboxInspectionResponseMapper responseMapper;

    @GetMapping
    public Mono<OpsOutboxInspectionResponse> inspectOutbox(
            @RequestParam(name = OpsFilterParameters.STATUS, required = false) String status,
            @RequestParam(name = OpsFilterParameters.EVENT_TYPE, required = false) String eventType,
            @RequestParam(name = OpsFilterParameters.AGGREGATE_ID, required = false) String aggregateId,
            @RequestParam(name = OpsFilterParameters.CREATED_FROM, required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant createdFrom,
            @RequestParam(name = OpsFilterParameters.CREATED_TO, required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant createdTo,
            @RequestParam(name = OpsFilterParameters.SIZE, required = false) Integer size,
            @RequestParam(name = OpsFilterParameters.PAGE_TOKEN, required = false) String pageToken
    ) {
        var request = new OpsOutboxInspectionRequest(
                Optional.ofNullable(status),
                Optional.ofNullable(eventType),
                Optional.ofNullable(aggregateId),
                Optional.ofNullable(createdFrom),
                Optional.ofNullable(createdTo),
                size == null ? OpsOutboxInspectionRequest.DEFAULT_PAGE_SIZE : size,
                Optional.ofNullable(pageToken)
        );

        return outboxInspectionService.inspect(request)
                .map(responseMapper::toResponse);
    }
}
