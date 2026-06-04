package dev.kavrin.paymentrisk.ops.api.deadletter;

import dev.kavrin.paymentrisk.ops.api.OpsApiPaths;
import dev.kavrin.paymentrisk.ops.api.OpsFilterParameters;
import dev.kavrin.paymentrisk.ops.api.deadletter.dto.OpsDeadLetterInspectionResponse;
import dev.kavrin.paymentrisk.ops.application.deadletter.OpsDeadLetterInspectionRequest;
import dev.kavrin.paymentrisk.ops.application.deadletter.OpsDeadLetterInspectionService;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping(OpsApiPaths.OPS_API_V1 + "/dead-letters")
public class OpsDeadLetterController {

    private final OpsDeadLetterInspectionService deadLetterInspectionService;
    private final OpsDeadLetterInspectionResponseMapper responseMapper;

    @GetMapping
    public Mono<OpsDeadLetterInspectionResponse> inspectDeadLetters(
            @RequestParam(required = false) String sourceSystem,
            @RequestParam(name = OpsFilterParameters.STATUS, required = false) String status,
            @RequestParam(required = false) String destinationName,
            @RequestParam(required = false) String eventId,
            @RequestParam(required = false) String messageId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant failedFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant failedTo,
            @RequestParam(name = OpsFilterParameters.SIZE, required = false) Integer size,
            @RequestParam(name = OpsFilterParameters.PAGE_TOKEN, required = false) String pageToken
    ) {
        var request = new OpsDeadLetterInspectionRequest(
                Optional.ofNullable(sourceSystem),
                Optional.ofNullable(status),
                Optional.ofNullable(destinationName),
                Optional.ofNullable(eventId),
                Optional.ofNullable(messageId),
                Optional.ofNullable(failedFrom),
                Optional.ofNullable(failedTo),
                size == null ? OpsDeadLetterInspectionRequest.DEFAULT_PAGE_SIZE : size,
                Optional.ofNullable(pageToken)
        );

        return deadLetterInspectionService.inspect(request)
                .map(responseMapper::toResponse);
    }
}
