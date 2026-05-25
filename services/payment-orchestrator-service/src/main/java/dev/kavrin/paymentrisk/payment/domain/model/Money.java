package dev.kavrin.paymentrisk.payment.domain.model;

import java.util.Currency;

public record Money(long amountMinor, Currency currency) {

    public Money {
        if (amountMinor <= 0) {
            throw new IllegalArgumentException("amountMinor must be positive.");
        }
        if (currency == null) {
            throw new IllegalArgumentException("currency is required.");
        }
    }

    public static Money of(long amountMinor, String currencyCode) {
        return new Money(amountMinor, Currency.getInstance(RequiredText.require(currencyCode, "currency", 3).toUpperCase()));
    }

    public String currencyCode() {
        return currency.getCurrencyCode();
    }
}
