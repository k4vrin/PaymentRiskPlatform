package dev.kavrin.paymentrisk.shared.api.error;

import org.springframework.http.HttpStatus;

public class PaymentRiskApiException extends RuntimeException {

    private final HttpStatus status;
    private final ApiErrorCode code;

    public PaymentRiskApiException(HttpStatus status, ApiErrorCode code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() {
        return status;
    }

    public ApiErrorCode code() {
        return code;
    }
}
