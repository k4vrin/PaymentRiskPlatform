package dev.kavrin.paymentrisk.ops.api;

import static org.assertj.core.api.Assertions.assertThat;

import dev.kavrin.paymentrisk.ops.api.dto.OpsPageRequest;
import dev.kavrin.paymentrisk.ops.api.dto.OpsPageResponse;
import dev.kavrin.paymentrisk.ops.api.dto.OpsSortDirection;
import dev.kavrin.paymentrisk.ops.api.dto.OpsSortRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

class OpsApiConventionsTest {

    @Test
    void shouldUseVersionedOpsApiPrefix() {
        assertThat(OpsApiPaths.OPS_API_V1).isEqualTo("/api/v1/ops");
    }

    @Test
    void shouldResolveStablePaginationDefaults() {
        var request = new OpsPageRequest(null, null);

        assertThat(request.resolvedSize()).isEqualTo(50);
    }

    @Test
    void shouldResolveStableSortDefaults() {
        var request = new OpsSortRequest(null, null);

        assertThat(request.resolvedSortBy()).isEqualTo("createdAt");
        assertThat(request.resolvedSortDirection()).isEqualTo(OpsSortDirection.DESC);
    }

    @Test
    void shouldCopyPageResponseItems() {
        var items = List.of("pmt_1");
        var metadata = new OpsPageResponse.PageMetadata(1, null, false);

        var response = new OpsPageResponse<>(items, metadata);

        assertThat(response.items()).containsExactly("pmt_1");
        assertThat(response.page()).isEqualTo(metadata);
    }
}
