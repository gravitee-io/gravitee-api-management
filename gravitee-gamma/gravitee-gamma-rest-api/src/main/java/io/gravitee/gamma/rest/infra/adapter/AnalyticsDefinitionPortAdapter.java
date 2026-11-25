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

import io.gravitee.apim.core.analytics_engine.model.AdditionalMetricName;
import io.gravitee.apim.core.analytics_engine.model.ApiSpec;
import io.gravitee.apim.core.analytics_engine.model.FacetSpec;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.analytics_engine.service_provider.AnalyticsQueryContextProvider;
import io.gravitee.apim.core.analytics_engine.use_case.GetApiMetricSpecUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.GetMetricFacetSpecUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.GetMetricFilterSpecUseCase;
import io.gravitee.gamma.rest.core.observability.analytics.model.AdditionalMetricSupport;
import io.gravitee.gamma.rest.core.observability.analytics.model.DimensionDefinition;
import io.gravitee.gamma.rest.core.observability.analytics.model.MetricDefinition;
import io.gravitee.gamma.rest.core.observability.analytics.port.service_provider.AnalyticsDefinitionPort;
import io.gravitee.gamma.rest.core.observability.catalog.model.CatalogOrigin;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterSpec;
import io.gravitee.gamma.rest.core.observability.filter.model.Signal;
import io.gravitee.gamma.rest.core.observability.filter.port.service_provider.FilterRegistry;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;

/**
 * Reads the APIM analytics catalog through the management-v2 definition use cases, so Gamma offers exactly
 * the metrics and facets the analytics engine accepts, under the catalog's own labels.
 *
 * <p>Filters are the exception: Gamma checks a query's conditions against its own filter registry on the
 * {@code ANALYTICS} signal, not against the catalog, so a catalog filter the registry does not serve there is
 * left out rather than offered and then refused. A facet is offered for the kinds of every catalog metric grouped by
 * it, whichever kinds a request asks ({@link DimensionDefinition#builtIn}), under the label of the Gamma filter of its
 * name when there is one, so the field reads the same in the filter bar and in the dimension picker.
 *
 * <p>An additional metric field is in no catalog declaration: the engine measures it with
 * {@link AdditionalMetricName#MEASURES} in the request documents, where it is grouped and filtered like the built-in
 * {@link AdditionalMetricName#DIMENSIONS_OF}, and serves it for every kind whose built-ins are searched in those
 * documents (the HTTP family and Edge, read from the routing). The support is read from there.
 *
 * @author GraviteeSource Team
 */
@AllArgsConstructor
public class AnalyticsDefinitionPortAdapter implements AnalyticsDefinitionPort {

    private final GetApiMetricSpecUseCase getApiMetricSpecUseCase;
    private final GetMetricFacetSpecUseCase getMetricFacetSpecUseCase;
    private final GetMetricFilterSpecUseCase getMetricFilterSpecUseCase;
    private final FilterRegistry filterRegistry;
    private final AnalyticsQueryContextProvider queryContextProvider;

    @Override
    public List<MetricDefinition> metricsOf(ApiType apiKind) {
        var analyticsFilters = analyticsFilters();
        var dimensions = builtInDimensions();
        return metricSpecsOf(apiKind.name())
            .stream()
            .map(spec -> toCoreModel(spec, analyticsFilters, dimensions))
            .toList();
    }

    @Override
    public AdditionalMetricSupport additionalMetricSupport() {
        var dimensionsOf = Arrays.stream(ApiSpec.Name.values())
            .flatMap(kind -> metricSpecsOf(kind.name()).stream())
            .filter(spec -> spec.name() == AdditionalMetricName.DIMENSIONS_OF)
            .findFirst()
            .orElseThrow(() ->
                new IllegalStateException(
                    "The analytics catalog declares no " +
                        AdditionalMetricName.DIMENSIONS_OF +
                        " metric, the one an additional metric field takes its dimensions from"
                )
            );
        var apiTypes = Arrays.stream(ApiSpec.Name.values())
            .filter(this::measuresAdditionalMetrics)
            .map(kind -> ApiType.valueOf(kind.name()))
            .toList();
        var analyticsFilters = analyticsFilters();
        var groupedByFields = Arrays.stream(ApiSpec.Name.values())
            .flatMap(kind -> metricSpecsOf(kind.name()).stream())
            .map(MetricSpec::name)
            .filter(queryContextProvider::groupsByAdditionalMetricsLike)
            .map(Enum::name)
            .distinct()
            .toList();
        return new AdditionalMetricSupport(
            apiTypes,
            AdditionalMetricName.MEASURES.stream().map(Enum::name).toList(),
            facetsOf(dimensionsOf.name(), builtInDimensions()),
            filtersOf(dimensionsOf.name(), analyticsFilters),
            dimensionsOf.name().name(),
            groupedByFields
        );
    }

