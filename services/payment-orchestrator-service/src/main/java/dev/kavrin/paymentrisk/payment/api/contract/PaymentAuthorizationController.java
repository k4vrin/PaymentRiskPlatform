package dev.kavrin.paymentrisk.payment.api.contract;

import dev.kavrin.paymentrisk.payment.api.dto.AuthorizationRequest;
import dev.kavrin.paymentrisk.payment.api.dto.AuthorizationResponse;
import dev.kavrin.paymentrisk.payment.api.dto.PaymentDetailsResponse;
import dev.kavrin.paymentrisk.payment.application.query.PaymentLookupService;
import dev.kavrin.paymentrisk.payment.application.service.AuthorizePaymentService;
import dev.kavrin.paymentrisk.payment.domain.model.PaymentId;
import dev.kavrin.paymentrisk.shared.api.correlation.CorrelationIds;
import dev.kavrin.paymentrisk.shared.api.version.ApiPaths;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPaths.API_V1 + "/payments")
public class PaymentAuthorizationController {

    private final AuthorizePaymentService authorizePaymentService;
    private final PaymentLookupService paymentLookupService;

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

    @GetMapping("/{paymentId}")
    public Mono<PaymentDetailsResponse> getPayment(
            @PathVariable String paymentId
    ) {
        var domainPaymentId = PaymentId.of(paymentId);

        return paymentLookupService.getPaymentDetails(domainPaymentId)
                .map(PaymentDetailsResponseMapper::toResponse);
    }
}
