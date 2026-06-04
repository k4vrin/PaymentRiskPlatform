package dev.kavrin.paymentrisk.ops.infrastructure;

import dev.kavrin.paymentrisk.ops.application.OpsPaymentSearchItem;
import dev.kavrin.paymentrisk.ops.application.OpsPaymentSearchPort;
import dev.kavrin.paymentrisk.ops.application.OpsPaymentSearchRequest;
import dev.kavrin.paymentrisk.ops.application.OpsPaymentSearchResult;
import dev.kavrin.paymentrisk.payment.domain.model.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Database-backed implementation of payment search for operator workflows.
 * <p>
 * Uses DatabaseClient instead of Spring Data repositories because this is a
 * read-model query: it combines optional filters, left joins, dynamic SQL, and
 * keyset pagination in one database round trip.
 * <p>
 * The adapter returns a flattened investigation view, not a Payment aggregate.
 * Operator screens need searchable summaries and must not expose raw payment
 * method tokens, device fingerprints, or their stored hashes.
 */
@Component
@RequiredArgsConstructor
public class DatabaseOpsPaymentSearchAdapter implements OpsPaymentSearchPort {

    private final DatabaseClient databaseClient;

    @Override
    public Mono<OpsPaymentSearchResult> search(OpsPaymentSearchRequest request) {
        var filterHash = filterHash(request);

        // Build one query for the ops read model. LEFT JOINs keep early-life
        // payments visible even before risk decisions or reversals exist.
        var sql = new StringBuilder("""
                SELECT
                    p.payment_id,
                    p.merchant_id,
                    p.customer_id,
                    p.amount_minor,
                    p.currency,
                    p.status,
                    p.external_reference,
                    p.created_at,
                    p.updated_at,
                    a.status AS authorization_status,
                    a.authorization_code,
                    a.authorized_at,
                    r.decision AS risk_decision,
                    r.score AS risk_score,
                    r.rule_version,
                    r.decided_at,
                    pr.payment_reversal_id,
                    pr.status AS reversal_status,
                    pr.reason AS reversal_reason,
                    pr.reversed_at
                FROM payments p
                LEFT JOIN payment_authorizations a ON a.payment_id = p.payment_id
                LEFT JOIN payment_risk_decisions r ON r.payment_id = p.payment_id
                LEFT JOIN payment_reversals pr ON pr.payment_id = p.payment_id
                WHERE 1 = 1
                """);

        // Add filters only when present. Values are always bound parameters, so
        // request input never becomes executable SQL.
        var bindings = new ArrayList<Binding>();

        request.status().ifPresent(value -> {
            sql.append(" AND p.status = :status");
            bindings.add(new Binding("status", value.name()));
        });

        request.merchantId().ifPresent(value -> {
            sql.append(" AND p.merchant_id = :merchantId");
            bindings.add(new Binding("merchantId", value.value()));
        });

        request.customerId().ifPresent(value -> {
            sql.append(" AND p.customer_id = :customerId");
            bindings.add(new Binding("customerId", value.value()));
        });

        request.paymentId().ifPresent(value -> {
            sql.append(" AND p.payment_id = :paymentId");
            bindings.add(new Binding("paymentId", value.value()));
        });

        request.createdFrom().ifPresent(value -> {
            sql.append(" AND p.created_at >= :createdFrom");
            bindings.add(new Binding("createdFrom", value));
        });

        request.createdTo().ifPresent(value -> {
            sql.append(" AND p.created_at <= :createdTo");
            bindings.add(new Binding("createdTo", value));
        });

        request.pageToken().map(DatabaseOpsPaymentSearchAdapter::decodeCursor)
                .ifPresent(cursor -> {
                    if (!filterHash.equals(cursor.filterHash())) {
                        throw new IllegalArgumentException("pageToken does not match current filters");
                    }

                    // Keyset pagination continues after the last row from the
                    // previous page. The ORDER BY is:
                    //
                    //   p.created_at DESC, p.payment_id DESC
                    //
                    // So the next page must contain rows with an older
                    // created_at, or rows at the same created_at with a smaller
                    // payment_id. The payment_id tie-breaker makes the ordering
                    // total and stable when multiple payments share a timestamp.
                    sql.append("""
                             AND (
                                p.created_at < :cursorCreatedAt
                                OR (p.created_at = :cursorCreatedAt AND p.payment_id < :cursorPaymentId)
                            )
                            """);
                    bindings.add(new Binding("cursorCreatedAt", cursor.createdAt()));
                    bindings.add(new Binding("cursorPaymentId", cursor.paymentId()));
                });

        sql.append(" ORDER BY p.created_at DESC, p.payment_id DESC");
        sql.append(" LIMIT :limit");

        // Fetch one extra row to know whether there is another page without a
        // separate COUNT query. If pageSize is 50 and the database returns 51
        // rows, the API returns 50 items and emits a nextPageToken.
        var limit = request.pageSize() + 1;

        // Convert rows directly into the operator-facing read model. This
        // adapter does not rehydrate domain objects because no payment business
        // decision is made on this path.
        var executeSpec = databaseClient.sql(sql.toString());

        for (var binding : bindings) {
            executeSpec = executeSpec.bind(binding.name(), binding.value());
        }

        executeSpec = executeSpec.bind("limit", limit);

        return executeSpec
                .map((row, metadata) -> new OpsPaymentSearchItem(
                        row.get("payment_id", String.class),
                        row.get("merchant_id", String.class),
                        row.get("customer_id", String.class),
                        row.get("amount_minor", Long.class),
                        row.get("currency", String.class),
                        PaymentStatus.valueOf(row.get("status", String.class)),
                        Optional.ofNullable(row.get("external_reference", String.class)),
                        authorizationSummary(row),
                        riskSummary(row),
                        reversalSummary(row),
                        row.get("created_at", Instant.class),
                        row.get("updated_at", Instant.class)
                ))
                .all()
                .collectList()
                .map(items -> {
                    var hasNext = items.size() > request.pageSize();

                    var pageItems = hasNext
                            ? items.subList(0, request.pageSize())
                            : items;

                    // The token stores a filter hash plus the last returned
                    // row's keyset values. The hash prevents a client from
                    // reusing a cursor from one filtered search with another
                    // filter set, which would otherwise skip or duplicate rows.
                    // Clients should treat the token as opaque.
                    var nextPageToken = hasNext
                            ? Optional.of(encodeCursor(filterHash, pageItems.getLast()))
                            : Optional.<String>empty();

                    return new OpsPaymentSearchResult(
                            pageItems,
                            nextPageToken
                    );
                });
    }

