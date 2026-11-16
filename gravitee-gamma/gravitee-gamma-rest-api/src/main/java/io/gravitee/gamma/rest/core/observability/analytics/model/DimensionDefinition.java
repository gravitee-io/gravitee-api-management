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
import io.gravitee.gamma.rest.core.observability.filter.model.StaticFilters;
import java.time.Instant;
import java.util.Collection;
import java.util.Set;

/**
 * A facet a metric can be grouped by: a built-in one under the label of the Gamma filter of its name when there is
 * one, else of the analytics catalog.
 *
 * @param description the record's description on a {@code DECLARED} facet; {@code null} otherwise
 * @param namespace the namespace of an additional metric field, as the catalog serves it
 *             ({@code AdditionalMetricNamespace}); {@code null} on a built-in and when unknown
 * @param apiTypes the kinds the facet is offered for: on an additional metric field, those its namespace ties it to,
 *             as the filter catalog serves them for the key, none when the namespace says nothing; on a built-in, those
 *             {@link #builtIn} gives, whatever the request
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
    /** An additional metric field of no known namespace, tied to no kind. */
    public DimensionDefinition(String name, String label, CatalogOrigin origin) {
        this(name, label, null, null, Set.of(), null, origin);
    }

    /**
     * A facet of the engine catalog, offered for the kinds of the engine metrics grouped by it ({@code listingKinds}),
     * or for a kind-specific field, the kinds its filter declares ({@link StaticFilters#kindSpecificApiTypes}): the
     * engine carries the LLM and MCP proxy fields on every HTTP connection document, the product reads them for LLM
     * and MCP traffic only.
     */
    public static DimensionDefinition builtIn(String name, String label, Collection<ApiType> listingKinds) {
        var kinds = StaticFilters.kindSpecificApiTypes(name).orElseGet(() -> Set.copyOf(listingKinds));
        return new DimensionDefinition(name, label, null, null, kinds, null, CatalogOrigin.BUILT_IN);
    }
}
