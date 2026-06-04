package dev.kavrin.paymentrisk.ops.application.deadletter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class DefaultOpsDeadLetterInspectionService implements OpsDeadLetterInspectionService {

    private final OpsDeadLetterInspectionPort deadLetterInspectionPort;

    @Override
    public Mono<OpsDeadLetterResult> inspect(OpsDeadLetterInspectionRequest request) {
        return deadLetterInspectionPort.inspect(request);
    }
}
