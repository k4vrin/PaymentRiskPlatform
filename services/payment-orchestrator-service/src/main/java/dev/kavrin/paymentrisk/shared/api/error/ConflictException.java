package dev.kavrin.paymentrisk.shared.api.error;

import org.springframework.http.HttpStatus;

public final class ConflictException extends PaymentRiskApiException {

    public ConflictException(ApiErrorCode.Business code, String message) {
        super(HttpStatus.CONFLICT, code, message);
    }
}
