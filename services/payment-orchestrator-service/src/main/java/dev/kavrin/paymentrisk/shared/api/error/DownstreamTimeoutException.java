package dev.kavrin.paymentrisk.shared.api.error;

import org.springframework.http.HttpStatus;

public final class DownstreamTimeoutException extends PaymentRiskApiException {

    public DownstreamTimeoutException(String message) {
        super(HttpStatus.GATEWAY_TIMEOUT, ApiErrorCode.Infrastructure.RISK_SERVICE_TIMEOUT, message);
    }
}
