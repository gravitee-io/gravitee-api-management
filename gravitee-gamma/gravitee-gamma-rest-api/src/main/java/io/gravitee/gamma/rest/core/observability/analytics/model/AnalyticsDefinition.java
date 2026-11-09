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
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * What a widget editor offers for some API kinds: the metrics grouped by family, and every facet they can be
 * grouped by.
 *
 * @param families   In {@link MetricFamily} order, none of them empty; metrics keep their catalog order.
 * @param dimensions Each facet of the metrics once, in order of first appearance in {@code families}.
 * @author GraviteeSource Team
 */
public record AnalyticsDefinition(List<FamilyMetrics> families, List<DimensionDefinition> dimensions) {
    public record FamilyMetrics(MetricFamily family, List<MetricDefinition> metrics) {}

    public static AnalyticsDefinition of(Collection<MetricDefinition> metrics) {
        Map<MetricFamily, List<MetricDefinition>> byFamily = metrics
            .stream()
            .collect(Collectors.groupingBy(MetricDefinition::family, () -> new EnumMap<>(MetricFamily.class), Collectors.toList()));
        var families = byFamily
            .entrySet()
            .stream()
            .map(entry -> new FamilyMetrics(entry.getKey(), List.copyOf(entry.getValue())))
            .toList();
        var dimensions = families
            .stream()
            .flatMap(family -> family.metrics().stream())
            .flatMap(metric -> metric.facets().stream())
            .collect(Collectors.toMap(DimensionDefinition::name, Function.identity(), (first, next) -> first, LinkedHashMap::new))
            .values();
        return new AnalyticsDefinition(families, List.copyOf(dimensions));
    }

    /**
     * The entries of {@code origins} only, metrics and dimensions each narrowed on their own: the metrics of those
     * origins, a family left empty dropped, and the dimensions of those origins, whether or not a kept metric is
     * grouped by them. A kept metric still names every facet it is grouped by. Every entry when no origin is asked.
     */
    public AnalyticsDefinition narrowedTo(Set<CatalogOrigin> origins) {
        var narrowedFamilies = families
            .stream()
            .map(family ->
                new FamilyMetrics(
                    family.family(),
                    family
                        .metrics()
                        .stream()
                        .filter(metric -> metric.origin().keptBy(origins))
                        .toList()
                )
            )
            .filter(family -> !family.metrics().isEmpty())
            .toList();
        var narrowedDimensions = dimensions
            .stream()
            .filter(dimension -> dimension.origin().keptBy(origins))
            .toList();
        return new AnalyticsDefinition(narrowedFamilies, narrowedDimensions);
    }
}
