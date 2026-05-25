package dev.kavrin.paymentrisk.payment.domain.model;

public record DeviceFingerprint(String value) {

    public DeviceFingerprint {
        value = RequiredText.require(value, "deviceFingerprint", 256);
    }

    public static DeviceFingerprint of(String value) {
        return new DeviceFingerprint(value);
    }

    public String masked() {
        if (value.length() <= 10) {
            return "********";
        }
        return value.substring(0, 6) + "..." + value.substring(value.length() - 4);
    }
}
