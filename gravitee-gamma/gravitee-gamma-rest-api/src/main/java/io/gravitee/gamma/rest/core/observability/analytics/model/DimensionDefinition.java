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
package io.gravitee.gamma.rest.core.observability.analytics.model;

import io.gravitee.gamma.rest.core.observability.catalog.model.CatalogOrigin;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import java.time.Instant;
import java.util.Set;

/**
 * A facet a metric can be grouped by, with the label of the analytics catalog.
 *
 * @param description the record's description on a {@code DECLARED} facet; {@code null} otherwise
 * @param namespace the namespace of an additional metric field, as the catalog serves it
 *             ({@code AdditionalMetricNamespace}); {@code null} on a built-in and when unknown
 * @param apiTypes the kinds the facet is tied to: on an additional metric field, those its namespace ties it to, none
 *             when the namespace says nothing; on a built-in, the kinds of the metrics of a definition listing it
 *             ({@link #tiedTo}), none before the definition is built
 * @param lastSeen when a document of the environment last carried the key of a {@code DECLARED} or {@code DISCOVERED}
 *             facet, as the filter catalog serves it: to a traffic reader only, {@code null} otherwise and on a built-in
 * @author GraviteeSource Team
 */
public record DimensionDefinition(
    String name,
    String label,
    String description,
    String namespace,
    Set<ApiType> apiTypes,
    Instant lastSeen,
    CatalogOrigin origin
) {
    /** A facet tied to no kind: a built-in, or an additional metric field of no known namespace. */
    public DimensionDefinition(String name, String label, CatalogOrigin origin) {
        this(name, label, null, null, Set.of(), null, origin);
    }

    /** A built-in facet tied to {@code kinds}, those of the metrics listing it; a dynamic facet keeps its namespace's. */
    public DimensionDefinition tiedTo(Set<ApiType> kinds) {
        return origin == CatalogOrigin.BUILT_IN
            ? new DimensionDefinition(name, label, description, namespace, Set.copyOf(kinds), lastSeen, origin)
            : this;
    }
}
