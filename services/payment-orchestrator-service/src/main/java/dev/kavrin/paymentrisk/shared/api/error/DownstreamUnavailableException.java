package dev.kavrin.paymentrisk.shared.api.error;

import org.springframework.http.HttpStatus;

public final class DownstreamUnavailableException extends PaymentRiskApiException {

    public DownstreamUnavailableException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, ApiErrorCode.Infrastructure.DOWNSTREAM_UNAVAILABLE, message);
    }
}
