package dev.kavrin.paymentrisk.ops.api;

import dev.kavrin.paymentrisk.shared.api.version.ApiPaths;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OpsApiPaths {
    public static final String OPS_API_V1 = ApiPaths.API_V1 + "/ops";
}
