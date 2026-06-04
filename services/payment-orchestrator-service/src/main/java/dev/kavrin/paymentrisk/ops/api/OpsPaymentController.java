package dev.kavrin.paymentrisk.ops.api;

import dev.kavrin.paymentrisk.ops.api.dto.OpsPaymentSearchResponse;
import dev.kavrin.paymentrisk.ops.application.OpsPaymentSearchRequest;
import dev.kavrin.paymentrisk.ops.application.OpsPaymentSearchService;
import dev.kavrin.paymentrisk.payment.domain.model.CustomerId;
import dev.kavrin.paymentrisk.payment.domain.model.MerchantId;
import dev.kavrin.paymentrisk.payment.domain.model.PaymentId;
import dev.kavrin.paymentrisk.payment.domain.model.PaymentStatus;
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
@RequestMapping(OpsApiPaths.OPS_API_V1 + "/payments")
public class OpsPaymentController {

    private final OpsPaymentSearchService paymentSearchService;
    private final OpsPaymentSearchResponseMapper responseMapper;

    @GetMapping
    public Mono<OpsPaymentSearchResponse> searchPayments(
            @RequestParam(name = OpsFilterParameters.STATUS, required = false) PaymentStatus status,
            @RequestParam(name = OpsFilterParameters.MERCHANT_ID, required = false) String merchantId,
            @RequestParam(name = OpsFilterParameters.CUSTOMER_ID, required = false) String customerId,
            @RequestParam(name = OpsFilterParameters.PAYMENT_ID, required = false) String paymentId,
            @RequestParam(name = OpsFilterParameters.CREATED_FROM, required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant createdFrom,
            @RequestParam(name = OpsFilterParameters.CREATED_TO, required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant createdTo,
            @RequestParam(name = OpsFilterParameters.SIZE, required = false) Integer size,
            @RequestParam(name = OpsFilterParameters.PAGE_TOKEN, required = false) String pageToken
    ) {
        var request = OpsPaymentSearchRequest.firstPage(
                Optional.ofNullable(status),
                optionalMerchantId(merchantId),
                optionalCustomerId(customerId),
                optionalPaymentId(paymentId),
                Optional.ofNullable(createdFrom),
                Optional.ofNullable(createdTo),
                size
        );

        request = new OpsPaymentSearchRequest(
                request.status(),
                request.merchantId(),
                request.customerId(),
                request.paymentId(),
                request.createdFrom(),
                request.createdTo(),
                request.pageSize(),
                Optional.ofNullable(pageToken)
        );

        return paymentSearchService.search(request)
                .map(responseMapper::toResponse);
    }

    private Optional<MerchantId> optionalMerchantId(String value) {
        return Optional.ofNullable(value)
                .filter(id -> !id.isBlank())
                .map(MerchantId::of);
    }

    private Optional<CustomerId> optionalCustomerId(String value) {
        return Optional.ofNullable(value)
                .filter(id -> !id.isBlank())
                .map(CustomerId::of);
    }

    private Optional<PaymentId> optionalPaymentId(String value) {
        return Optional.ofNullable(value)
                .filter(id -> !id.isBlank())
                .map(PaymentId::of);
    }
}
