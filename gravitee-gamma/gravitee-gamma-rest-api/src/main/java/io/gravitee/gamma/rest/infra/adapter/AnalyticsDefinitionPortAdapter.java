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
package io.gravitee.gamma.rest.infra.adapter;

import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.analytics_engine.use_case.GetApiMetricSpecUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.GetMetricFacetSpecUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.GetMetricFilterSpecUseCase;
import io.gravitee.gamma.rest.core.observability.analytics.model.DimensionDefinition;
import io.gravitee.gamma.rest.core.observability.analytics.model.MetricDefinition;
import io.gravitee.gamma.rest.core.observability.analytics.port.service_provider.AnalyticsDefinitionPort;
import io.gravitee.gamma.rest.core.observability.catalog.model.CatalogOrigin;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterSpec;
import io.gravitee.gamma.rest.core.observability.filter.model.Signal;
import io.gravitee.gamma.rest.core.observability.filter.port.service_provider.FilterRegistry;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;

/**
 * Reads the APIM analytics catalog through the management-v2 definition use cases, so Gamma offers exactly
 * the metrics and facets the analytics engine accepts, under the catalog's own labels.
 *
 * <p>Filters are the exception: Gamma checks a query's conditions against its own filter registry on the
 * {@code ANALYTICS} signal, not against the catalog, so a catalog filter the registry does not serve there is
 * left out rather than offered and then refused.
 *
 * @author GraviteeSource Team
 */
@AllArgsConstructor
public class AnalyticsDefinitionPortAdapter implements AnalyticsDefinitionPort {

    private final GetApiMetricSpecUseCase getApiMetricSpecUseCase;
    private final GetMetricFacetSpecUseCase getMetricFacetSpecUseCase;
    private final GetMetricFilterSpecUseCase getMetricFilterSpecUseCase;
    private final FilterRegistry filterRegistry;

    @Override
    public List<MetricDefinition> metricsOf(ApiType apiKind) {
        var analyticsFilters = filterRegistry
            .getFilters(Set.of(Signal.ANALYTICS), Set.of())
            .stream()
            .map(FilterSpec::name)
            .collect(Collectors.toSet());
        return getApiMetricSpecUseCase
            .execute(new GetApiMetricSpecUseCase.Input(apiKind.name()))
            .specs()
            .stream()
            .map(spec -> toCoreModel(spec, analyticsFilters))
            .toList();
    }

    private MetricDefinition toCoreModel(MetricSpec spec, Set<String> analyticsFilters) {
        var metric = spec.name().name();
        var facets = getMetricFacetSpecUseCase
            .execute(new GetMetricFacetSpecUseCase.Input(metric))
            .specs()
            .stream()
            .map(facet -> new DimensionDefinition(facet.name().name(), facet.label(), CatalogOrigin.BUILT_IN))
            .toList();
        var filters = getMetricFilterSpecUseCase
            .execute(new GetMetricFilterSpecUseCase.Input(metric))
            .specs()
            .stream()
            .map(filter -> filter.name().name())
            .filter(analyticsFilters::contains)
            .toList();
        return new MetricDefinition(
            metric,
            spec.label(),
            spec.unit() == null ? null : spec.unit().name(),
            spec
                .apis()
                .stream()
                .map(api -> ApiType.valueOf(api.name()))
                .toList(),
            spec.measures().stream().map(Enum::name).toList(),
            facets,
            filters,
            CatalogOrigin.BUILT_IN
        );
    }
}
