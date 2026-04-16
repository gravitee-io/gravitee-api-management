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
package io.gravitee.apim.infra.domain_service.analytics_engine.definition;

import io.gravitee.apim.core.analytics_engine.model.AnalyticsDefinition;
import io.gravitee.apim.core.analytics_engine.model.AnalyticsDefinitionSpec;
import io.gravitee.apim.core.analytics_engine.model.ApiSpec;
import io.gravitee.apim.core.analytics_engine.model.FacetSpec;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.analytics_engine.query_service.AnalyticsDefinitionQueryService;
import io.gravitee.apim.infra.domain_service.observability.YAMLDefinitionLoader;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsDefinitionYAMLQueryService implements AnalyticsDefinitionQueryService {

    private static final String ANALYTICS_DEFINITION_FILE = "analytics/definition/analytics-definition.yaml";

    private final AnalyticsDefinitionSpec spec;

    public AnalyticsDefinitionYAMLQueryService() {
        var rawSpec = YAMLDefinitionLoader.load(ANALYTICS_DEFINITION_FILE, AnalyticsDefinition.class).spec();
        spec = enrichFiltersWithApiTypes(rawSpec);
    }

    private static AnalyticsDefinitionSpec enrichFiltersWithApiTypes(AnalyticsDefinitionSpec rawSpec) {
        Map<FilterSpec.Name, Set<ApiSpec.Name>> apisByFilter = new EnumMap<>(FilterSpec.Name.class);
        for (var metric : rawSpec.metrics()) {
            for (var filterName : metric.filters()) {
                apisByFilter.computeIfAbsent(filterName, k -> EnumSet.noneOf(ApiSpec.Name.class)).addAll(metric.apis());
            }
        }

        var enrichedFilters = rawSpec
            .filters()
            .stream()
            .map(f ->
                new FilterSpec(
                    f.name(),
                    f.label(),
                    f.type(),
                    f.enumValues(),
                    f.range(),
                    f.operators(),
                    List.copyOf(apisByFilter.getOrDefault(f.name(), Set.of()))
                )
            )
            .toList();

        return new AnalyticsDefinitionSpec(rawSpec.apis(), rawSpec.metrics(), enrichedFilters, rawSpec.facets());
    }

    @Override
    public List<ApiSpec> getApis() {
        return spec.apis();
    }

    @Override
    public List<MetricSpec> getMetrics(ApiSpec.Name apiSpecName) {
        return spec
            .metrics()
            .stream()
            .filter(metric -> metric.apis().contains(apiSpecName))
            .toList();
    }

    @Override
    public List<FilterSpec> getAllFilters() {
        return spec.filters();
    }

    @Override
    public List<FilterSpec> getFilters(MetricSpec.Name metricSpecName) {
        var metric = getMetricByName(metricSpecName);
        return spec
            .filters()
            .stream()
            .filter(filter -> metric.filters().contains(filter.name()))
            .toList();
    }

    @Override
    public List<FacetSpec> getFacets(MetricSpec.Name metricSpecName) {
        var metric = getMetricByName(metricSpecName);
        return spec
            .facets()
            .stream()
            .filter(facet -> metric.facets().contains(facet.name()))
            .toList();
    }

    @Override
    public Optional<MetricSpec> findMetric(MetricSpec.Name metricName) {
        return spec
            .metrics()
            .stream()
            .filter(metric -> metric.name().equals(metricName))
            .findFirst();
    }

    private MetricSpec getMetricByName(MetricSpec.Name metricSpecName) {
        return spec
            .metrics()
            .stream()
            .filter(metric -> metric.name().equals(metricSpecName))
            .findFirst()
            .orElse(null);
    }
}
