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
package io.gravitee.apim.rest.api.mcp.core.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.analytics_engine.model.ApiSpec;
import io.gravitee.apim.core.analytics_engine.model.FacetSpec;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.observability.model.NumberRange;
import io.gravitee.apim.rest.api.mcp.core.Names;
import io.gravitee.apim.rest.api.mcp.core.port.AnalyticsCatalog;
import java.util.List;

/**
 * The analytics catalog: which metrics exist for which kind of API, and what each can be measured, split and
 * filtered by. Read from the shipped definition.
 */
@UseCase
public class DescribeMetricsUseCase {

    private final AnalyticsCatalog catalog;

    public DescribeMetricsUseCase(AnalyticsCatalog catalog) {
        this.catalog = catalog;
    }

    public record Input(String apiType, String metric) {}

    /**
     * @param apiTypes the catalog, narrowed to one API type when asked; null when one metric is described
     * @param metric one metric with its facets and filters; null otherwise
     */
    public record Output(String summary, List<ApiTypeMetrics> apiTypes, MetricDetail metric) {}

    public record ApiTypeMetrics(String name, String label, List<Metric> metrics) {}

    public record Metric(String name, String label, String unit, List<String> measures) {}

    public record MetricDetail(Metric metric, List<String> apiTypes, List<Facet> facets, List<Filter> filters) {}

    public record Facet(String name, String label, String type, List<Object> values) {}

    public record Filter(String name, String label, String type, List<String> operators, List<String> values, NumberRange range) {}

    public Output execute(Input input) {
        if (input.metric() != null && !input.metric().isBlank()) {
            return describe(Names.metric(input.metric()));
        }
        var types = catalog.apiTypes();
        if (input.apiType() != null && !input.apiType().isBlank()) {
            var wanted = Names.apiType(input.apiType());
            types = types
                .stream()
                .filter(type -> type.name() == wanted)
                .toList();
        }
        var described = types.stream().map(this::describe).toList();
        long metrics = described
            .stream()
            .mapToLong(type -> type.metrics().size())
            .sum();
        return new Output(
            metrics +
                " metric(s) over " +
                described.size() +
                " API type(s). Call describe_metrics with metric=<name> for its facets and filters.",
            described,
            null
        );
    }

    private ApiTypeMetrics describe(ApiSpec type) {
        return new ApiTypeMetrics(
            type.name().name(),
            type.label(),
            catalog.metrics(type.name()).stream().map(DescribeMetricsUseCase::toMetric).toList()
        );
    }

    private Output describe(MetricSpec.Name name) {
        var spec = catalog
            .apiTypes()
            .stream()
            .flatMap(type -> catalog.metrics(type.name()).stream())
            .filter(metric -> metric.name() == name)
            .findFirst()
            .orElseThrow(() -> Names.unknownMetric(name.name()));
        var facets = catalog.facets(name).stream().map(DescribeMetricsUseCase::toFacet).toList();
        var filters = catalog.filters(name).stream().map(DescribeMetricsUseCase::toFilter).toList();
        var detail = new MetricDetail(toMetric(spec), spec.apis().stream().map(Enum::name).toList(), facets, filters);
        return new Output(
            name + " (" + spec.unit() + "): " + facets.size() + " facet(s) to split by, " + filters.size() + " filter(s).",
            null,
            detail
        );
    }

    private static Metric toMetric(MetricSpec spec) {
        return new Metric(spec.name().name(), spec.label(), spec.unit().name(), spec.measures().stream().map(Enum::name).toList());
    }

    private static Facet toFacet(FacetSpec spec) {
        return new Facet(spec.name().name(), spec.label(), spec.type(), spec.enumValues());
    }

    private static Filter toFilter(FilterSpec spec) {
        return new Filter(
            spec.name().name(),
            spec.label(),
            spec.type() == null ? null : spec.type().name(),
            spec.operators() == null ? List.of() : spec.operators().stream().map(Enum::name).toList(),
            spec.enumValues(),
            spec.range()
        );
    }
}
