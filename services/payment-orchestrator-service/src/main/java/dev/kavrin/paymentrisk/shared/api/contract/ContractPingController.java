package dev.kavrin.paymentrisk.shared.api.contract;

import dev.kavrin.paymentrisk.shared.api.correlation.CorrelationIds;
import dev.kavrin.paymentrisk.shared.api.version.ApiPaths;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

@RestController
@RequestMapping(ApiPaths.API_V1 + "/contract")
class ContractPingController {

    private static final String SERVICE_NAME = "payment-orchestrator-service";

    @GetMapping("/ping")
    ContractPingResponse ping(ServerWebExchange exchange) {
        String correlationId = exchange.getAttribute(CorrelationIds.ATTRIBUTE_NAME);
        return new ContractPingResponse(
                SERVICE_NAME,
                "v1",
                correlationId
        );
    }
}

record ContractPingResponse(
        String serviceName,
        String apiVersion,
        String correlationId
) {
}