    private Optional<OpsPaymentSearchItem.AuthorizationSummary> authorizationSummary(
            io.r2dbc.spi.Row row
    ) {
        var status = row.get("authorization_status", String.class);
        if (status == null) {
            return Optional.empty();
        }

        return Optional.of(new OpsPaymentSearchItem.AuthorizationSummary(
                status,
                Optional.ofNullable(row.get("authorization_code", String.class)),
                Optional.ofNullable(row.get("authorized_at", Instant.class))
        ));
    }

    private Optional<OpsPaymentSearchItem.RiskSummary> riskSummary(
            io.r2dbc.spi.Row row
    ) {
        var decision = row.get("risk_decision", String.class);
        if (decision == null) {
            return Optional.empty();
        }

        return Optional.of(new OpsPaymentSearchItem.RiskSummary(
                decision,
                row.get("risk_score", Integer.class),
                row.get("rule_version", String.class),
                row.get("decided_at", Instant.class)
        ));
    }

    private Optional<OpsPaymentSearchItem.ReversalSummary> reversalSummary(
            io.r2dbc.spi.Row row
    ) {
        var reversalId = row.get("payment_reversal_id", String.class);
        if (reversalId == null) {
            return Optional.empty();
        }

        return Optional.of(new OpsPaymentSearchItem.ReversalSummary(
                reversalId,
                row.get("reversal_status", String.class),
                row.get("reversal_reason", String.class),
                row.get("reversed_at", Instant.class)
        ));
    }

    private static String encodeCursor(String filterHash, OpsPaymentSearchItem item) {
        // Cursor format is deliberately URL-safe base64 so it can be passed as
        // a query parameter without leaking implementation details into the
        // public API shape.
        var rawCursor = filterHash + "|" + item.createdAt() + "|" + item.paymentId();

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(rawCursor.getBytes(StandardCharsets.UTF_8));
    }

    private static Cursor decodeCursor(String pageToken) {
        try {
            var decoded = new String(
                    Base64.getUrlDecoder().decode(pageToken),
                    StandardCharsets.UTF_8
            );
            // Use explicit separators for the internal token parts. The first
            // field is the filter hash, then the keyset values used by ORDER BY.
            var firstSeparatorIndex = decoded.indexOf('|');
            var secondSeparatorIndex = decoded.indexOf('|', firstSeparatorIndex + 1);

            if (firstSeparatorIndex < 0 || secondSeparatorIndex < 0) {
                throw new IllegalArgumentException("pageToken is invalid");
            }

            return new Cursor(
                    decoded.substring(0, firstSeparatorIndex),
                    Instant.parse(decoded.substring(firstSeparatorIndex + 1, secondSeparatorIndex)),
                    decoded.substring(secondSeparatorIndex + 1)
            );
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("pageToken is invalid", exception);
        }
    }

    private static String filterHash(OpsPaymentSearchRequest request) {
        var rawFilters = String.join("|",
                request.status().map(Enum::name).orElse(""),
                request.merchantId().map(value -> value.value()).orElse(""),
                request.customerId().map(value -> value.value()).orElse(""),
                request.paymentId().map(value -> value.value()).orElse(""),
                request.createdFrom().map(Instant::toString).orElse(""),
                request.createdTo().map(Instant::toString).orElse("")
        );

        try {
            var digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawFilters.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private record Binding(String name, Object value) {
    }

    private record Cursor(String filterHash, Instant createdAt, String paymentId) {
    }
}
