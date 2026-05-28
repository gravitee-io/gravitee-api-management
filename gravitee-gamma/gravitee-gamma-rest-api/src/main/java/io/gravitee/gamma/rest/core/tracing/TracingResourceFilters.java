/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gamma.rest.core.tracing;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds the OTel resource-attribute filter envelope the tracing repositories use to scope queries.
 * Module-agnostic: the caller passes the {@code gravitee.module} value (e.g. {@code "aim"},
 * {@code "apim"}, …) so a single global trace explorer can serve every gamma module without
 * duplicating the filter logic per-module.
 *
 * <p>The resulting map is passed to {@code TracingPort} / {@code OtelLogPort} as the resource scope,
 * which the backing repositories emit as {@code term { resource.attributes.<key>: <value> }} ES
 * filters so spans from another env / module / API never leak across.
 *
 * @author GraviteeSource Team
 */
public final class TracingResourceFilters {

    /** OTel resource-attribute key for the producing env id. See {@code TracerResourceAttributes}. */
    public static final String ENV_ID_KEY = "gravitee.env.id";

    /** OTel resource-attribute key for the producing API id. See {@code TracerResourceAttributes}. */
    public static final String API_ID_KEY = "gravitee.api.id";

    private TracingResourceFilters() {}

    /**
     * Builds the resource-attribute filter map scoped to a specific API inside an env. Module isn't
     * filtered explicitly: every {@code apiId} belongs to exactly one module (an API's type defines
     * its module — LLM_PROXY → AIM, HTTP_PROXY → APIM, etc.), so the api-id term alone produces
     * module-correct results without a redundant {@code gravitee.module} clause.
     *
     * <p>{@code apiId} is required by the explorer: per-API auth scope means each query stays bounded
     * regardless of how many APIs the caller can see, and the single resource-attribute term is
     * portable across backends (works on Tempo too, which doesn't efficiently OR over hundreds of
     * api-ids).
     */
    public static Map<String, String> forApi(String envId, String apiId) {
        Map<String, String> filters = new LinkedHashMap<>();
        if (envId != null) {
            filters.put(ENV_ID_KEY, envId);
        }
        if (apiId != null) {
            filters.put(API_ID_KEY, apiId);
        }
        return filters;
    }
}
