package dev.kavrin.paymentrisk.ops.api.payment;

import dev.kavrin.paymentrisk.ops.api.dto.OpsPageResponse;
import dev.kavrin.paymentrisk.ops.api.payment.dto.OpsPaymentSearchItemResponse;
import dev.kavrin.paymentrisk.ops.api.payment.dto.OpsPaymentSearchResponse;
import dev.kavrin.paymentrisk.ops.application.payment.OpsPaymentSearchItem;
import dev.kavrin.paymentrisk.ops.application.payment.OpsPaymentSearchResult;
import org.springframework.stereotype.Component;

@Component
public class OpsPaymentSearchResponseMapper {

    public OpsPaymentSearchResponse toResponse(OpsPaymentSearchResult result) {
        var nextPageToken = result.nextPageToken().orElse(null);
        var items = result.items().stream()
                .map(this::toItemResponse)
                .toList();

        return new OpsPaymentSearchResponse(
                items,
                new OpsPageResponse.PageMetadata(
                        items.size(),
                        nextPageToken,
                        nextPageToken != null
                )
        );
    }

    private OpsPaymentSearchItemResponse toItemResponse(OpsPaymentSearchItem item) {
        return new OpsPaymentSearchItemResponse(
                item.paymentId(),
                item.merchantId(),
                item.customerId(),
                item.amountMinor(),
                item.currency(),
                item.status().name(),
                item.externalReference().orElse(null),
                item.authorization()
                        .map(this::toAuthorizationResponse)
                        .orElse(null),
                item.risk()
                        .map(this::toRiskResponse)
                        .orElse(null),
                item.reversal()
                        .map(this::toReversalResponse)
                        .orElse(null),
                item.createdAt(),
                item.updatedAt()
        );
    }

    private OpsPaymentSearchItemResponse.AuthorizationSummaryResponse toAuthorizationResponse(
            OpsPaymentSearchItem.AuthorizationSummary summary
    ) {
        return new OpsPaymentSearchItemResponse.AuthorizationSummaryResponse(
                summary.authorizationStatus(),
                summary.authorizationCode().orElse(null),
                summary.authorizedAt().orElse(null)
        );
    }

    private OpsPaymentSearchItemResponse.RiskSummaryResponse toRiskResponse(
            OpsPaymentSearchItem.RiskSummary summary
    ) {
        return new OpsPaymentSearchItemResponse.RiskSummaryResponse(
                summary.decision(),
                summary.score(),
                summary.ruleVersion(),
                summary.decidedAt()
        );
    }

    private OpsPaymentSearchItemResponse.ReversalSummaryResponse toReversalResponse(
            OpsPaymentSearchItem.ReversalSummary summary
    ) {
        return new OpsPaymentSearchItemResponse.ReversalSummaryResponse(
                summary.reversalId(),
                summary.status(),
                summary.reason(),
                summary.reversedAt()
        );
    }
}
