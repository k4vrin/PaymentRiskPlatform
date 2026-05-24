package dev.kavrin.paymentrisk.shared.api.error;

import dev.kavrin.paymentrisk.shared.api.correlation.CorrelationIds;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

import java.util.List;

@RestControllerAdvice
public class GlobalApiExceptionHandler {

    @ExceptionHandler(WebExchangeBindException.class)
    ResponseEntity<ApiErrorResponse> handleWebExchangeBindException(
            WebExchangeBindException exception,
            ServerWebExchange exchange
    ) {
        List<ApiErrorResponse.FieldError> fieldErrors = exception.getFieldErrors()
                .stream()
                .map(fieldError -> new ApiErrorResponse.FieldError(
                        fieldError.getField(),
                        fieldError.getDefaultMessage()
                ))
                .toList();

        return error(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.Validation.VALIDATION_FAILED,
                "Request validation failed.",
                exchange,
                fieldErrors
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiErrorResponse> handleConstraintViolationException(
            ConstraintViolationException exception,
            ServerWebExchange exchange
    ) {
        List<ApiErrorResponse.FieldError> fieldErrors = exception.getConstraintViolations()
                .stream()
                .map(violation -> new ApiErrorResponse.FieldError(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()
                ))
                .toList();

        return error(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.Validation.VALIDATION_FAILED,
                "Request validation failed.",
                exchange,
                fieldErrors
        );
    }

    @ExceptionHandler(ServerWebInputException.class)
    ResponseEntity<ApiErrorResponse> handleServerWebInputException(
            ServerWebInputException exception,
            ServerWebExchange exchange
    ) {
        return error(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.Validation.MALFORMED_REQUEST,
                "Request body or parameters are malformed.",
                exchange
        );
    }

    @ExceptionHandler(PaymentRiskApiException.class)
    ResponseEntity<ApiErrorResponse> handlePaymentRiskApiException(
            PaymentRiskApiException exception,
            ServerWebExchange exchange
    ) {
        return error(exception.status(), exception.code(), exception.getMessage(), exchange);
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiErrorResponse> handleResponseStatusException(
            ResponseStatusException exception,
            ServerWebExchange exchange
    ) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        ApiErrorCode code = switch (status) {
            case NOT_FOUND -> ApiErrorCode.Business.RESOURCE_NOT_FOUND;
            case UNAUTHORIZED -> ApiErrorCode.Security.UNAUTHORIZED;
            case FORBIDDEN -> ApiErrorCode.Security.FORBIDDEN;
            case BAD_REQUEST -> ApiErrorCode.Validation.INVALID_REQUEST;
            case CONFLICT -> ApiErrorCode.Business.PAYMENT_STATE_CONFLICT;
            default -> ApiErrorCode.Infrastructure.INTERNAL_ERROR;
        };

        return error(status, code, status.getReasonPhrase(), exchange);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiErrorResponse> handleAuthenticationException(
            AuthenticationException exception,
            ServerWebExchange exchange
    ) {
        return error(
                HttpStatus.UNAUTHORIZED,
                ApiErrorCode.Security.AUTHENTICATION_REQUIRED,
                "Authentication is required.",
                exchange
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiErrorResponse> handleAccessDeniedException(
            AccessDeniedException exception,
            ServerWebExchange exchange
    ) {
        return error(
                HttpStatus.FORBIDDEN,
                ApiErrorCode.Security.ACCESS_DENIED,
                "Access is denied.",
                exchange
        );
    }

    @ExceptionHandler(Throwable.class)
    ResponseEntity<ApiErrorResponse> handleThrowable(Throwable exception, ServerWebExchange exchange) {
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ApiErrorCode.Infrastructure.INTERNAL_ERROR,
                "An unexpected error occurred.",
                exchange
        );
    }

    private ResponseEntity<ApiErrorResponse> error(
            HttpStatus status,
            ApiErrorCode code,
            String message,
            ServerWebExchange exchange
    ) {
        return error(status, code, message, exchange, List.of());
    }

    private ResponseEntity<ApiErrorResponse> error(
            HttpStatus status,
            ApiErrorCode code,
            String message,
            ServerWebExchange exchange,
            List<ApiErrorResponse.FieldError> fieldErrors
    ) {
        String correlationId = exchange.getAttribute(CorrelationIds.ATTRIBUTE_NAME);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = exchange.getRequest().getHeaders()
                    .getFirst(CorrelationIds.HEADER_NAME);
        }

        ApiErrorResponse response = ApiErrorResponse.of(
                status.value(),
                code,
                message,
                exchange.getRequest().getPath().value(),
                correlationId,
                fieldErrors
        );

        return ResponseEntity.status(status).body(response);
    }
}