    /** Whether the engine searches a kind's built-ins with the service that also measures additional metric fields. */
    private boolean measuresAdditionalMetrics(ApiSpec.Name kind) {
        return metricSpecsOf(kind.name())
            .stream()
            .anyMatch(spec -> queryContextProvider.measuresAdditionalMetricsLike(spec.name()));
    }

    private List<MetricSpec> metricSpecsOf(String apiKind) {
        return getApiMetricSpecUseCase.execute(new GetApiMetricSpecUseCase.Input(apiKind)).specs();
    }

    private Set<String> analyticsFilters() {
        return filterRegistry.getFilters(Set.of(Signal.ANALYTICS), Set.of()).stream().map(FilterSpec::name).collect(Collectors.toSet());
    }

    /** Every facet of the catalog by name, offered for the kinds of the catalog metrics grouped by it, every kind read. */
    private Map<String, DimensionDefinition> builtInDimensions() {
        Map<String, String> filterLabels = filterRegistry
            .getFilters(Set.of(), Set.of())
            .stream()
            .collect(Collectors.toMap(FilterSpec::name, FilterSpec::label, (first, next) -> first));
        Map<String, Set<ApiType>> kinds = new HashMap<>();
        Map<String, String> catalogLabels = new HashMap<>();
        for (var kind : ApiSpec.Name.values()) {
            for (var spec : metricSpecsOf(kind.name())) {
                for (var facet : facetSpecsOf(spec.name())) {
                    kinds.computeIfAbsent(facet.name().name(), name -> EnumSet.noneOf(ApiType.class)).addAll(apiTypesOf(spec));
                    catalogLabels.putIfAbsent(facet.name().name(), facet.label());
                }
            }
        }
        return kinds
            .keySet()
            .stream()
            .collect(
                Collectors.toMap(Function.identity(), name ->
                    DimensionDefinition.builtIn(name, filterLabels.getOrDefault(name, catalogLabels.get(name)), kinds.get(name))
                )
            );
    }

    private MetricDefinition toCoreModel(MetricSpec spec, Set<String> analyticsFilters, Map<String, DimensionDefinition> dimensions) {
        return new MetricDefinition(
            spec.name().name(),
            spec.label(),
            spec.unit() == null ? null : ServedMetricUnit.of(spec.unit()),
            apiTypesOf(spec),
            spec.measures().stream().map(Enum::name).toList(),
            facetsOf(spec.name(), dimensions),
            filtersOf(spec.name(), analyticsFilters),
            CatalogOrigin.BUILT_IN,
            spec.unit() == null ? null : ServedMetricUnit.currencyOf(spec.unit()).orElse(null)
        );
    }

    private static List<ApiType> apiTypesOf(MetricSpec spec) {
        return spec
            .apis()
            .stream()
            .map(api -> ApiType.valueOf(api.name()))
            .toList();
    }

    private List<DimensionDefinition> facetsOf(MetricSpec.Name metric, Map<String, DimensionDefinition> dimensions) {
        return facetSpecsOf(metric)
            .stream()
            .map(facet -> dimensions.get(facet.name().name()))
            .toList();
    }

    private List<FacetSpec> facetSpecsOf(MetricSpec.Name metric) {
        return getMetricFacetSpecUseCase.execute(new GetMetricFacetSpecUseCase.Input(metric.name())).specs();
    }

    private List<String> filtersOf(MetricSpec.Name metric, Set<String> analyticsFilters) {
        return getMetricFilterSpecUseCase
            .execute(new GetMetricFilterSpecUseCase.Input(metric.name()))
            .specs()
            .stream()
            .map(filter -> filter.name().name())
            .filter(analyticsFilters::contains)
            .toList();
    }
}
