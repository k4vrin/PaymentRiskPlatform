package dev.kavrin.paymentrisk.payment.application.command;

import dev.kavrin.paymentrisk.payment.domain.model.PaymentId;
import dev.kavrin.paymentrisk.payment.domain.model.ReversalReason;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class ReversePaymentRequestFingerprint {

    public static String from(ReversePaymentCommand command) {
        var canonical = "%s|%s"
                .formatted(
                        PaymentId.of(command.paymentId()).value(),
                        ReversalReason.ofNullable(command.reason()).value()
                );

        return sha256(canonical);
    }

    private static String sha256(String value) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is not available",
                    exception
            );
        }
    }
}
