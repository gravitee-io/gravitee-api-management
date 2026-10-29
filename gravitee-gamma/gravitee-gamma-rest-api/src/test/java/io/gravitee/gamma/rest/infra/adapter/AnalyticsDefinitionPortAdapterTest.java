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

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryValidator;
import io.gravitee.apim.core.analytics_engine.exception.InvalidQueryException;
import io.gravitee.apim.core.analytics_engine.model.ApiSpec;
import io.gravitee.apim.core.analytics_engine.model.FacetMetricMeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.FacetSpec;
import io.gravitee.apim.core.analytics_engine.model.FacetsRequest;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.analytics_engine.model.TimeRange;
import io.gravitee.apim.core.analytics_engine.use_case.GetApiMetricSpecUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.GetMetricFacetSpecUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.GetMetricFilterSpecUseCase;
import io.gravitee.apim.infra.domain_service.analytics_engine.definition.AnalyticsDefinitionYAMLQueryService;
import io.gravitee.gamma.rest.core.observability.analytics.model.AnalyticsDefinition;
import io.gravitee.gamma.rest.core.observability.analytics.model.CatalogOrigin;
import io.gravitee.gamma.rest.core.observability.analytics.model.DimensionDefinition;
import io.gravitee.gamma.rest.core.observability.analytics.model.MetricDefinition;
import io.gravitee.gamma.rest.core.observability.analytics.model.MetricFamily;
import io.gravitee.gamma.rest.core.observability.analytics.port.AnalyticsDefinitionPortContractTest;
import io.gravitee.gamma.rest.core.observability.analytics.port.service_provider.AnalyticsDefinitionPort;
import io.gravitee.gamma.rest.core.observability.analytics.use_case.GetObservabilityAnalyticsDefinitionUseCase;
import io.gravitee.gamma.rest.core.observability.filter.domain_service.ObservabilityFilterValidator;
import io.gravitee.gamma.rest.core.observability.filter.exception.UnsupportedObservabilityFilterException;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterCondition;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterOperator;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterSpec;
import io.gravitee.gamma.rest.core.observability.filter.model.Signal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Runs on the catalog shipped with the server: what Gamma serves is what the analytics engine accepts. */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AnalyticsDefinitionPortAdapterTest extends AnalyticsDefinitionPortContractTest {

    private final AnalyticsDefinitionYAMLQueryService catalog = new AnalyticsDefinitionYAMLQueryService();
    private final SpiFilterRegistry filterRegistry = new SpiFilterRegistry();
    private final AnalyticsDefinitionPortAdapter adapter = new AnalyticsDefinitionPortAdapter(
        new GetApiMetricSpecUseCase(catalog),
        new GetMetricFacetSpecUseCase(catalog),
        new GetMetricFilterSpecUseCase(catalog),
        filterRegistry
    );
    private final GetObservabilityAnalyticsDefinitionUseCase useCase = new GetObservabilityAnalyticsDefinitionUseCase(adapter);

    @Override
    protected AnalyticsDefinitionPort port() {
        return adapter;
    }

    @Test
    void should_serve_every_http_metric_in_the_gateway_family_for_http_proxy() {
        var definition = definitionOf(ApiType.HTTP_PROXY);

        assertThat(definition.families()).extracting(AnalyticsDefinition.FamilyMetrics::family).containsExactly(MetricFamily.GATEWAY);
        assertThat(metricNames(definition)).containsExactlyInAnyOrder(
            "HTTP_REQUESTS",
            "HTTP_ERRORS",
            "HTTP_ERROR_RATE",
            "HTTP_REQUEST_CONTENT_LENGTH",
            "HTTP_RESPONSE_CONTENT_LENGTH",
            "HTTP_ENDPOINT_RESPONSE_TIME",
            "HTTP_GATEWAY_RESPONSE_TIME",
            "HTTP_GATEWAY_LATENCY"
        );
    }

    @Test
    void should_add_the_six_llm_metrics_in_their_own_family_for_llm() {
        var definition = definitionOf(ApiType.LLM);

        assertThat(definition.families())
            .extracting(AnalyticsDefinition.FamilyMetrics::family)
            .containsExactly(MetricFamily.GATEWAY, MetricFamily.LLM);
        assertThat(definition.families().get(0).metrics()).hasSize(8);
        assertThat(definition.families().get(1).metrics())
            .extracting(MetricDefinition::name)
            .containsExactly(
                "LLM_PROMPT_TOKEN_SENT",
                "LLM_PROMPT_TOKEN_RECEIVED",
                "LLM_PROMPT_TOKEN_SENT_COST",
                "LLM_PROMPT_TOKEN_RECEIVED_COST",
                "LLM_PROMPT_TOTAL_TOKEN",
                "LLM_PROMPT_TOKEN_TOTAL_COST"
            );
    }

    @Test
    void should_serve_the_authz_family_with_its_facets_for_authz() {
        var definition = definitionOf(ApiType.AUTHZ);

        assertThat(definition.families()).extracting(AnalyticsDefinition.FamilyMetrics::family).containsExactly(MetricFamily.AUTHZ);
        assertThat(definition.families().get(0).metrics()).hasSize(8);
        assertThat(definition.dimensions()).extracting(DimensionDefinition::name).contains("AUTHZ_DECISION", "AUTHZ_PDP");
    }

    @Test
    void should_serve_the_kafka_service_family_with_its_facets_for_native() {
        var definition = definitionOf(ApiType.NATIVE);

        assertThat(definition.families()).extracting(AnalyticsDefinition.FamilyMetrics::family).containsExactly(MetricFamily.NATIVE);
        assertThat(definition.families().get(0).metrics()).hasSize(22);
        assertThat(definition.dimensions()).extracting(DimensionDefinition::name).contains("NATIVE_TOPIC", "NATIVE_CLIENT_ID");
    }

    /** No re-labelling and no re-listing: every field is the catalog's own, filters narrowed to the analytics ones. */
    @ParameterizedTest
    @EnumSource(value = ApiSpec.Name.class)
    void should_map_each_metric_from_the_catalog_as_is(ApiSpec.Name kind) {
        var metrics = adapter.metricsOf(ApiType.valueOf(kind.name()));

        assertThat(metrics)
            .extracting(MetricDefinition::name)
            .containsExactlyElementsOf(
                catalog
                    .getMetrics(kind)
                    .stream()
                    .map(spec -> spec.name().name())
                    .toList()
            );
        for (var metric : metrics) {
            var spec = catalog.findMetric(MetricSpec.Name.valueOf(metric.name())).orElseThrow();
            assertThat(metric.label()).isEqualTo(spec.label());
            assertThat(metric.unit()).isEqualTo(spec.unit().name());
            assertThat(metric.apiTypes()).extracting(Enum::name).containsExactlyElementsOf(names(spec.apis()));
            assertThat(metric.measures()).containsExactlyElementsOf(names(spec.measures()));
            assertThat(metric.filters()).containsExactlyElementsOf(
                catalog
                    .getFilters(spec.name())
                    .stream()
                    .map(f -> f.name().name())
                    .filter(analyticsFilterNames()::contains)
                    .toList()
            );
            assertThat(metric.facets()).containsExactlyElementsOf(
                catalog
                    .getFacets(spec.name())
                    .stream()
                    .map(f -> new DimensionDefinition(f.name().name(), f.label(), CatalogOrigin.BUILT_IN))
                    .toList()
            );
            assertThat(metric.origin()).isEqualTo(CatalogOrigin.BUILT_IN);
        }
    }

    /** A kind added to {@link ApiType} before the catalog knows it would be refused by the engine without a technical code. */
    @Test
    void should_find_every_api_kind_in_the_catalog() {
        assertThat(Arrays.stream(ApiSpec.Name.values()).map(Enum::name).toList()).containsAll(
            ApiType.API_KINDS.stream().map(Enum::name).toList()
        );
    }

    @Test
    void should_know_every_api_kind_of_the_catalog() {
        assertThat(Arrays.stream(ApiSpec.Name.values()).map(Enum::name).toList()).allSatisfy(name ->
            assertThat(ApiType.API_KINDS).contains(ApiType.valueOf(name))
        );
    }

    @Test
    void should_offer_every_facet_of_a_metric_as_a_labelled_dimension() {
        var definition = definitionOf(ApiType.HTTP_PROXY, ApiType.LLM, ApiType.MESSAGE, ApiType.NATIVE, ApiType.EDGE, ApiType.AUTHZ);

        assertThat(definition.dimensions()).allSatisfy(dimension -> assertThat(dimension.label()).isNotBlank());
        assertThat(definition.dimensions()).extracting(DimensionDefinition::name).doesNotHaveDuplicates();
    }

    @Test
    void should_leave_out_a_catalog_filter_the_analytics_endpoints_refuse() {
        assertThat(catalog.getFilters(MetricSpec.Name.HTTP_REQUESTS))
            .extracting(filter -> filter.name().name())
            .contains("HTTP_PATH_MAPPING");

        var httpRequests = adapter
            .metricsOf(ApiType.HTTP_PROXY)
            .stream()
            .filter(metric -> metric.name().equals("HTTP_REQUESTS"))
            .findFirst()
            .orElseThrow();

        assertThat(httpRequests.filters()).doesNotContain("HTTP_PATH_MAPPING").contains("API", "HTTP_STATUS");
        assertThat(httpRequests.facets()).extracting(DimensionDefinition::name).contains("HTTP_PATH_MAPPING");
    }

    /** Gamma checks a query's conditions against its own filter registry, not the engine catalog. */
    @Test
    void should_advertise_only_filters_the_analytics_endpoints_accept() {
        var validator = new ObservabilityFilterValidator(filterRegistry);
        var refused = new ArrayList<String>();
        for (var kind : ApiType.API_KINDS) {
            for (var metric : adapter.metricsOf(kind)) {
                for (var filter : metric.filters()) {
                    try {
                        validator.validate(List.of(aConditionOn(filter)), Signal.ANALYTICS);
                    } catch (UnsupportedObservabilityFilterException e) {
                        refused.add(kind + " " + metric.name() + " " + filter + ": " + e.getMessage());
                    }
                }
            }
        }

        assertThat(refused).isEmpty();
    }

    /** The engine checks a facet against the catalog the definition is read from. */
    @Test
    void should_advertise_only_facets_the_analytics_endpoints_accept() {
        var validator = new AnalyticsQueryValidator(catalog);
        var refused = new ArrayList<String>();
        for (var kind : ApiType.API_KINDS) {
            for (var metric : adapter.metricsOf(kind)) {
                for (var facet : metric.facets()) {
                    try {
                        validator.validateFacetsRequest(aFacetsRequest(metric, facet));
                    } catch (InvalidQueryException | IllegalArgumentException e) {
                        refused.add(kind + " " + metric.name() + " " + facet.name() + ": " + e.getMessage());
                    }
                }
            }
        }

        assertThat(refused).isEmpty();
    }

    private Set<String> analyticsFilterNames() {
        return filterRegistry.getFilters(Set.of(Signal.ANALYTICS), Set.of()).stream().map(FilterSpec::name).collect(Collectors.toSet());
    }

    private FilterCondition aConditionOn(String filterName) {
        return filterRegistry
            .getFilters(Set.of(), Set.of())
            .stream()
            .filter(spec -> spec.name().equals(filterName))
            .findFirst()
            .map(spec -> new FilterCondition(filterName, spec.operators().getFirst(), List.of(aValueOf(spec))))
            .orElseGet(() -> new FilterCondition(filterName, FilterOperator.EQ, List.of("value")));
    }

    private static String aValueOf(FilterSpec spec) {
        return switch (spec.type()) {
            case ENUM -> spec.enumValues() == null || spec.enumValues().isEmpty() ? "value" : spec.enumValues().getFirst().value();
            case NUMBER -> spec.range() == null || spec.range().min() == null ? "1" : String.valueOf(spec.range().min().longValue());
            case BOOLEAN -> "true";
            default -> "value";
        };
    }

    private static FacetsRequest aFacetsRequest(MetricDefinition metric, DimensionDefinition facet) {
        var measures = List.of(MetricSpec.Measure.valueOf(metric.measures().getFirst()));
        return new FacetsRequest(
            new TimeRange(Instant.EPOCH, Instant.EPOCH.plusSeconds(60)),
            null,
            List.of(new FacetMetricMeasuresRequest(MetricSpec.Name.valueOf(metric.name()), measures, null, null)),
            List.of(FacetSpec.Name.valueOf(facet.name())),
            null,
            null
        );
    }

    private AnalyticsDefinition definitionOf(ApiType... kinds) {
        return useCase.execute(new GetObservabilityAnalyticsDefinitionUseCase.Input(Set.of(kinds))).definition();
    }

    private static List<String> metricNames(AnalyticsDefinition definition) {
        return definition
            .families()
            .stream()
            .flatMap(family -> family.metrics().stream())
            .map(MetricDefinition::name)
            .toList();
    }

    private static List<String> names(List<? extends Enum<?>> values) {
        return values.stream().map(Enum::name).toList();
    }
}
