package dev.kavrin.paymentrisk.payment.api.contract;

import dev.kavrin.paymentrisk.payment.api.dto.AuthorizationRequest;
import dev.kavrin.paymentrisk.payment.api.dto.AuthorizationResponse;
import dev.kavrin.paymentrisk.payment.application.service.AuthorizePaymentService;
import dev.kavrin.paymentrisk.shared.api.correlation.CorrelationIds;
import dev.kavrin.paymentrisk.shared.api.version.ApiPaths;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPaths.API_V1 + "/payments")
public class PaymentAuthorizationController {

    private final AuthorizePaymentService authorizePaymentService;

    @PostMapping("/authorize")
    public Mono<AuthorizationResponse> authorize(
            @Valid @RequestBody AuthorizationRequest request,
            ServerWebExchange exchange
    ) {
        String correlationId = exchange.getAttributeOrDefault(CorrelationIds.ATTRIBUTE_NAME, "");

        return authorizePaymentService.authorize(
                AuthorizationRequestMapper.toCommand(request, correlationId)
        ).map(AuthorizationResponseMapper::toResponse);
    }
}
