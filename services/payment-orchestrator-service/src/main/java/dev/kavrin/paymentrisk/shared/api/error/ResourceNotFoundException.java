package dev.kavrin.paymentrisk.shared.api.error;

import org.springframework.http.HttpStatus;

public final class ResourceNotFoundException extends PaymentRiskApiException {

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, ApiErrorCode.Business.RESOURCE_NOT_FOUND, message);
    }

    public ResourceNotFoundException(ApiErrorCode.Business code, String message) {
        super(HttpStatus.NOT_FOUND, code, message);
    }
}
