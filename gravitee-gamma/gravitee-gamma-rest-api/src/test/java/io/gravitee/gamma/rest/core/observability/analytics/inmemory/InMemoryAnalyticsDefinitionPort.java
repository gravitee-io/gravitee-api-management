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
package io.gravitee.gamma.rest.core.observability.analytics.inmemory;

import static io.gravitee.gamma.rest.core.observability.analytics.model.MetricDefinitionFixtures.aMetric;

import io.gravitee.apim.core.analytics_engine.model.AdditionalMetricName;
import io.gravitee.gamma.rest.core.observability.analytics.model.AdditionalMetricSupport;
import io.gravitee.gamma.rest.core.observability.analytics.model.DimensionDefinition;
import io.gravitee.gamma.rest.core.observability.analytics.model.MetricDefinition;
import io.gravitee.gamma.rest.core.observability.analytics.port.service_provider.AnalyticsDefinitionPort;
import io.gravitee.gamma.rest.core.observability.catalog.model.CatalogOrigin;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import java.util.ArrayList;
import java.util.List;

/**
 * An analytics catalog holding the metrics a test gives it, in the order it gives them, and serving the additional
 * metrics for the kinds a test gives it, the HTTP ones and Edge by default, with the engine's measures and a fixed
 * facet and filter set.
 */
public class InMemoryAnalyticsDefinitionPort implements AnalyticsDefinitionPort {

    /** The kinds the fixture's HTTP built-ins exist for. */
    public static final List<ApiType> HTTP_KINDS = List.of(ApiType.HTTP_PROXY, ApiType.LLM, ApiType.MESSAGE, ApiType.MCP, ApiType.A2A);

    /** The kinds the fixture serves additional metrics for unless told otherwise: the HTTP ones and Edge, as the engine does. */
    public static final List<ApiType> ADDITIONAL_METRIC_KINDS = List.of(
        ApiType.HTTP_PROXY,
        ApiType.LLM,
        ApiType.MESSAGE,
        ApiType.MCP,
        ApiType.A2A,
        ApiType.EDGE
    );

    private final List<MetricDefinition> metrics = new ArrayList<>();
    private List<ApiType> additionalMetricKinds = ADDITIONAL_METRIC_KINDS;

    public MetricDefinition givenMetric(String name, List<ApiType> apiTypes, String... facets) {
        return givenMetric(aMetric(name, apiTypes, facets));
    }

    public MetricDefinition givenMetric(MetricDefinition metric) {
        metrics.add(metric);
        return metric;
    }

    public InMemoryAnalyticsDefinitionPort givenAdditionalMetricsServedFor(List<ApiType> kinds) {
        additionalMetricKinds = List.copyOf(kinds);
        return this;
    }

    @Override
    public List<MetricDefinition> metricsOf(ApiType apiKind) {
        return metrics
            .stream()
            .filter(metric -> metric.apiTypes().contains(apiKind))
            .toList();
    }

    @Override
    public AdditionalMetricSupport additionalMetricSupport() {
        return new AdditionalMetricSupport(
            additionalMetricKinds,
            AdditionalMetricName.MEASURES.stream().map(Enum::name).toList(),
            List.of(
                new DimensionDefinition("API", "API label", CatalogOrigin.BUILT_IN),
                new DimensionDefinition("HTTP_STATUS", "HTTP_STATUS label", CatalogOrigin.BUILT_IN)
            ),
            List.of("API", "API_TYPE")
        );
    }
}
