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
package io.gravitee.gamma.rest.core.observability.catalog.model;

import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterType;
import java.time.Instant;
import java.util.Set;

/**
 * One entry of the environment's additional metric catalog.
 *
 * @param name      the wire name: the built-in name of a {@code BUILT_IN} entry, {@code additional-metrics.<rawKey>}
 *                  otherwise.
 * @param type      the filter type of the built-in spec serving the entry; {@code null} otherwise, the value type then
 *                  gives it.
 * @param apiTypes  the kinds of the built-in on a {@code BUILT_IN} entry; on the others, the kinds the key's namespace
 *                  ties it to ({@link AdditionalMetricNamespace}), none when it says nothing: documents do not record
 *                  which API kind wrote a key.
 * @param lastSeen  {@code null} when no document carried the key in the discovery window, or when the caller may not
 *                  read the environment's traffic.
 * @param namespace the key's namespace on a {@code DECLARED} or {@code DISCOVERED} entry, as
 *                  {@link AdditionalMetricNamespace#of} serves it; {@code null} on a {@code BUILT_IN} entry and when
 *                  unknown.
 *
 * @author GraviteeSource Team
 */
public record AdditionalMetricCatalogEntry(
    String name,
    AdditionalMetricField field,
    CatalogOrigin origin,
    String label,
    String description,
    String unit,
    boolean exposed,
    Instant lastSeen,
    FilterType type,
    Set<ApiType> apiTypes,
    String namespace
) {
    public AdditionalMetricCatalogEntry {
        apiTypes = Set.copyOf(apiTypes);
    }
}
