package dev.kavrin.paymentrisk.ops.infrastructure;

import dev.kavrin.paymentrisk.ops.application.OpsOutboxInspectionItem;
import dev.kavrin.paymentrisk.ops.application.OpsOutboxInspectionPort;
import dev.kavrin.paymentrisk.ops.application.OpsOutboxInspectionRequest;
import dev.kavrin.paymentrisk.ops.application.OpsOutboxInspectionResult;
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

@Component
public class DatabaseOpsOutboxInspectionAdapter implements OpsOutboxInspectionPort {

    private final DatabaseClient databaseClient;

    public DatabaseOpsOutboxInspectionAdapter(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<OpsOutboxInspectionResult> inspect(OpsOutboxInspectionRequest request) {
        var filterHash = filterHash(request);

        // DatabaseClient is used instead of a repository because this is an
        // operator search query with optional filters, custom ordering, and
        // pagination. A repository method would become awkward very quickly.
        var sql = new StringBuilder("""
                SELECT
                    event_id,
                    aggregate_id,
                    aggregate_type,
                    event_type,
                    schema_version,
                    status,
                    retry_count,
                    last_error,
                    next_retry_at,
                    created_at,
                    occurred_at,
                    published_at,
                    correlation_id,
                    payload_json
                FROM outbox_events
                WHERE 1 = 1
                """);

        // Keep SQL structure and user-provided values separate.
        // This avoids SQL injection and lets the database bind values safely.
        var bindings = new ArrayList<Binding>();

        request.status().ifPresent(value -> {
            sql.append(" AND status = :status");
            bindings.add(new Binding("status", value));
        });

        request.eventType().ifPresent(value -> {
            sql.append(" AND event_type = :eventType");
            bindings.add(new Binding("eventType", value));
        });

        request.aggregateId().ifPresent(value -> {
            sql.append(" AND aggregate_id = :aggregateId");
            bindings.add(new Binding("aggregateId", value));
        });

        request.createdFrom().ifPresent(value -> {
            sql.append(" AND created_at >= :createdFrom");
            bindings.add(new Binding("createdFrom", value));
        });

        request.createdTo().ifPresent(value -> {
            sql.append(" AND created_at <= :createdTo");
            bindings.add(new Binding("createdTo", value));
        });

        // Failed/retryable records are usually the most operationally urgent.
        // For FAILED rows, order by next retry time first so operators can see
        // what will be retried soonest. Otherwise use stable newest-first order.
        if (request.status().filter("FAILED"::equalsIgnoreCase).isPresent()) {
            request.pageToken().map(DatabaseOpsOutboxInspectionAdapter::decodeCursor)
                    .ifPresent(cursor -> {
                        if (!filterHash.equals(cursor.filterHash())) {
                            throw new IllegalArgumentException("pageToken does not match current filters");
                        }

                        sql.append("""
                                 AND (
                                    COALESCE(next_retry_at, '9999-12-31T23:59:59Z'::timestamptz) > :cursorNextRetryAt
                                    OR (
                                        COALESCE(next_retry_at, '9999-12-31T23:59:59Z'::timestamptz) = :cursorNextRetryAt
                                        AND created_at < :cursorCreatedAt
                                    )
                                    OR (
                                        COALESCE(next_retry_at, '9999-12-31T23:59:59Z'::timestamptz) = :cursorNextRetryAt
                                        AND created_at = :cursorCreatedAt
                                        AND event_id < :cursorEventId
                                    )
                                )
                                """);
                        bindings.add(new Binding("cursorNextRetryAt", cursor.nextRetryAt()));
                        bindings.add(new Binding("cursorCreatedAt", cursor.createdAt()));
                        bindings.add(new Binding("cursorEventId", cursor.eventId()));
                    });
            sql.append(" ORDER BY next_retry_at ASC NULLS LAST, created_at DESC, event_id DESC");
        } else {
            request.pageToken().map(DatabaseOpsOutboxInspectionAdapter::decodeCursor)
                    .ifPresent(cursor -> {
                        if (!filterHash.equals(cursor.filterHash())) {
                            throw new IllegalArgumentException("pageToken does not match current filters");
                        }

                        sql.append("""
                                 AND (
                                    created_at < :cursorCreatedAt
                                    OR (created_at = :cursorCreatedAt AND event_id < :cursorEventId)
                                )
                                """);
                        bindings.add(new Binding("cursorCreatedAt", cursor.createdAt()));
                        bindings.add(new Binding("cursorEventId", cursor.eventId()));
                    });
            sql.append(" ORDER BY created_at DESC, event_id DESC");
        }

        // Fetch one extra row to know whether another page exists.
        var limit = request.pageSize() + 1;
        sql.append(" LIMIT :limit");

        var executeSpec = databaseClient.sql(sql.toString());

        for (var binding : bindings) {
            executeSpec = executeSpec.bind(binding.name(), binding.value());
        }

        executeSpec = executeSpec.bind("limit", limit);

        return executeSpec
                .map((row, metadata) -> new OpsOutboxInspectionItem(
                        row.get("event_id", String.class),
                        row.get("aggregate_id", String.class),
                        row.get("aggregate_type", String.class),
                        row.get("event_type", String.class),
                        row.get("schema_version", String.class),
                        row.get("status", String.class),
                        row.get("retry_count", Integer.class),
                        Optional.ofNullable(row.get("last_error", String.class)),
                        Optional.ofNullable(row.get("next_retry_at", Instant.class)),
                        row.get("created_at", Instant.class),
                        row.get("occurred_at", Instant.class),
                        Optional.ofNullable(row.get("published_at", Instant.class)),
                        Optional.ofNullable(row.get("correlation_id", String.class)),
                        Optional.ofNullable(row.get("payload_json", String.class))
                ))
                .all()
                .collectList()
                .map(items -> {
                    var hasNext = items.size() > request.pageSize();

                    var pageItems = hasNext
                            ? items.subList(0, request.pageSize())
                            : items;

                    var nextPageToken = hasNext
                            ? Optional.of(encodeCursor(filterHash, pageItems.getLast()))
                            : Optional.<String>empty();

                    return new OpsOutboxInspectionResult(
                            pageItems,
                            nextPageToken
                    );
                });
    }

    private record Binding(String name, Object value) {
    }

    private static String encodeCursor(String filterHash, OpsOutboxInspectionItem item) {
        var nextRetryAt = item.nextRetryAt()
                .orElse(Instant.parse("9999-12-31T23:59:59Z"));
        var rawCursor = filterHash + "|" + nextRetryAt + "|" + item.createdAt() + "|" + item.eventId();

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
            var first = decoded.indexOf('|');
            var second = decoded.indexOf('|', first + 1);
            var third = decoded.indexOf('|', second + 1);

            if (first < 0 || second < 0 || third < 0) {
                throw new IllegalArgumentException("pageToken is invalid");
            }

            return new Cursor(
                    decoded.substring(0, first),
                    Instant.parse(decoded.substring(first + 1, second)),
                    Instant.parse(decoded.substring(second + 1, third)),
                    decoded.substring(third + 1)
            );
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("pageToken is invalid", exception);
        }
    }

    private static String filterHash(OpsOutboxInspectionRequest request) {
        var rawFilters = String.join("|",
                request.status().orElse(""),
                request.eventType().orElse(""),
                request.aggregateId().orElse(""),
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

    private record Cursor(
            String filterHash,
            Instant nextRetryAt,
            Instant createdAt,
            String eventId
    ) {
    }
}
