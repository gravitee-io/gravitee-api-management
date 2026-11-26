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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryValidator;
import io.gravitee.apim.core.analytics_engine.exception.InvalidQueryException;
import io.gravitee.apim.core.analytics_engine.model.AdditionalMetricName;
import io.gravitee.apim.core.analytics_engine.model.ApiSpec;
import io.gravitee.apim.core.analytics_engine.model.FacetMetricMeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.FacetName;
import io.gravitee.apim.core.analytics_engine.model.FacetSpec;
import io.gravitee.apim.core.analytics_engine.model.FacetsRequest;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.analytics_engine.model.TimeRange;
import io.gravitee.apim.core.analytics_engine.use_case.GetApiMetricSpecUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.GetMetricFacetSpecUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.GetMetricFilterSpecUseCase;
import io.gravitee.apim.infra.domain_service.analytics_engine.definition.AnalyticsDefinitionYAMLQueryService;
import io.gravitee.gamma.rest.core.observability.analytics.model.AnalyticsDefinition;
import io.gravitee.gamma.rest.core.observability.analytics.model.DimensionDefinition;
import io.gravitee.gamma.rest.core.observability.analytics.model.MetricDefinition;
import io.gravitee.gamma.rest.core.observability.analytics.model.MetricFamily;
import io.gravitee.gamma.rest.core.observability.analytics.port.AnalyticsDefinitionPortContractTest;
import io.gravitee.gamma.rest.core.observability.analytics.port.service_provider.AnalyticsDefinitionPort;
import io.gravitee.gamma.rest.core.observability.analytics.use_case.GetObservabilityAnalyticsDefinitionUseCase;
import io.gravitee.gamma.rest.core.observability.catalog.domain_service.AdditionalMetricCatalogDomainService;
import io.gravitee.gamma.rest.core.observability.catalog.domain_service.MetricDefinitionKeyDomainService;
import io.gravitee.gamma.rest.core.observability.catalog.inmemory.InMemoryAdditionalMetricDefinitionRepository;
import io.gravitee.gamma.rest.core.observability.catalog.inmemory.InMemoryCatalogPorts;
import io.gravitee.gamma.rest.core.observability.catalog.model.CatalogOrigin;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
        filterRegistry,
        AnalyticsEngineRouting.provider()
    );
    private final InMemoryCatalogPorts platform = new InMemoryCatalogPorts();
    private final GetObservabilityAnalyticsDefinitionUseCase useCase = new GetObservabilityAnalyticsDefinitionUseCase(
        adapter,
        new InMemoryAdditionalMetricDefinitionRepository(),
        platform,
        platform,
        platform,
        new AdditionalMetricCatalogDomainService(new MetricDefinitionKeyDomainService(new AdditionalMetricGrammarPortAdapter(), platform))
    );

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
    void should_serve_the_engine_units_in_the_gamma_vocabulary() {
        var units = new HashMap<String, String>();
        definitionOf(ApiType.LLM)
            .families()
            .forEach(family -> family.metrics().forEach(metric -> units.put(metric.name(), metric.unit())));

        assertThat(units)
            .containsEntry("HTTP_REQUESTS", "COUNT")
            .containsEntry("HTTP_GATEWAY_RESPONSE_TIME", "MILLISECONDS")
            .containsEntry("LLM_PROMPT_TOTAL_TOKEN", "TOKENS")
            .containsEntry("LLM_PROMPT_TOKEN_TOTAL_COST", "CURRENCY");
    }

    @Test
    void should_serve_the_platform_currency_with_each_llm_cost_and_with_no_token_count() {
        var currencies = new HashMap<String, String>();
        adapter.metricsOf(ApiType.LLM).forEach(metric -> currencies.put(metric.name(), metric.currency()));

        assertThat(currencies)
            .containsEntry("LLM_PROMPT_TOKEN_SENT_COST", "USD")
            .containsEntry("LLM_PROMPT_TOKEN_RECEIVED_COST", "USD")
            .containsEntry("LLM_PROMPT_TOKEN_TOTAL_COST", "USD")
            .containsEntry("LLM_PROMPT_TOTAL_TOKEN", null);
    }

    @Test
    void should_serve_the_edge_token_counts_in_tokens() {
        assertThat(adapter.metricsOf(ApiType.EDGE))
            .filteredOn(metric -> metric.name().startsWith("EDGE_TOKENS_"))
            .extracting(MetricDefinition::unit)
            .containsExactly("TOKENS", "TOKENS");
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

    /**
     * No re-listing: every field is the catalog's own, filters narrowed to the analytics ones, a facet labelled as the
     * Gamma filter of its name when there is one.
     */
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
            assertThat(metric.unit()).isEqualTo(ServedMetricUnit.of(spec.unit()));
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
                    .map(f ->
                        new DimensionDefinition(
                            f.name().name(),
                            filterLabels().getOrDefault(f.name().name(), f.label()),
                            null,
                            null,
                            expectedKinds(f.name().name()),
                            null,
                            CatalogOrigin.BUILT_IN
                        )
                    )
                    .toList()
            );
            assertThat(metric.origin()).isEqualTo(CatalogOrigin.BUILT_IN);
        }
    }

    /**
     * An additional metric is measured in the request documents: grouped and filtered like the built-in read there, and
     * served for every kind whose built-ins the engine searches in those documents, Edge included (OBS-70 §8).
     */
    @Test
    void should_serve_additional_metrics_as_the_engine_measures_them_like_the_built_in_whose_dimensions_they_take() {
        var dimensionsOf = adapter
            .metricsOf(ApiType.HTTP_PROXY)
            .stream()
            .filter(metric -> metric.name().equals(AdditionalMetricName.DIMENSIONS_OF.name()))
            .findFirst()
            .orElseThrow();
        var routing = AnalyticsEngineRouting.provider();

        var support = adapter.additionalMetricSupport();

        assertThat(support.apiTypes()).containsExactly(
            ApiType.HTTP_PROXY,
            ApiType.MESSAGE,
            ApiType.LLM,
            ApiType.MCP,
            ApiType.A2A,
            ApiType.EDGE
        );
        assertThat(support.dimensionsOf()).isEqualTo(dimensionsOf.name());
        for (var kind : ApiSpec.Name.values()) {
            var measuredThere = catalog
                .getMetrics(kind)
                .stream()
                .anyMatch(spec -> routing.measuresAdditionalMetricsLike(spec.name()));
            assertThat(support.apiTypes().contains(ApiType.valueOf(kind.name()))).as(kind.name()).isEqualTo(measuredThere);
        }
        assertThat(support.facets()).containsExactlyElementsOf(dimensionsOf.facets());
        assertThat(support.filters()).containsExactlyElementsOf(dimensionsOf.filters());
        assertThat(support.measures()).containsExactly("AVG", "SUM", "MIN", "MAX", "P50", "P90", "P95", "P99", "COUNT");
    }

    /**
     * The decision's list (OBS-171, "Contract answers to the OBS-175 outline"): every built-in of the HTTP request
     * documents takes the environment's keyword facets, custom ones included, and the engine accepts each of them.
     */
    @Test
    void should_add_the_dynamic_facets_to_exactly_the_built_ins_the_engine_groups_by_additional_metric_fields() {
        var support = adapter.additionalMetricSupport();
        var routing = AnalyticsEngineRouting.provider();
        var fraudTier = (FacetName) FacetName.parse("custom.fraud-tier").orElseThrow();

        assertThat(support.groupedByFields()).containsExactlyInAnyOrder(
            "HTTP_REQUESTS",
            "HTTP_ERRORS",
            "HTTP_ERROR_RATE",
            "HTTP_REQUEST_CONTENT_LENGTH",
            "HTTP_RESPONSE_CONTENT_LENGTH",
            "HTTP_ENDPOINT_RESPONSE_TIME",
            "HTTP_GATEWAY_RESPONSE_TIME",
            "HTTP_GATEWAY_LATENCY",
            "LLM_PROMPT_TOKEN_SENT",
            "LLM_PROMPT_TOKEN_RECEIVED",
            "LLM_PROMPT_TOKEN_SENT_COST",
            "LLM_PROMPT_TOKEN_RECEIVED_COST",
            "LLM_PROMPT_TOTAL_TOKEN",
            "LLM_PROMPT_TOKEN_TOTAL_COST",
            "EDGE_DETECTION_COUNT",
            "EDGE_TOKENS_IN",
            "EDGE_TOKENS_OUT",
            "EDGE_HEARTBEAT_COUNT"
        );
        assertThat(support.groupedByFields()).allSatisfy(metric ->
            assertThatCode(() -> routing.resolve(groupedBy(MetricSpec.Name.valueOf(metric), fraudTier)))
                .as(metric)
                .doesNotThrowAnyException()
        );
        var notAdvertised = Arrays.stream(ApiSpec.Name.values())
            .flatMap(kind -> catalog.getMetrics(kind).stream())
            .map(MetricSpec::name)
            .filter(metric -> !support.groupedByFields().contains(metric.name()))
            .distinct()
            .toList();
        assertThat(notAdvertised).isNotEmpty().contains(MetricSpec.Name.MESSAGES);
        assertThat(notAdvertised).allSatisfy(metric ->
            assertThatThrownBy(() -> routing.resolve(groupedBy(metric, fraudTier)))
                .as(metric.name())
                .isInstanceOf(InvalidQueryException.class)
        );
    }

    private static FacetsRequest groupedBy(MetricSpec.Name metric, FacetName facet) {
        return new FacetsRequest(
            new TimeRange(Instant.EPOCH, Instant.EPOCH.plusSeconds(60)),
            List.of(),
            List.of(new FacetMetricMeasuresRequest(metric, List.of(MetricSpec.Measure.COUNT), List.of())),
            List.of(facet),
            null,
            List.of()
        );
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

    /**
     * A dimension is offered for the kinds it applies to: those of the catalog metrics grouped by it, the facets a
     * metric accepts saying where the engine groups by it. {@code HTTP_STATUS} is not an Edge dimension though Edge
     * additional metrics are grouped by it, and {@code GATEWAY} serves the Message and AuthZ screens.
     */
    @Test
    void should_offer_a_built_in_dimension_for_the_kinds_of_the_catalog_metrics_grouped_by_it() {
        var kindsByDimension = kindsByDimension(definitionOf(ApiType.API_KINDS.toArray(ApiType[]::new)));

        assertThat(kindsByDimension).allSatisfy((name, kinds) -> assertThat(kinds).as(name).isEqualTo(expectedKinds(name)));
        assertThat(kindsByDimension.get("HTTP_STATUS")).containsExactlyInAnyOrderElementsOf(HTTP_KINDS);
        assertThat(kindsByDimension.get("API")).containsExactlyInAnyOrderElementsOf(ApiType.API_KINDS);
        assertThat(kindsByDimension.get("GATEWAY")).contains(ApiType.MESSAGE, ApiType.AUTHZ, ApiType.EDGE);
        assertThat(kindsByDimension.get("AUTHZ_PDP")).containsExactly(ApiType.AUTHZ);
        assertThat(kindsByDimension.get("NATIVE_TOPIC")).containsExactly(ApiType.NATIVE);
        assertThat(kindsByDimension.values()).allSatisfy(kinds -> assertThat(kinds).isNotEmpty().doesNotContain(ApiType.AUTHZ_DECISION));
    }

    /**
     * Every HTTP connection document carries the LLM and MCP proxy fields and the API product, but they describe LLM,
     * MCP and product traffic: those dimensions are offered for the kinds their filter declares, as the filter bar
     * offers the filters.
     */
    @Test
    void should_offer_the_kind_specific_fields_for_the_kinds_their_filter_declares() {
        var kindsByDimension = kindsByDimension(definitionOf(ApiType.API_KINDS.toArray(ApiType[]::new)));
        var filterKinds = analyticsFilterKinds();

        assertThat(kindsByDimension).containsAllEntriesOf(KIND_SPECIFIC);
        KIND_SPECIFIC.keySet().forEach(name -> {
            assertThat(filterKinds.get(name)).as(name).isEqualTo(KIND_SPECIFIC.get(name));
            assertThat(kindsListingEachFacet().get(name))
                .as("the catalog metrics grouped by " + name + ", wider than its filter")
                .containsAll(KIND_SPECIFIC.get(name))
                .hasSizeGreaterThan(KIND_SPECIFIC.get(name).size());
        });
        assertThat(kindsByDimension.get("LLM_PROXY_MODEL")).doesNotContain(ApiType.HTTP_PROXY);
    }

    /**
     * A filter's kinds only narrow a dimension: widening a kind-specific filter must not offer the dimension for a kind
     * no catalog metric is grouped by it for.
     */
    @Test
    void should_never_offer_a_built_in_dimension_for_a_kind_no_catalog_metric_groups_by_it_for() {
        var kindsListing = kindsListingEachFacet();

        assertThat(kindsByDimension(definitionOf(ApiType.API_KINDS.toArray(ApiType[]::new)))).allSatisfy((name, kinds) ->
            assertThat(kinds).as(name).isSubsetOf(kindsListing.get(name))
        );
    }

    /** The kinds are the dimension's own: asking for other kinds or origins never changes them. */
    @Test
    void should_offer_a_built_in_dimension_for_the_same_kinds_whatever_kinds_and_origins_are_asked() {
        var everyKind = kindsByDimension(definitionOf(ApiType.API_KINDS.toArray(ApiType[]::new)));

        for (var kind : ApiType.API_KINDS) {
            for (var origins : List.of(Set.<CatalogOrigin>of(), Set.of(CatalogOrigin.BUILT_IN))) {
                var definition = useCase
                    .execute(new GetObservabilityAnalyticsDefinitionUseCase.Input("DEFAULT", "DEFAULT", Set.of(kind), origins))
                    .definition();
                assertThat(kindsByDimension(definition))
                    .as(kind + " " + origins)
                    .allSatisfy((name, kinds) -> assertThat(kinds).as(name).isEqualTo(everyKind.get(name)));
            }
        }
    }

    @Test
    void should_offer_every_facet_of_a_metric_as_a_labelled_dimension() {
        var definition = definitionOf(ApiType.HTTP_PROXY, ApiType.LLM, ApiType.MESSAGE, ApiType.NATIVE, ApiType.EDGE, ApiType.AUTHZ);

        assertThat(definition.dimensions()).allSatisfy(dimension -> assertThat(dimension.label()).isNotBlank());
        assertThat(definition.dimensions()).extracting(DimensionDefinition::name).doesNotHaveDuplicates();
    }

    @Test
    void should_leave_out_a_catalog_filter_the_analytics_endpoints_refuse() {
        assertThat(catalog.getFilters(MetricSpec.Name.MESSAGE_PAYLOAD_SIZE))
            .extracting(filter -> filter.name().name())
            .contains("MESSAGE_CONNECTOR_ID");
        assertThat(analyticsFilterNames()).doesNotContain("MESSAGE_CONNECTOR_ID");

        var payloadSize = metricOf(ApiType.MESSAGE, "MESSAGE_PAYLOAD_SIZE");

        assertThat(payloadSize.filters()).doesNotContain("MESSAGE_CONNECTOR_ID").contains("API");
    }

    @Test
    void should_offer_the_path_mapping_as_a_filter_and_a_facet_under_the_gamma_label() {
        for (var kind : List.of(ApiType.HTTP_PROXY, ApiType.LLM, ApiType.MCP, ApiType.A2A)) {
            var httpRequests = metricOf(kind, "HTTP_REQUESTS");

            assertThat(httpRequests.filters()).as("filters of HTTP_REQUESTS on %s", kind).contains("HTTP_PATH_MAPPING");
            assertThat(httpRequests.facets())
                .as("facets of HTTP_REQUESTS on %s", kind)
                .filteredOn(dimension -> dimension.name().equals("HTTP_PATH_MAPPING"))
                .extracting(DimensionDefinition::label)
                .containsExactly("Path mapping");
        }
    }

    /** Why the filter is not offered on AuthZ: no AuthZ metric of the catalog accepts it. */
    @Test
    void should_not_find_the_path_mapping_among_the_filters_of_any_authz_metric() {
        var authzMetrics = adapter.metricsOf(ApiType.AUTHZ);

        assertThat(authzMetrics).isNotEmpty();
        assertThat(authzMetrics).allSatisfy(metric ->
            assertThat(catalog.getFilters(MetricSpec.Name.valueOf(metric.name())))
                .as("catalog filters of %s", metric.name())
                .extracting(filter -> filter.name().name())
                .doesNotContain("HTTP_PATH_MAPPING")
        );
    }

    private MetricDefinition metricOf(ApiType kind, String name) {
        return adapter
            .metricsOf(kind)
            .stream()
            .filter(metric -> metric.name().equals(name))
            .findFirst()
            .orElseThrow();
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

    /** The kinds each filter of the {@code ANALYTICS} signal is tied to, as the registry serves them. */
    private Map<String, Set<ApiType>> analyticsFilterKinds() {
        return filterRegistry
            .getFilters(Set.of(Signal.ANALYTICS), Set.of())
            .stream()
            .collect(Collectors.toMap(FilterSpec::name, spec -> spec.apiTypes() == null ? Set.of() : spec.apiTypes()));
    }

    /** The kinds the facets of the 7 kind-specific fields are offered for, those their filter declares. */
    private static final Map<String, Set<ApiType>> KIND_SPECIFIC = Map.of(
        "LLM_PROXY_MODEL",
        Set.of(ApiType.LLM),
        "LLM_PROXY_PROVIDER",
        Set.of(ApiType.LLM),
        "MCP_PROXY_METHOD",
        Set.of(ApiType.MCP),
        "MCP_PROXY_TOOL",
        Set.of(ApiType.MCP),
        "MCP_PROXY_RESOURCE",
        Set.of(ApiType.MCP),
        "MCP_PROXY_PROMPT",
        Set.of(ApiType.MCP),
        "API_PRODUCT",
        Set.of(ApiType.A2A, ApiType.HTTP_PROXY, ApiType.LLM, ApiType.MCP)
    );

    private static final List<ApiType> HTTP_KINDS = List.of(ApiType.A2A, ApiType.HTTP_PROXY, ApiType.LLM, ApiType.MCP, ApiType.MESSAGE);

    /** Read from the catalog itself: the kinds of every catalog metric grouped by each facet. */
    private Map<String, Set<ApiType>> kindsListingEachFacet() {
        Map<String, Set<ApiType>> kinds = new HashMap<>();
        for (var kind : ApiSpec.Name.values()) {
            for (var spec : catalog.getMetrics(kind)) {
                for (var facet : catalog.getFacets(spec.name())) {
                    kinds
                        .computeIfAbsent(facet.name().name(), name -> new HashSet<>())
                        .addAll(
                            spec
                                .apis()
                                .stream()
                                .map(api -> ApiType.valueOf(api.name()))
                                .toList()
                        );
                }
            }
        }
        return kinds;
    }

    /** The labels of the Gamma filters, which a dimension of the same name takes. */
    private Map<String, String> filterLabels() {
        return filterRegistry.getFilters(Set.of(), Set.of()).stream().collect(Collectors.toMap(FilterSpec::name, FilterSpec::label));
    }

    private Set<ApiType> expectedKinds(String facet) {
        return KIND_SPECIFIC.getOrDefault(facet, kindsListingEachFacet().get(facet));
    }

    private static Map<String, Set<ApiType>> kindsByDimension(AnalyticsDefinition definition) {
        return definition
            .dimensions()
            .stream()
            .filter(dimension -> dimension.origin() == CatalogOrigin.BUILT_IN)
            .collect(Collectors.toMap(DimensionDefinition::name, DimensionDefinition::apiTypes));
    }

    private AnalyticsDefinition definitionOf(ApiType... kinds) {
        return useCase.execute(new GetObservabilityAnalyticsDefinitionUseCase.Input("DEFAULT", "DEFAULT", Set.of(kinds))).definition();
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
