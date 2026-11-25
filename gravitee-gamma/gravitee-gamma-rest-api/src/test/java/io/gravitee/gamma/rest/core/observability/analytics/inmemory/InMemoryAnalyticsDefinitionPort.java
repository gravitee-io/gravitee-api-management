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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An analytics catalog holding the metrics a test gives it, in the order it gives them, and serving the additional
 * metrics for the kinds a test gives it, the HTTP ones and Edge by default, with the engine's measures and the facets
 * and filters of the {@link #DIMENSIONS_OF} metric a test gives, a fixed set otherwise. A built-in facet is offered for
 * the kinds of every metric given that is grouped by it, as the engine catalog offers it.
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

    /** The built-in the additional metrics take their facets from, as the engine names it. */
    public static final String DIMENSIONS_OF = AdditionalMetricName.DIMENSIONS_OF.name();

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
        var kindsListing = kindsListingEachFacet();
        return metrics
            .stream()
            .filter(metric -> metric.apiTypes().contains(apiKind))
            .map(metric -> offeredForTheirKinds(metric, kindsListing))
            .toList();
    }

    /** The built-ins the engine reads from the request documents, as the HTTP data plane serves them. */
    public static final List<String> GROUPED_BY_FIELDS = List.of(
        DIMENSIONS_OF,
        "HTTP_ERRORS",
        "HTTP_GATEWAY_RESPONSE_TIME",
        "HTTP_ENDPOINT_RESPONSE_TIME",
        "HTTP_GATEWAY_LATENCY",
        "HTTP_REQUEST_CONTENT_LENGTH",
        "HTTP_RESPONSE_CONTENT_LENGTH"
    );

    @Override
    public AdditionalMetricSupport additionalMetricSupport() {
        var kindsListing = kindsListingEachFacet();
        var facets = metrics
            .stream()
            .filter(metric -> metric.name().equals(DIMENSIONS_OF))
            .findFirst()
            .map(metric -> offeredForTheirKinds(metric, kindsListing).facets())
            .orElseGet(() ->
                List.of(
                    DimensionDefinition.builtIn("API", "API label", HTTP_KINDS),
                    DimensionDefinition.builtIn("HTTP_STATUS", "HTTP_STATUS label", HTTP_KINDS)
                )
            );
        return new AdditionalMetricSupport(
            additionalMetricKinds,
            AdditionalMetricName.MEASURES.stream().map(Enum::name).toList(),
            facets,
            List.of("API", "API_TYPE"),
            DIMENSIONS_OF,
            GROUPED_BY_FIELDS
        );
    }

    /** Computed on each call: a test gives its metrics after building the fixture. */
    private Map<String, Set<ApiType>> kindsListingEachFacet() {
        Map<String, Set<ApiType>> kinds = new HashMap<>();
        for (var metric : metrics) {
            for (var facet : metric.facets()) {
                kinds.computeIfAbsent(facet.name(), name -> EnumSet.noneOf(ApiType.class)).addAll(metric.apiTypes());
            }
        }
        return kinds;
    }

    private static MetricDefinition offeredForTheirKinds(MetricDefinition metric, Map<String, Set<ApiType>> kindsListing) {
        return new MetricDefinition(
            metric.name(),
            metric.label(),
            metric.description(),
            metric.namespace(),
            metric.unit(),
            metric.apiTypes(),
            metric.measures(),
            metric
                .facets()
                .stream()
                .map(facet ->
                    facet.origin() == CatalogOrigin.BUILT_IN
                        ? DimensionDefinition.builtIn(facet.name(), facet.label(), kindsListing.get(facet.name()))
                        : facet
                )
                .toList(),
            metric.filters(),
            metric.origin(),
            metric.currency(),
            metric.lastSeen()
        );
    }
}
