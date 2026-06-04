package dev.kavrin.paymentrisk.ops.api;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OpsFilterParameters {
    public static final String AGGREGATE_ID = "aggregateId";
    public static final String CREATED_FROM = "createdFrom";
    public static final String CREATED_TO = "createdTo";
    public static final String CUSTOMER_ID = "customerId";
    public static final String EVENT_TYPE = "eventType";
    public static final String MERCHANT_ID = "merchantId";
    public static final String PAGE_TOKEN = "pageToken";
    public static final String PAYMENT_ID = "paymentId";
    public static final String SIZE = "size";
    public static final String SORT_BY = "sortBy";
    public static final String SORT_DIRECTION = "sortDirection";
    public static final String STATUS = "status";
}
