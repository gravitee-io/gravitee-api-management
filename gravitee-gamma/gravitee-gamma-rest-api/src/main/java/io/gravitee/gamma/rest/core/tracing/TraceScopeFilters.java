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
 * Builds the scope envelope (env + api keys) the trace explorer uses to bound queries against the
 * two OTel signals it stitches together — spans and payload logs. The map content is the same for
 * both signals, but each port interprets it differently:
 * <ul>
 *   <li>{@code TracingPort} (spans): the per-API tracer emits all Gravitee attributes on the OTel
 *   {@code Resource}, so the entries become {@code term { resource.attributes.<key>: <value> }}
 *   filters server-side.</li>
 *   <li>{@code OtelLogPort} (payload logs): {@code gravitee-reporter-otel} only carries
 *   {@code gravitee.org.id} as a Logger resource attribute (one per-org Logger serves every
 *   env/API through tag-based sharding); env, api etc. live in each {@code LogRecord}'s
 *   attributes map, so the entries become {@code term { attributes.<key>: <value> }} filters.</li>
 * </ul>
 * The use case is the boundary that knows it's reading both signals and is responsible for handing
 * the right shape to each port — see {@code GetTraceDetailUseCase}.
 *
 * @author GraviteeSource Team
 */
public final class TraceScopeFilters {

    /** OTel attribute key for the producing env id. See {@code TracerResourceAttributes}. */
    public static final String ENV_ID_KEY = "gravitee.env.id";

    /** OTel attribute key for the producing API id. See {@code TracerResourceAttributes}. */
    public static final String API_ID_KEY = "gravitee.api.id";

    private TraceScopeFilters() {}

    /**
     * Builds the scope envelope for a specific API inside an env. Module isn't filtered explicitly:
     * every {@code apiId} belongs to exactly one module (an API's type defines its module —
     * LLM_PROXY → AIM, HTTP_PROXY → APIM, etc.), so the api-id term alone produces module-correct
     * results without a redundant {@code gravitee.module} clause.
     *
     * <p>{@code apiId} is required by the explorer: per-API auth scope means each query stays bounded
     * regardless of how many APIs the caller can see, and the single term is portable across
     * backends (works on Tempo too, which doesn't efficiently OR over hundreds of api-ids).
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
