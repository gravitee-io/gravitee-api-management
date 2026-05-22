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
package io.gravitee.gateway.opentelemetry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Builds the OTel resource-attribute map a per-API Tracer should carry so consumers (Grafana, the APIM
 * tracing repository, future debug UIs) can filter traces by module / api / org / env without backend
 * configuration tricks. Reactor-agnostic by design — APIM, ESM, AIM and any future reactor type pass their
 * own module identifier as the first argument.
 * <p>
 * Null values are skipped rather than included as nulls — {@code Map.of} would NPE, and an API deployed
 * from a minimal definition (legacy fixtures, some IT paths) may legitimately have null
 * {@code organizationId} / {@code environmentId}; we want the deployment to succeed with whatever
 * identity is available rather than failing.
 *
 * @author GraviteeSource Team
 */
public final class TracerResourceAttributes {

    private TracerResourceAttributes() {}

    /**
     * @param module    {@code gravitee.module} value identifying the reactor type — e.g. {@code apim},
     *                  {@code esm}, {@code aim}. Required (non-null); use the caller's module name verbatim.
     * @param apiId     {@code gravitee.api.id} value, or {@code null} to omit.
     * @param apiName   {@code gravitee.api.name} value, or {@code null} to omit.
     * @param apiType   {@code gravitee.api.type} discriminator (reactor-specific, e.g. {@code API_V4}
     *                  / {@code API_V4_TCP} / {@code API_V2} on APIM), or {@code null} to omit.
     * @param orgId     {@code gravitee.org.id} value, or {@code null} to omit.
     * @param envId     {@code gravitee.env.id} value, or {@code null} to omit.
     */
    public static Map<String, String> of(
        final String module,
        final String apiId,
        final String apiName,
        final String apiType,
        final String orgId,
        final String envId
    ) {
        // Enforce the documented non-null contract at the boundary: a null module would silently insert
        // "gravitee.module" → null otherwise, which downstream OTel SDK code would only surface as a
        // confusing NPE deep in the exporter pipeline.
        Objects.requireNonNull(module, "module must not be null");
        Map<String, String> attrs = new LinkedHashMap<>();
        attrs.put("gravitee.module", module);
        putIfNotNull(attrs, "gravitee.api.id", apiId);
        putIfNotNull(attrs, "gravitee.api.name", apiName);
        putIfNotNull(attrs, "gravitee.api.type", apiType);
        putIfNotNull(attrs, "gravitee.org.id", orgId);
        putIfNotNull(attrs, "gravitee.env.id", envId);
        return attrs;
    }

    private static void putIfNotNull(final Map<String, String> map, final String key, final String value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}
