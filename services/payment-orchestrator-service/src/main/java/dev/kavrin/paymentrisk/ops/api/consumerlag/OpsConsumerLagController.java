package dev.kavrin.paymentrisk.ops.api.consumerlag;

import dev.kavrin.paymentrisk.ops.api.OpsApiPaths;
import dev.kavrin.paymentrisk.ops.api.consumerlag.dto.ConsumerLagItemResponse;
import dev.kavrin.paymentrisk.ops.api.consumerlag.dto.ConsumerLagResponse;
import dev.kavrin.paymentrisk.ops.application.consumerlag.ConsumerLagRequest;
import dev.kavrin.paymentrisk.ops.application.consumerlag.ConsumerLagResult;
import dev.kavrin.paymentrisk.ops.application.consumerlag.ConsumerLagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping(OpsApiPaths.OPS_API_V1 + "/consumer-lag")
public class OpsConsumerLagController {

    private final ConsumerLagService consumerLagService;

    @GetMapping
    public Mono<ConsumerLagResponse> inspectConsumerLag(
            @RequestParam(required = false) String consumerGroup,
            @RequestParam(required = false) String topic
    ) {
        return consumerLagService.inspect(new ConsumerLagRequest(
                        Optional.ofNullable(consumerGroup),
                        Optional.ofNullable(topic)
                ))
                .map(this::toResponse);
    }

    private ConsumerLagResponse toResponse(ConsumerLagResult result) {
        var items = result.items().stream()
                .map(item -> new ConsumerLagItemResponse(
                        item.consumerGroup(),
                        item.topic(),
                        item.partition(),
                        item.currentOffset(),
                        item.endOffset(),
                        item.lag(),
                        item.lastObservedAt(),
                        item.status()
                ))
                .toList();

        return new ConsumerLagResponse(
                items,
                result.unavailableReason().orElse(null)
        );
    }
}
