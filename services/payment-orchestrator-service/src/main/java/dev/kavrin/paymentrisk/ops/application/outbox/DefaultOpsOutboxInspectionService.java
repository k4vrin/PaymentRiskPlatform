package dev.kavrin.paymentrisk.ops.application.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class DefaultOpsOutboxInspectionService implements OpsOutboxInspectionService {

    private final OpsOutboxInspectionPort outboxInspectionPort;

    @Override
    public Mono<OpsOutboxInspectionResult> inspect(OpsOutboxInspectionRequest request) {
        return outboxInspectionPort.inspect(request);
    }
}
