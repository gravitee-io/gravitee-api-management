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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.analytics.domain_service.AiWorkspaceAnalyticsApisDomainService;
import io.gravitee.apim.core.analytics.model.ApiDataKind;
import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryValidator;
import io.gravitee.apim.core.analytics_engine.model.FacetBucketResponse;
import io.gravitee.apim.core.analytics_engine.model.FacetMetricMeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.FacetsRequest;
import io.gravitee.apim.core.analytics_engine.model.FacetsResponse;
import io.gravitee.apim.core.analytics_engine.model.Filter;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.Measure;
import io.gravitee.apim.core.analytics_engine.model.MeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.MeasuresResponse;
import io.gravitee.apim.core.analytics_engine.model.MetricFacetsResponse;
import io.gravitee.apim.core.analytics_engine.model.MetricMeasuresResponse;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesBucketResponse;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesMetricResponse;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesRequest;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesResponse;
import io.gravitee.apim.core.analytics_engine.service_provider.AnalyticsQueryContextProvider;
import io.gravitee.apim.core.analytics_engine.use_case.ComputeFacetsUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.ComputeMeasuresUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.ComputeTimeSeriesUseCase;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.user.domain_service.UserContextLoader;
import io.gravitee.apim.core.user.model.UserContext;
import io.gravitee.apim.infra.domain_service.analytics_engine.definition.AnalyticsDefinitionYAMLQueryService;
import io.gravitee.apim.infra.domain_service.analytics_engine.processors.UnitEnrichmentPostProcessorImpl;
import io.gravitee.gamma.rest.core.observability.analytics.model.AnalyticsFacetMetricQuery;
import io.gravitee.gamma.rest.core.observability.analytics.model.AnalyticsMetricQuery;
import io.gravitee.gamma.rest.core.observability.analytics.model.AnalyticsNumberRange;
import io.gravitee.gamma.rest.core.observability.analytics.model.AnalyticsSortSpec;
import io.gravitee.gamma.rest.core.observability.analytics.model.DataKind;
import io.gravitee.gamma.rest.core.observability.analytics.model.SeriesRanking;
import io.gravitee.gamma.rest.core.observability.analytics.port.service_provider.ObservabilityAnalyticsDataPort.FacetsQuery;
import io.gravitee.gamma.rest.core.observability.analytics.port.service_provider.ObservabilityAnalyticsDataPort.MeasuresQuery;
import io.gravitee.gamma.rest.core.observability.analytics.port.service_provider.ObservabilityAnalyticsDataPort.TimeSeriesQuery;
import io.gravitee.gamma.rest.core.observability.analytics.use_case.AnalyticsRequestPipeline.PreparedScope;
import io.gravitee.gamma.rest.core.observability.exception.InvalidObservabilityQueryException;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterCondition;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterOperator;
import io.gravitee.gamma.rest.core.observability.logs.model.TrafficScope;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ObservabilityAnalyticsDataPortAdapterTest {

    private static final String ORG = "org-1";
    private static final String ENV = "env-1";
    private static final PreparedScope SCOPE = new PreparedScope(
        Instant.parse("2026-06-10T00:00:00Z"),
        Instant.parse("2026-06-11T00:00:00Z"),
        List.of(new FilterCondition("API", FilterOperator.IN, List.of("api-1"))),
        Set.of("api-1")
    );

    @Mock
    private ComputeMeasuresUseCase computeMeasuresUseCase;

    @Mock
    private ComputeFacetsUseCase computeFacetsUseCase;

    @Mock
    private ComputeTimeSeriesUseCase computeTimeSeriesUseCase;

    @Mock
    private UserContextLoader userContextLoader;

    @Mock
    private AiWorkspaceAnalyticsApisDomainService workspaceApis;

    @Mock
    private AnalyticsQueryContextProvider queryContextProvider;

    private ObservabilityAnalyticsDataPortAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ObservabilityAnalyticsDataPortAdapter(
            computeMeasuresUseCase,
            computeFacetsUseCase,
            computeTimeSeriesUseCase,
            new AnalyticsQueryValidator(new AnalyticsDefinitionYAMLQueryService()),
            new UnitEnrichmentPostProcessorImpl(new AnalyticsDefinitionYAMLQueryService()),
            userContextLoader,
            workspaceApis,
            queryContextProvider,
            new ObjectMapper()
        );
    }

    @Test
    void should_map_an_authz_api_to_the_authz_gamma_api_type() {
        when(userContextLoader.loadApis(any(), eq(ApiDataKind.AGGREGATES))).thenAnswer(invocation ->
            ((UserContext) invocation.getArgument(0)).withApis(
                List.of(Api.builder().id("api-1").name("Authz API").type(io.gravitee.definition.model.v4.ApiType.AUTHZ).build())
            )
        );

        var accessibleApis = adapter.loadAccessibleApis(ORG, ENV, DataKind.AGGREGATES);

        assertThat(accessibleApis).hasSize(1);
        assertThat(accessibleApis.getFirst().type()).isEqualTo(ApiType.AUTHZ);
    }

    @Test
    void should_load_the_apis_whose_requests_the_caller_reads_for_per_request_data() {
        when(userContextLoader.loadApis(any(), eq(ApiDataKind.PER_REQUEST))).thenAnswer(invocation ->
            ((UserContext) invocation.getArgument(0)).withApis(
                List.of(Api.builder().id("api-1").name("Logged API").type(io.gravitee.definition.model.v4.ApiType.PROXY).build())
            )
        );

        var accessibleApis = adapter.loadAccessibleApis(ORG, ENV, DataKind.PER_REQUEST);

        assertThat(accessibleApis).hasSize(1);
        assertThat(accessibleApis.getFirst().id()).isEqualTo("api-1");
    }

    @Test
    void should_load_the_apis_of_the_workspace_the_caller_reads() {
        when(workspaceApis.findWorkspaceApis(any(), any(), eq("workspace"))).thenReturn(
            List.of(Api.builder().id("llm").name("LLM").type(io.gravitee.definition.model.v4.ApiType.LLM_PROXY).build())
        );

        var apis = adapter.loadWorkspaceApis(ORG, ENV, "workspace");

        assertThat(apis).hasSize(1);
        assertThat(apis.getFirst().id()).isEqualTo("llm");
        assertThat(apis.getFirst().type()).isEqualTo(ApiType.LLM);
    }

    @Test
    void should_narrow_by_the_api_product_only_when_the_engine_narrows_every_metric_by_it() {
        when(queryContextProvider.narrowsBy(MetricSpec.Name.HTTP_REQUESTS, FilterSpec.Name.API_PRODUCT)).thenReturn(true);
        when(queryContextProvider.narrowsBy(MetricSpec.Name.AUTHZ_DECISIONS, FilterSpec.Name.API_PRODUCT)).thenReturn(false);

        assertThat(adapter.narrowsByApiProduct(List.of("HTTP_REQUESTS"))).isTrue();
        assertThat(adapter.narrowsByApiProduct(List.of("HTTP_REQUESTS", "AUTHZ_DECISIONS"))).isFalse();
        assertThat(adapter.narrowsByApiProduct(List.of())).isFalse();
    }

    @Nested
    class UnknownQueryNames {

        @Test
        void should_reject_an_unknown_metric_without_querying_the_engine() {
            var query = measuresQuery(SCOPE, new AnalyticsMetricQuery(null, "HTTP_NOPE", List.of("COUNT"), List.of()));

            assertThatThrownBy(() -> adapter.computeMeasures(query))
                .isInstanceOf(InvalidObservabilityQueryException.class)
                .hasMessageContaining("HTTP_NOPE")
                .extracting("technicalCode")
                .isEqualTo("observability.query.unknown_metric");
            verifyNoInteractions(computeMeasuresUseCase);
        }

        @Test
        void should_reject_a_metric_without_a_name() {
            var query = measuresQuery(SCOPE, new AnalyticsMetricQuery(null, null, List.of("COUNT"), List.of()));

            assertThatThrownBy(() -> adapter.computeMeasures(query))
                .isInstanceOf(InvalidObservabilityQueryException.class)
                .extracting("technicalCode")
                .isEqualTo("observability.query.unknown_metric");
        }

        @Test
        void should_reject_an_unknown_measure() {
            var query = measuresQuery(SCOPE, new AnalyticsMetricQuery(null, "HTTP_REQUESTS", List.of("MEDIAN"), List.of()));

            assertThatThrownBy(() -> adapter.computeMeasures(query))
                .isInstanceOf(InvalidObservabilityQueryException.class)
                .hasMessageContaining("MEDIAN")
                .extracting("technicalCode")
                .isEqualTo("observability.query.unknown_measure");
        }

        @Test
        void should_reject_an_unknown_facet() {
            var query = facetsQuery(List.of("HTTP_NOPE"), List.of());

            assertThatThrownBy(() -> adapter.computeFacets(query))
                .isInstanceOf(InvalidObservabilityQueryException.class)
                .hasMessageContaining("HTTP_NOPE")
                .extracting("technicalCode")
                .isEqualTo("observability.query.unknown_facet");
            verifyNoInteractions(computeFacetsUseCase);
        }

        @Test
        void should_reject_an_unknown_sort_measure() {
            var query = facetsQuery(List.of("HTTP_STATUS"), List.of(new AnalyticsSortSpec("MEDIAN", "DESC")));

            assertThatThrownBy(() -> adapter.computeFacets(query))
                .isInstanceOf(InvalidObservabilityQueryException.class)
                .extracting("technicalCode")
                .isEqualTo("observability.query.unknown_measure");
        }

        @Test
        void should_reject_an_unknown_sort_order() {
            var query = facetsQuery(List.of("HTTP_STATUS"), List.of(new AnalyticsSortSpec("COUNT", "SIDEWAYS")));

            assertThatThrownBy(() -> adapter.computeFacets(query))
                .isInstanceOf(InvalidObservabilityQueryException.class)
                .hasMessageContaining("SIDEWAYS")
                .extracting("technicalCode")
                .isEqualTo("observability.query.unknown_sort_order");
        }

        @Test
        void should_read_the_sort_order_case_insensitively() {
            when(computeFacetsUseCase.executeForApis(any(), any(), any())).thenReturn(
                new ComputeFacetsUseCase.Output(new FacetsResponse(List.of()))
            );

            adapter.computeFacets(facetsQuery(List.of("HTTP_STATUS"), List.of(new AnalyticsSortSpec("COUNT", "desc"))));

            var captor = ArgumentCaptor.forClass(FacetsRequest.class);
            verify(computeFacetsUseCase).executeForApis(any(), captor.capture(), any());
            assertThat(captor.getValue().metrics().getFirst().sorts().getFirst().order()).isEqualTo(
                FacetMetricMeasuresRequest.Sort.Order.DESC
            );
        }

        private static FacetsQuery facetsQuery(List<String> facets, List<AnalyticsSortSpec> sorts) {
            return new FacetsQuery(
                ORG,
                ENV,
                SCOPE,
                facets,
                null,
                List.of(new AnalyticsFacetMetricQuery("HTTP_REQUESTS", List.of("COUNT"), sorts, List.of())),
                List.of(),
                Map.of()
            );
        }
    }

    @Nested
    class IgnoredFilters {

        private static final List<FilterSpec.Name> SKIPPED_BY_THE_ENGINE = List.of(FilterSpec.Name.NATIVE_CLIENT_ID);

        @Test
        void should_list_what_gamma_did_not_apply_after_what_the_engine_skipped() {
            when(computeMeasuresUseCase.executeForApis(any(), any(), any())).thenReturn(
                new ComputeMeasuresUseCase.Output(
                    new MeasuresResponse(
                        List.of(
                            new MetricMeasuresResponse(MetricSpec.Name.HTTP_REQUESTS, null, List.of(), SKIPPED_BY_THE_ENGINE),
                            new MetricMeasuresResponse(MetricSpec.Name.HTTP_ERRORS, null, List.of(), List.of())
                        )
                    )
                )
            );

            var response = adapter.computeMeasures(
                new MeasuresQuery(
                    ORG,
                    ENV,
                    List.of(SCOPE),
                    List.of(
                        new MeasuresQuery.Metric(
                            new AnalyticsMetricQuery(null, "HTTP_REQUESTS", List.of("COUNT"), List.of()),
                            0,
                            List.of("RECORD_TYPE")
                        ),
                        new MeasuresQuery.Metric(new AnalyticsMetricQuery(null, "HTTP_ERRORS", List.of("COUNT"), List.of()), 0, List.of())
                    )
                )
            );

            assertThat(response.at("/metrics/0/ignoredFilters").toString()).isEqualTo("[\"NATIVE_CLIENT_ID\",\"RECORD_TYPE\"]");
            assertThat(response.at("/metrics/1").has("ignoredFilters")).as("nothing skipped, no field").isFalse();
        }

        @Test
        void should_merge_what_gamma_did_not_apply_on_a_metric_queried_twice_without_ids() {
            when(computeMeasuresUseCase.executeForApis(any(), any(), any())).thenReturn(
                new ComputeMeasuresUseCase.Output(
                    new MeasuresResponse(List.of(new MetricMeasuresResponse(MetricSpec.Name.HTTP_REQUESTS, null, List.of(), List.of())))
                )
            );

            var response = adapter.computeMeasures(
                new MeasuresQuery(
                    ORG,
                    ENV,
                    List.of(SCOPE),
                    List.of(
                        new MeasuresQuery.Metric(
                            new AnalyticsMetricQuery(null, "HTTP_REQUESTS", List.of("COUNT"), List.of()),
                            0,
                            List.of("HTTP_STATUS")
                        ),
                        new MeasuresQuery.Metric(
                            new AnalyticsMetricQuery(null, "HTTP_REQUESTS", List.of("AVG"), List.of()),
                            0,
                            List.of("PLAN", "HTTP_STATUS")
                        )
                    )
                )
            );

            assertThat(response.get("metrics")).hasSize(1);
            assertThat(response.at("/metrics/0/ignoredFilters").toString()).isEqualTo("[\"HTTP_STATUS\",\"PLAN\"]");
        }

        // The use case moves them into the query's scope; the engine's per-metric filters are never used.
        @Test
        void should_never_hand_metric_level_conditions_to_the_engine_per_metric() {
            when(computeFacetsUseCase.executeForApis(any(), any(), any())).thenReturn(
                new ComputeFacetsUseCase.Output(
                    new FacetsResponse(List.of(new MetricFacetsResponse(MetricSpec.Name.HTTP_REQUESTS, null, List.of(), List.of())))
                )
            );
            var plan = new FilterCondition("PLAN", FilterOperator.EQ, List.of("plan-1"));

            var response = adapter.computeFacets(
                new FacetsQuery(
                    ORG,
                    ENV,
                    SCOPE,
                    List.of("HTTP_STATUS"),
                    null,
                    List.of(new AnalyticsFacetMetricQuery("HTTP_REQUESTS", List.of("COUNT"), List.of(), List.of(plan))),
                    List.of(),
                    Map.of("HTTP_REQUESTS", List.of("RECORD_TYPE"))
                )
            );

            var captor = ArgumentCaptor.forClass(FacetsRequest.class);
            verify(computeFacetsUseCase).executeForApis(any(), captor.capture(), any());
            assertThat(captor.getValue().metrics().getFirst().filters()).isNullOrEmpty();
            assertThat(response.at("/metrics/0/ignoredFilters").toString()).isEqualTo("[\"RECORD_TYPE\"]");
        }
    }

    @Nested
    class MeasuresGroups {

        private static final PreparedScope ERRORS_SCOPE = new PreparedScope(
            SCOPE.from(),
            SCOPE.to(),
            List.of(
                new FilterCondition("HTTP_STATUS", FilterOperator.GTE, List.of("500")),
                new FilterCondition("API", FilterOperator.IN, List.of("api-1"))
            ),
            Set.of("api-1")
        );

        @Test
        void should_hand_every_condition_on_one_field_to_the_engine() {
            when(computeMeasuresUseCase.executeForApis(any(), any(), any())).thenReturn(
                new ComputeMeasuresUseCase.Output(new MeasuresResponse(List.of()))
            );
            var scope = new PreparedScope(
                SCOPE.from(),
                SCOPE.to(),
                List.of(
                    new FilterCondition("HTTP_STATUS", FilterOperator.EQ, List.of("200")),
                    new FilterCondition("HTTP_STATUS", FilterOperator.EQ, List.of("500")),
                    new FilterCondition("API", FilterOperator.IN, List.of("api-1"))
                ),
                Set.of("api-1")
            );

            adapter.computeMeasures(measuresQuery(scope, new AnalyticsMetricQuery(null, "HTTP_REQUESTS", List.of("COUNT"), List.of())));

            var captor = ArgumentCaptor.forClass(MeasuresRequest.class);
            verify(computeMeasuresUseCase).executeForApis(any(), captor.capture(), any());
            assertThat(captor.getValue().filters())
                .filteredOn(filter -> filter.name() == FilterSpec.Name.HTTP_STATUS)
                .extracting(Filter::value)
                .containsExactly("200", "500");
        }

        @Test
        void should_hand_the_traffic_scope_to_the_engine_with_every_readable_api_authorized() {
            when(computeMeasuresUseCase.executeForApis(any(), any(), any())).thenReturn(
                new ComputeMeasuresUseCase.Output(new MeasuresResponse(List.of()))
            );
            var aim = new PreparedScope(
                SCOPE.from(),
                SCOPE.to(),
                List.of(new FilterCondition("API", FilterOperator.IN, List.of("studio", "orders"))),
                Set.of("studio", "orders"),
                new TrafficScope(Set.of("studio"), Set.of("mcp"))
            );

            adapter.computeMeasures(measuresQuery(aim, new AnalyticsMetricQuery(null, "HTTP_REQUESTS", List.of("COUNT"), List.of())));

            var captor = ArgumentCaptor.forClass(MeasuresRequest.class);
            verify(computeMeasuresUseCase).executeForApis(any(), captor.capture(), eq(Set.of("studio", "orders")));
            assertThat(captor.getValue().filters())
                .filteredOn(filter -> filter.name() == FilterSpec.Name.TRAFFIC_SCOPE)
                .extracting(Filter::value)
                .containsExactly(new io.gravitee.apim.core.analytics_engine.model.TrafficScope(Set.of("studio"), Set.of("mcp")));
        }

        // Gamma's readable APIs come from the same rule as the engine's context: reloading it per group
        // would repeat a repository search for the same answer.
        @Test
        void should_authorize_each_group_query_on_its_own_scope_without_reloading_the_engine_context() {
            when(computeMeasuresUseCase.executeForApis(any(), any(), any())).thenReturn(
                new ComputeMeasuresUseCase.Output(new MeasuresResponse(List.of()))
            );
            var otherApi = new PreparedScope(
                SCOPE.from(),
                SCOPE.to(),
                List.of(new FilterCondition("API", FilterOperator.IN, List.of("api-2"))),
                Set.of("api-2")
            );

            adapter.computeMeasures(
                new MeasuresQuery(
                    ORG,
                    ENV,
                    List.of(SCOPE, otherApi),
                    List.of(
                        new MeasuresQuery.Metric(new AnalyticsMetricQuery("a", "HTTP_REQUESTS", List.of("COUNT"), List.of()), 0, List.of()),
                        new MeasuresQuery.Metric(new AnalyticsMetricQuery("b", "HTTP_REQUESTS", List.of("COUNT"), List.of()), 1, List.of())
                    )
                )
            );

            verify(computeMeasuresUseCase).executeForApis(any(), any(), eq(Set.of("api-1")));
            verify(computeMeasuresUseCase).executeForApis(any(), any(), eq(Set.of("api-2")));
            verify(computeMeasuresUseCase, never()).execute(any());
        }

        @Test
        void should_run_one_engine_query_per_group_and_answer_each_metric_with_its_id_in_request_order() {
            when(computeMeasuresUseCase.executeForApis(any(), any(), any())).thenReturn(
                output(new MetricMeasuresResponse(MetricSpec.Name.HTTP_REQUESTS, null, List.of(count(1000)), List.of())),
                output(
                    new MetricMeasuresResponse(
                        MetricSpec.Name.HTTP_REQUESTS,
                        null,
                        List.of(count(42)),
                        List.of(FilterSpec.Name.NATIVE_CLIENT_ID)
                    )
                )
            );

            var response = adapter.computeMeasures(
                new MeasuresQuery(
                    ORG,
                    ENV,
                    List.of(SCOPE, ERRORS_SCOPE),
                    List.of(
                        new MeasuresQuery.Metric(
                            new AnalyticsMetricQuery("errors", "HTTP_REQUESTS", List.of("COUNT"), List.of()),
                            1,
                            List.of()
                        ),
                        new MeasuresQuery.Metric(
                            new AnalyticsMetricQuery("total", "HTTP_REQUESTS", List.of("COUNT"), List.of()),
                            0,
                            List.of()
                        )
                    )
                )
            );

            var captor = ArgumentCaptor.forClass(MeasuresRequest.class);
            verify(computeMeasuresUseCase, times(2)).executeForApis(any(), captor.capture(), any());
            assertThat(captor.getAllValues().get(1).filters()).anyMatch(filter -> filter.name() == FilterSpec.Name.HTTP_STATUS);
            assertThat(response.get("metrics")).hasSize(2);
            assertThat(response.at("/metrics/0/id").asText()).isEqualTo("errors");
            assertThat(response.at("/metrics/0/measures/0/value").asInt()).isEqualTo(42);
            assertThat(response.at("/metrics/0/ignoredFilters").toString()).isEqualTo("[\"NATIVE_CLIENT_ID\"]");
            assertThat(response.at("/metrics/1/id").asText()).isEqualTo("total");
            assertThat(response.at("/metrics/1/measures/0/value").asInt()).isEqualTo(1000);
            assertThat(response.at("/metrics/1").has("ignoredFilters")).as("skipped on the other group only").isFalse();
        }

        @Test
        void should_keep_only_its_own_measures_on_each_identified_metric() {
            when(computeMeasuresUseCase.executeForApis(any(), any(), any())).thenReturn(
                output(
                    new MetricMeasuresResponse(
                        MetricSpec.Name.HTTP_REQUESTS,
                        null,
                        List.of(count(10), new Measure(MetricSpec.Measure.AVG, 2)),
                        List.of()
                    )
                )
            );

            var response = adapter.computeMeasures(
                new MeasuresQuery(
                    ORG,
                    ENV,
                    List.of(SCOPE),
                    List.of(
                        new MeasuresQuery.Metric(
                            new AnalyticsMetricQuery("count", "HTTP_REQUESTS", List.of("COUNT"), List.of()),
                            0,
                            List.of()
                        ),
                        new MeasuresQuery.Metric(
                            new AnalyticsMetricQuery("avg", "HTTP_REQUESTS", List.of("AVG"), List.of()),
                            0,
                            List.of("RECORD_TYPE")
                        )
                    )
                )
            );

            assertThat(response.at("/metrics/0/measures").findValuesAsText("name")).containsExactly("COUNT");
            assertThat(response.at("/metrics/1/measures").findValuesAsText("name")).containsExactly("AVG");
            assertThat(response.at("/metrics/0").has("ignoredFilters")).isFalse();
            assertThat(response.at("/metrics/1/ignoredFilters").toString()).isEqualTo("[\"RECORD_TYPE\"]");
        }

        @Test
        void should_answer_a_zero_valued_entry_per_identified_metric_of_an_empty_group_without_querying_the_engine() {
            var response = adapter.computeMeasures(
                new MeasuresQuery(
                    ORG,
                    ENV,
                    List.of(PreparedScope.empty(SCOPE.from(), SCOPE.to())),
                    List.of(
                        new MeasuresQuery.Metric(
                            new AnalyticsMetricQuery("total", "HTTP_REQUESTS", List.of("COUNT"), List.of()),
                            0,
                            List.of()
                        ),
                        new MeasuresQuery.Metric(
                            new AnalyticsMetricQuery("latency", "HTTP_GATEWAY_LATENCY", List.of("AVG"), List.of()),
                            0,
                            List.of()
                        )
                    )
                )
            );

            verifyNoInteractions(computeMeasuresUseCase);
            assertThat(response.get("metrics")).hasSize(2);
            assertThat(response.at("/metrics/0/id").asText()).isEqualTo("total");
            assertThat(response.at("/metrics/0/name").asText()).isEqualTo("HTTP_REQUESTS");
            assertThat(response.at("/metrics/0/measures").toString()).isEqualTo("[{\"name\":\"COUNT\",\"value\":0}]");
            assertThat(response.at("/metrics/1/unit").asText()).isEqualTo("MILLISECONDS");
        }

        @Test
        void should_answer_one_zero_valued_entry_per_name_without_ids() {
            var response = adapter.computeMeasures(
                new MeasuresQuery(
                    ORG,
                    ENV,
                    List.of(PreparedScope.empty(SCOPE.from(), SCOPE.to())),
                    List.of(
                        new MeasuresQuery.Metric(
                            new AnalyticsMetricQuery(null, "HTTP_REQUESTS", List.of("COUNT"), List.of()),
                            0,
                            List.of()
                        ),
                        new MeasuresQuery.Metric(new AnalyticsMetricQuery(null, "HTTP_REQUESTS", List.of("AVG"), List.of()), 0, List.of())
                    )
                )
            );

            assertThat(response.get("metrics")).hasSize(1);
            assertThat(response.at("/metrics/0/measures").findValuesAsText("name")).containsExactly("COUNT", "AVG");
            assertThat(response.at("/metrics/0").has("id")).isFalse();
        }

        private static ComputeMeasuresUseCase.Output output(MetricMeasuresResponse metric) {
            return new ComputeMeasuresUseCase.Output(new MeasuresResponse(List.of(metric)));
        }

        private static Measure count(long value) {
            return new Measure(MetricSpec.Measure.COUNT, value);
        }
    }

    private static MeasuresQuery measuresQuery(PreparedScope scope, AnalyticsMetricQuery... metrics) {
        return new MeasuresQuery(
            ORG,
            ENV,
            List.of(scope),
            Arrays.stream(metrics)
                .map(metric -> new MeasuresQuery.Metric(metric, 0, List.of()))
                .toList()
        );
    }

    @Nested
    class SeriesLimit {

        private static final List<AnalyticsFacetMetricQuery> REQUEST_COUNT = List.of(
            new AnalyticsFacetMetricQuery("HTTP_REQUESTS", List.of("COUNT"), List.of(), List.of())
        );
        private static final SeriesRanking TOP_B_AND_C = new SeriesRanking(Map.of("HTTP_REQUESTS", Set.of(List.of("b"), List.of("c"))));

        @Test
        void should_fetch_only_the_ranked_values_on_top_of_the_scope() {
            givenTheEngineAnswers(List.of());

            adapter.computeTimeSeries(query(List.of("LLM_PROXY_MODEL"), List.of(), TOP_B_AND_C));

            var request = engineRequest();
            assertThat(request.filters()).containsExactly(
                new Filter(FilterSpec.Name.API, io.gravitee.apim.core.observability.model.FilterOperator.IN, List.of("api-1")),
                new Filter(FilterSpec.Name.LLM_PROXY_MODEL, io.gravitee.apim.core.observability.model.FilterOperator.IN, List.of("b", "c"))
            );
            assertThat(request.limit()).as("every ranked value fits in each time bucket").isEqualTo(2);
        }

        @Test
        void should_restrict_every_facet_level_to_its_ranked_values() {
            givenTheEngineAnswers(List.of());
            var ranking = new SeriesRanking(
                Map.of(
                    "HTTP_REQUESTS",
                    Set.of(List.of("api-1", "gpt"), List.of("api-1", "mistral"), List.of("api-2", "gpt"), List.of("api-2", "claude"))
                )
            );

            adapter.computeTimeSeries(query(List.of("APPLICATION", "LLM_PROXY_MODEL"), List.of(), ranking));

            var request = engineRequest();
            assertThat(request.filters()).contains(
                new Filter(
                    FilterSpec.Name.APPLICATION,
                    io.gravitee.apim.core.observability.model.FilterOperator.IN,
                    List.of("api-1", "api-2")
                ),
                new Filter(
                    FilterSpec.Name.LLM_PROXY_MODEL,
                    io.gravitee.apim.core.observability.model.FilterOperator.IN,
                    List.of("claude", "gpt", "mistral")
                )
            );
            assertThat(request.limit()).as("the innermost level's ranked values, not the outer level's").isEqualTo(3);
        }

        @Test
        void should_draw_no_series_for_an_empty_ranking() {
            givenTheEngineAnswers(List.of(timeBucket(1L, facetBucket("a", 1))));

            var response = adapter.computeTimeSeries(query(List.of("LLM_PROXY_MODEL"), List.of(), new SeriesRanking(Map.of())));

            var request = engineRequest();
            assertThat(request.filters()).extracting(Filter::name).containsExactly(FilterSpec.Name.API);
            assertThat(request.limit()).as("no ranked value; the engine reads 0 as its default").isZero();
            assertThat(response.at("/metrics/0/buckets/0/buckets")).isEmpty();
        }

        @Test
        void should_only_restrict_the_facet_levels_the_ranking_reached() {
            givenTheEngineAnswers(List.of());
            var ranking = new SeriesRanking(Map.of("HTTP_REQUESTS", Set.of(List.of("api-1"), List.of("api-2"))));

            adapter.computeTimeSeries(query(List.of("API", "PLAN"), List.of(), ranking));

            var request = engineRequest();
            assertThat(request.filters()).extracting(Filter::name).containsExactly(FilterSpec.Name.API, FilterSpec.Name.API);
            assertThat(request.limit()).isEqualTo(2);
        }

        @Test
        void should_not_filter_on_bucket_keys_that_are_not_filter_values() {
            givenTheEngineAnswers(List.of());
            var ranking = new SeriesRanking(Map.of("HTTP_REQUESTS", Set.of(List.of("200-299", "100-199"))));

            adapter.computeTimeSeries(
                query(List.of("HTTP_STATUS_CODE_GROUP", "HTTP_STATUS"), List.of(new AnalyticsNumberRange(100, 199)), ranking)
            );

            assertThat(engineRequest().filters())
                .as("a status group key and a range key are labels, not values")
                .extracting(Filter::name)
                .containsExactly(FilterSpec.Name.API);
        }

        @Test
        void should_keep_only_the_ranked_series_in_every_bucket() {
            givenTheEngineAnswers(
                List.of(
                    timeBucket(1L, facetBucket("a", 9), facetBucket("b", 1), facetBucket("c", 2)),
                    timeBucket(2L, facetBucket("d", 7), facetBucket("c", 3)),
                    timeBucket(3L, facetBucket("a", 4))
                )
            );

            var response = adapter.computeTimeSeries(query(List.of("LLM_PROXY_MODEL"), List.of(), TOP_B_AND_C));

            assertThat(response.at("/metrics/0/buckets/0/buckets").findValuesAsText("key")).containsExactly("b", "c");
            assertThat(response.at("/metrics/0/buckets/1/buckets").findValuesAsText("key")).containsExactly("c");
            assertThat(response.at("/metrics/0/buckets/2/buckets")).isEmpty();
            assertThat(response.at("/metrics/0/buckets/2/type").asText()).as("typed after the series are kept").isEqualTo("LEAF");
        }

        @Test
        void should_keep_an_inner_series_only_under_the_outer_value_it_was_ranked_in() {
            givenTheEngineAnswers(
                List.of(
                    timeBucket(
                        1L,
                        new FacetBucketResponse("app-1", null, List.of(facetBucket("gpt", 1), facetBucket("claude", 1)), null),
                        new FacetBucketResponse("app-2", null, List.of(facetBucket("gpt", 1), facetBucket("claude", 1)), null),
                        new FacetBucketResponse("app-3", null, List.of(facetBucket("gpt", 1)), null)
                    )
                )
            );
            var ranking = new SeriesRanking(Map.of("HTTP_REQUESTS", Set.of(List.of("app-1", "gpt"), List.of("app-2", "claude"))));

            var response = adapter.computeTimeSeries(query(List.of("APPLICATION", "LLM_PROXY_MODEL"), List.of(), ranking));

            var outer = response.at("/metrics/0/buckets/0/buckets");
            assertThat(outer).as("app-3 has no ranked series").hasSize(2);
            assertThat(outer.at("/0/key").asText()).isEqualTo("app-1");
            assertThat(outer.at("/1/key").asText()).isEqualTo("app-2");
            assertThat(outer.at("/0/buckets").findValuesAsText("key")).containsExactly("gpt");
            assertThat(outer.at("/1/buckets").findValuesAsText("key")).containsExactly("claude");
        }

        @Test
        void should_drop_an_outer_value_left_without_a_ranked_series_in_a_time_bucket() {
            givenTheEngineAnswers(
                List.of(
                    timeBucket(
                        1L,
                        new FacetBucketResponse("app-1", null, List.of(facetBucket("claude", 1)), null),
                        new FacetBucketResponse("app-2", null, List.of(facetBucket("claude", 1)), null)
                    )
                )
            );
            var ranking = new SeriesRanking(Map.of("HTTP_REQUESTS", Set.of(List.of("app-1", "gpt"), List.of("app-2", "claude"))));

            var response = adapter.computeTimeSeries(query(List.of("APPLICATION", "LLM_PROXY_MODEL"), List.of(), ranking));

            var outer = response.at("/metrics/0/buckets/0/buckets");
            assertThat(outer).as("app-1 holds only an unranked model in this hour").hasSize(1);
            assertThat(outer.at("/0/key").asText()).isEqualTo("app-2");
        }

        @Test
        void should_not_report_a_ranking_condition_the_engine_ignored_unless_the_caller_sent_it() {
            when(computeTimeSeriesUseCase.executeForApis(any(), any(), any())).thenReturn(
                new ComputeTimeSeriesUseCase.Output(
                    new TimeSeriesResponse(
                        List.of(
                            new TimeSeriesMetricResponse(
                                MetricSpec.Name.HTTP_REQUESTS,
                                null,
                                List.of(),
                                List.of(FilterSpec.Name.EDGE_MODEL, FilterSpec.Name.NATIVE_CLIENT_ID, FilterSpec.Name.API)
                            )
                        )
                    )
                )
            );
            var ranking = new SeriesRanking(Map.of("HTTP_REQUESTS", Set.of(List.of("model-1", "api-1"))));

            var response = adapter.computeTimeSeries(query(List.of("EDGE_MODEL", "API"), List.of(), ranking));

            assertThat(response.at("/metrics/0/ignoredFilters").toString()).isEqualTo("[\"NATIVE_CLIENT_ID\",\"API\"]");
        }

        private void givenTheEngineAnswers(List<TimeSeriesBucketResponse> buckets) {
            when(computeTimeSeriesUseCase.executeForApis(any(), any(), any())).thenReturn(
                new ComputeTimeSeriesUseCase.Output(
                    new TimeSeriesResponse(List.of(new TimeSeriesMetricResponse(MetricSpec.Name.HTTP_REQUESTS, null, buckets, List.of())))
                )
            );
        }

        private TimeSeriesRequest engineRequest() {
            var captor = ArgumentCaptor.forClass(TimeSeriesRequest.class);
            verify(computeTimeSeriesUseCase).executeForApis(any(), captor.capture(), any());
            return captor.getValue();
        }

        private static TimeSeriesQuery query(List<String> facets, List<AnalyticsNumberRange> ranges, SeriesRanking ranking) {
            return new TimeSeriesQuery(ORG, ENV, SCOPE, 60_000L, facets, REQUEST_COUNT, ranges, Map.of(), ranking);
        }

        private static TimeSeriesBucketResponse timeBucket(long timestamp, FacetBucketResponse... buckets) {
            return new TimeSeriesBucketResponse(String.valueOf(timestamp), null, timestamp, List.of(buckets), null);
        }

        private static FacetBucketResponse facetBucket(String key, long count) {
            return new FacetBucketResponse(key, null, null, List.of(new Measure(MetricSpec.Measure.COUNT, count)));
        }
    }

    // Runs whatever the caller can read: the use cases call it before answering an empty scope.
    @Nested
    class Validate {

        private final PreparedScope emptyScope = PreparedScope.empty(SCOPE.from(), SCOPE.to());

        @Test
        void should_accept_a_well_formed_query_on_an_empty_scope() {
            var query = measuresQuery(emptyScope, new AnalyticsMetricQuery(null, "HTTP_REQUESTS", List.of("COUNT"), List.of()));

            adapter.validate(query);

            verifyNoInteractions(computeMeasuresUseCase);
        }

        @Test
        void should_refuse_an_unknown_metric_on_an_empty_scope() {
            var query = measuresQuery(emptyScope, new AnalyticsMetricQuery(null, "HTTP_NOPE", List.of("COUNT"), List.of()));

            assertThatThrownBy(() -> adapter.validate(query))
                .isInstanceOf(InvalidObservabilityQueryException.class)
                .extracting("technicalCode")
                .isEqualTo("observability.query.unknown_metric");
        }

        @Test
        void should_apply_the_engine_rules_on_an_empty_scope() {
            var query = new FacetsQuery(
                ORG,
                ENV,
                emptyScope,
                List.of(),
                null,
                List.of(new AnalyticsFacetMetricQuery("HTTP_REQUESTS", List.of("COUNT"), List.of(), List.of())),
                List.of(),
                Map.of()
            );

            assertThatThrownBy(() -> adapter.validate(query))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("at least one facet");
        }

        @Test
        void should_refuse_an_unknown_measure_on_a_time_series_with_an_empty_scope() {
            var query = new TimeSeriesQuery(
                ORG,
                ENV,
                emptyScope,
                60_000L,
                List.of(),
                List.of(new AnalyticsFacetMetricQuery("HTTP_REQUESTS", List.of("MEDIAN"), List.of(), List.of())),
                List.of(),
                Map.of(),
                null
            );

            assertThatThrownBy(() -> adapter.validate(query))
                .isInstanceOf(InvalidObservabilityQueryException.class)
                .extracting("technicalCode")
                .isEqualTo("observability.query.unknown_measure");
        }
    }
}
