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
package io.gravitee.repository.tracing.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Filter passed to {@link io.gravitee.repository.tracing.api.TracingRepository#searchTraces}.
 * <p>
 * Two separate equality-filter maps matching the OTel attribute split:
 * <ul>
 *   <li>{@code attributeFilters} — span-attribute filters. The backend encodes each entry against the per-span
 *       attribute store (e.g. {@code attributes.<key>} in Elasticsearch, {@code span.<key>} in TraceQL).</li>
 *   <li>{@code resourceAttributeFilters} — resource-attribute filters. The backend encodes each entry against
 *       the resource-level attribute store (e.g. {@code resource.attributes.<key>} in Elasticsearch,
 *       {@code resource.<key>} in TraceQL). Use this for properties set by the producer's OTel SDK once per
 *       process — service identity, environment, deployment module, region, etc. — rather than per-span tags.</li>
 * </ul>
 * Separating the two avoids the impl having to guess (by prefix or hardcoded key list) where each entry
 * belongs, and lines up with how the backends actually store attributes.
 *
 * @author GraviteeSource Team
 */
public record TraceSearchCriteria(
    Map<String, String> attributeFilters,
    Integer limit,
    Instant start,
    Instant end,
    Map<String, String> resourceAttributeFilters
) {
    public TraceSearchCriteria {
        if (limit == null || limit <= 0) {
            limit = 20;
        }
        if (attributeFilters == null) {
            attributeFilters = new HashMap<>();
        }
        if (resourceAttributeFilters == null) {
            resourceAttributeFilters = new HashMap<>();
        }
    }
}
