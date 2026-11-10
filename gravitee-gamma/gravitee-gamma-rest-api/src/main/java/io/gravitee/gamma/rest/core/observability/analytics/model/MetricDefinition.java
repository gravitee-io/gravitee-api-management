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
import java.util.ArrayList;
import java.util.List;

/**
 * One metric of the analytics definition and what a query may ask of it: its measures, the facets it is grouped
 * by and the filters that narrow it. Names are the ones the analytics endpoints accept: a built-in name, or the wire
 * name of an additional metric field holding a number.
 *
 * @param description the record's description on a {@code DECLARED} metric; {@code null} otherwise
 * @param namespace the namespace of an additional metric field, as the catalog serves it
 *             ({@code AdditionalMetricNamespace}); {@code null} on a built-in and when unknown
 * @param apiTypes the kinds a built-in exists for; on an additional metric field, the kinds its namespace ties it to
 *             among those the engine measures such a field for, every one of those when the namespace says nothing
 * @param unit the unit of a built-in in Gamma's vocabulary ({@code AdditionalMetricUnit.NAMED_UNITS}, mapped from the
 *             engine's by {@code ServedMetricUnit}), the record's unit on a {@code DECLARED} field, or {@code null}
 *             when none is known
 * @param currency the ISO 4217 code of a built-in whose unit is {@code CURRENCY}; {@code null} on every other metric,
 *             a declared record in {@code CURRENCY} included, its currency being unknown
 * @param lastSeen when a document of the environment last carried the key of a {@code DECLARED} or {@code DISCOVERED}
 *             metric, as the filter catalog serves it: to a traffic reader only, {@code null} otherwise and on a
 *             built-in
 * @author GraviteeSource Team
 */
public record MetricDefinition(
    String name,
    String label,
    String description,
    String namespace,
    String unit,
    List<ApiType> apiTypes,
    List<String> measures,
    List<DimensionDefinition> facets,
    List<String> filters,
    CatalogOrigin origin,
    String currency,
    Instant lastSeen
) {
    /** A metric with no description, namespace or currency: a built-in that is not a cost. */
    public MetricDefinition(
        String name,
        String label,
        String unit,
        List<ApiType> apiTypes,
        List<String> measures,
        List<DimensionDefinition> facets,
        List<String> filters,
        CatalogOrigin origin
    ) {
        this(name, label, null, null, unit, apiTypes, measures, facets, filters, origin, null, null);
    }

    /** A built-in, with its currency when it is a cost. */
    public MetricDefinition(
        String name,
        String label,
        String unit,
        List<ApiType> apiTypes,
        List<String> measures,
        List<DimensionDefinition> facets,
        List<String> filters,
        CatalogOrigin origin,
        String currency
    ) {
        this(name, label, null, null, unit, apiTypes, measures, facets, filters, origin, currency, null);
    }

    /** A built-in is grouped by its API kinds; an additional metric field by its origin, under one family. */
    public MetricFamily family() {
        return origin == CatalogOrigin.BUILT_IN ? MetricFamily.of(apiTypes) : MetricFamily.ADDITIONAL;
    }

    /** This metric grouped by {@code more} facets as well, after its own. */
    public MetricDefinition groupedAlsoBy(List<DimensionDefinition> more) {
        if (more.isEmpty()) {
            return this;
        }
        List<DimensionDefinition> grouped = new ArrayList<>(facets);
        grouped.addAll(more);
        return new MetricDefinition(
            name,
            label,
            description,
            namespace,
            unit,
            apiTypes,
            measures,
            List.copyOf(grouped),
            filters,
            origin,
            currency,
            lastSeen
        );
    }
}
