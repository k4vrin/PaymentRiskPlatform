package dev.kavrin.paymentrisk.ops.infrastructure.deadletter;

import dev.kavrin.paymentrisk.ops.application.deadletter.OpsDeadLetterInspectionPort;
import dev.kavrin.paymentrisk.ops.application.deadletter.OpsDeadLetterInspectionRequest;
import dev.kavrin.paymentrisk.ops.application.deadletter.OpsDeadLetterItem;
import dev.kavrin.paymentrisk.ops.application.deadletter.OpsDeadLetterResult;
import dev.kavrin.paymentrisk.ops.application.deadletter.ReplayEligibility;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class DatabaseOpsDeadLetterInspectionAdapter implements OpsDeadLetterInspectionPort {

    private final DatabaseClient databaseClient;

    @Override
    public Mono<OpsDeadLetterResult> inspect(OpsDeadLetterInspectionRequest request) {
        var filterHash = filterHash(request);
        var sql = new StringBuilder("""
                SELECT
                    dead_letter_id,
                    source_system,
                    destination_name,
                    kafka_partition,
                    kafka_offset,
                    delivery_tag,
                    event_id,
                    message_id,
                    status,
                    failure_reason,
                    failed_at,
                    replay_eligible,
                    correlation_id
                FROM dead_letter_records
                WHERE 1 = 1
                """);
        var bindings = new ArrayList<Binding>();

        request.sourceSystem().ifPresent(value -> {
            sql.append(" AND source_system = :sourceSystem");
            bindings.add(new Binding("sourceSystem", value));
        });

        request.status().ifPresent(value -> {
            sql.append(" AND status = :status");
            bindings.add(new Binding("status", value));
        });

        request.destinationName().ifPresent(value -> {
            sql.append(" AND destination_name = :destinationName");
            bindings.add(new Binding("destinationName", value));
        });

        request.eventId().ifPresent(value -> {
            sql.append(" AND event_id = :eventId");
            bindings.add(new Binding("eventId", value));
        });

        request.messageId().ifPresent(value -> {
            sql.append(" AND message_id = :messageId");
            bindings.add(new Binding("messageId", value));
        });

        request.failedFrom().ifPresent(value -> {
            sql.append(" AND failed_at >= :failedFrom");
            bindings.add(new Binding("failedFrom", value));
        });

        request.failedTo().ifPresent(value -> {
            sql.append(" AND failed_at <= :failedTo");
            bindings.add(new Binding("failedTo", value));
        });

        request.pageToken().map(DatabaseOpsDeadLetterInspectionAdapter::decodeCursor)
                .ifPresent(cursor -> {
                    if (!filterHash.equals(cursor.filterHash())) {
                        throw new IllegalArgumentException("pageToken does not match current filters");
                    }

                    sql.append("""
                             AND (
                                failed_at < :cursorFailedAt
                                OR (failed_at = :cursorFailedAt AND dead_letter_id < :cursorDeadLetterId)
                            )
                            """);
                    bindings.add(new Binding("cursorFailedAt", cursor.failedAt()));
                    bindings.add(new Binding("cursorDeadLetterId", cursor.deadLetterId()));
                });

        sql.append(" ORDER BY failed_at DESC, dead_letter_id DESC");
        sql.append(" LIMIT :limit");

        var executeSpec = databaseClient.sql(sql.toString());

        for (var binding : bindings) {
            executeSpec = executeSpec.bind(binding.name(), binding.value());
        }

        executeSpec = executeSpec.bind("limit", request.pageSize() + 1);

        return executeSpec
                .map((row, metadata) -> new OpsDeadLetterItem(
                        row.get("dead_letter_id", String.class),
                        row.get("source_system", String.class),
                        row.get("destination_name", String.class),
                        row.get("status", String.class),
                        Optional.ofNullable(row.get("kafka_partition", Integer.class)),
                        Optional.ofNullable(row.get("kafka_offset", Long.class)),
                        Optional.ofNullable(row.get("delivery_tag", String.class)),
                        Optional.ofNullable(row.get("event_id", String.class)),
                        Optional.ofNullable(row.get("message_id", String.class)),
                        row.get("failure_reason", String.class),
                        row.get("failed_at", Instant.class),
                        replayEligibility(row.get("replay_eligible", Boolean.class)),
                        Optional.ofNullable(row.get("correlation_id", String.class))
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

                    return new OpsDeadLetterResult(pageItems, nextPageToken);
                });
    }

    private static ReplayEligibility replayEligibility(Boolean replayEligible) {
        return Boolean.TRUE.equals(replayEligible)
                ? ReplayEligibility.ELIGIBLE
                : ReplayEligibility.NOT_ELIGIBLE;
    }

    private static String encodeCursor(String filterHash, OpsDeadLetterItem item) {
        var rawCursor = filterHash + "|" + item.failedAt() + "|" + item.deadLetterId();

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

            if (first < 0 || second < 0) {
                throw new IllegalArgumentException("pageToken is invalid");
            }

            return new Cursor(
                    decoded.substring(0, first),
                    Instant.parse(decoded.substring(first + 1, second)),
                    decoded.substring(second + 1)
            );
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("pageToken is invalid", exception);
        }
    }

    private static String filterHash(OpsDeadLetterInspectionRequest request) {
        var rawFilters = String.join("|",
                request.sourceSystem().orElse(""),
                request.status().orElse(""),
                request.destinationName().orElse(""),
                request.eventId().orElse(""),
                request.messageId().orElse(""),
                request.failedFrom().map(Instant::toString).orElse(""),
                request.failedTo().map(Instant::toString).orElse("")
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

    private record Cursor(String filterHash, Instant failedAt, String deadLetterId) {
    }
}
