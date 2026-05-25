package dev.kavrin.paymentrisk.shared.api.error;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class TestErrorController {

    @PostMapping("/test/validate")
    void validate(@Valid @RequestBody TestRequest request) {
    }

    @GetMapping("/test/not-found")
    void notFound() {
        throw new ResourceNotFoundException("Resource was not found.");
    }

    @GetMapping("/test/conflict")
    void conflict() {
        throw new ConflictException(
                ApiErrorCode.Business.PAYMENT_STATE_CONFLICT,
                "Payment state does not allow this operation."
        );
    }

    @GetMapping("/test/timeout")
    void timeout() {
        throw new DownstreamTimeoutException("Risk service timed out.");
    }

    @GetMapping("/test/invalid-request")
    void invalidRequest() {
        throw new IllegalArgumentException("idempotencyKey contains unsupported characters.");
    }

    @GetMapping("/test/unexpected")
    void unexpected() {
        throw new IllegalStateException("boom");
    }

    record TestRequest(@NotBlank String name) {
    }
}
