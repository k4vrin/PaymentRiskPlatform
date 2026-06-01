package dev.kavrin.paymentrisk.payment.infrastructure.persistence;

import dev.kavrin.paymentrisk.payment.domain.model.DeviceFingerprint;
import dev.kavrin.paymentrisk.payment.domain.model.Payment;
import dev.kavrin.paymentrisk.payment.domain.model.PaymentMethodToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

@Component
public final class SensitivePaymentDataHasher {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String PAYMENT_METHOD_TOKEN_CONTEXT = "payment-method-token";
    private static final String DEVICE_FINGERPRINT_CONTEXT = "device-fingerprint";
    private static final int TOKEN_LAST_FOUR_LENGTH = 4;

    private final byte[] hashKey;

    @Autowired
    public SensitivePaymentDataHasher(
            @Value("${payment.sensitive-data.hash-key}") String hashKey
    ) {
        this(normalizedHashKey(hashKey).getBytes(StandardCharsets.UTF_8));
    }

    public SensitivePaymentDataHasher(byte[] hashKey) {
        this.hashKey = requireKey(hashKey);
    }

    public static SensitivePaymentDataHasher withUtf8Key(String hashKey) {
        return new SensitivePaymentDataHasher(hashKey);
    }

    public SensitivePaymentDataHashes hash(Payment payment) {
        Objects.requireNonNull(payment, "payment must not be null");

        return new SensitivePaymentDataHashes(
                hashPaymentMethodToken(payment.getPaymentMethodToken()),
                paymentMethodTokenLastFour(payment.getPaymentMethodToken()),
                hashDeviceFingerprint(payment.getDeviceFingerprint())
        );
    }

    public String hashPaymentMethodToken(PaymentMethodToken token) {
        Objects.requireNonNull(token, "token must not be null");
        return hmacHex(PAYMENT_METHOD_TOKEN_CONTEXT, token.value());
    }

    public String paymentMethodTokenLastFour(PaymentMethodToken token) {
        Objects.requireNonNull(token, "token must not be null");
        String value = token.value();

        if (value.length() <= TOKEN_LAST_FOUR_LENGTH) {
            return value;
        }

        return value.substring(value.length() - TOKEN_LAST_FOUR_LENGTH);
    }

    public String hashDeviceFingerprint(DeviceFingerprint fingerprint) {
        Objects.requireNonNull(fingerprint, "fingerprint must not be null");
        return hmacHex(DEVICE_FINGERPRINT_CONTEXT, fingerprint.value());
    }

    private String hmacHex(String context, String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(hashKey, HMAC_ALGORITHM));
            mac.update(context.getBytes(StandardCharsets.UTF_8));
            mac.update((byte) 0);
            byte[] hash = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(HMAC_ALGORITHM + " is not available", exception);
        } catch (InvalidKeyException exception) {
            throw new IllegalArgumentException("hashKey is not valid for " + HMAC_ALGORITHM, exception);
        }
    }

    private static byte[] requireKey(byte[] hashKey) {
        Objects.requireNonNull(hashKey, "hashKey must not be null");

        if (hashKey.length == 0) {
            throw new IllegalArgumentException("hashKey must not be empty");
        }

        return hashKey.clone();
    }

    private static String normalizedHashKey(String hashKey) {
        String normalized = Objects.requireNonNull(hashKey, "hashKey must not be null").trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("hashKey must not be blank");
        }

        return normalized;
    }

    public record SensitivePaymentDataHashes(
            String paymentMethodTokenHash,
            String paymentMethodTokenLastFour,
            String deviceFingerprintHash
    ) {

        public SensitivePaymentDataHashes {
            Objects.requireNonNull(paymentMethodTokenHash, "paymentMethodTokenHash must not be null");
            Objects.requireNonNull(paymentMethodTokenLastFour, "paymentMethodTokenLastFour must not be null");
            Objects.requireNonNull(deviceFingerprintHash, "deviceFingerprintHash must not be null");
        }
    }
}
