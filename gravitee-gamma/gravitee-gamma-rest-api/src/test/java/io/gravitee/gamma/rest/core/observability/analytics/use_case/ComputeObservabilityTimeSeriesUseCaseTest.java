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
package io.gravitee.gamma.rest.core.observability.analytics.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.gravitee.gamma.rest.core.observability.analytics.model.AnalyticsFacetMetricQuery;
import io.gravitee.gamma.rest.core.observability.analytics.model.AnalyticsSortSpec;
import io.gravitee.gamma.rest.core.observability.analytics.model.DataKind;
import io.gravitee.gamma.rest.core.observability.analytics.port.service_provider.ObservabilityAnalyticsDataPort;
import io.gravitee.gamma.rest.core.observability.exception.InvalidObservabilityQueryException;
import io.gravitee.gamma.rest.core.observability.filter.domain_service.ObservabilityFilterValidator;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterCondition;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterOperator;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterSpec;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterType;
import io.gravitee.gamma.rest.core.observability.filter.model.Signal;
import io.gravitee.gamma.rest.core.observability.filter.port.service_provider.FilterRegistry;
import io.gravitee.gamma.rest.core.observability.logs.domain_service.AccessibleApiScopeDomainService;
import io.gravitee.gamma.rest.core.observability.logs.port.service_provider.ObservabilityLogsDataPort.AccessibleApi;
import io.gravitee.gamma.rest.infra.adapter.EntrypointScopeProviderAdapter;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ComputeObservabilityTimeSeriesUseCaseTest {

    private static final String ORG_ID = "org-1";
    private static final String ENV_ID = "env-1";
    private static final Instant FROM = Instant.parse("2026-06-10T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-06-11T00:00:00Z");
    private static final List<AnalyticsFacetMetricQuery> METRICS = List.of(
        new AnalyticsFacetMetricQuery("HTTP_REQUESTS", List.of("COUNT"), List.of(), List.of())
    );

    @Mock
    private ObservabilityAnalyticsDataPort analyticsDataPort;

    @Mock
    private FilterRegistry filterRegistry;

    private ComputeObservabilityTimeSeriesUseCase useCase;

    @BeforeEach
    void setUp() {
        var pipeline = new AnalyticsRequestPipeline(
            new ObservabilityFilterValidator(filterRegistry),
            new AccessibleApiScopeDomainService(),
            new EntrypointScopeProviderAdapter()
        );
        useCase = new ComputeObservabilityTimeSeriesUseCase(analyticsDataPort, pipeline);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = { 0L, -60_000L })
    void should_reject_a_missing_or_non_positive_interval_before_loading_the_accessible_apis(Long interval) {
        assertThatThrownBy(() -> useCase.execute(input(interval, METRICS)))
            .isInstanceOf(InvalidObservabilityQueryException.class)
            .extracting("technicalCode")
            .isEqualTo("observability.query.invalid_interval");
        verifyNoInteractions(analyticsDataPort);
    }

    @Test
    void should_reject_a_request_without_metrics_before_loading_the_accessible_apis() {
        assertThatThrownBy(() -> useCase.execute(input(60_000L, List.of())))
            .isInstanceOf(InvalidObservabilityQueryException.class)
            .extracting("technicalCode")
            .isEqualTo("observability.query.metrics_required");
        verifyNoInteractions(analyticsDataPort);
    }

    @Test
    void should_compute_a_well_formed_request() {
        when(analyticsDataPort.loadAccessibleApis(ORG_ID, ENV_ID, DataKind.AGGREGATES)).thenReturn(
            List.of(new AccessibleApi("api-1", "API 1", ApiType.HTTP_PROXY))
        );
        var response = JsonNodeFactory.instance.objectNode();
        when(analyticsDataPort.computeTimeSeries(any())).thenReturn(response);

        var output = useCase.execute(input(60_000L, METRICS));

        assertThat(output.response()).isSameAs(response);
    }

    /** A breakdown by a field identifying callers is per-request data. */
    @Test
    void should_count_the_apis_whose_requests_the_caller_reads_when_a_facet_identifies_callers() {
        when(analyticsDataPort.loadAccessibleApis(ORG_ID, ENV_ID, DataKind.PER_REQUEST)).thenReturn(
            List.of(new AccessibleApi("logged-api", "Logged API", ApiType.HTTP_PROXY))
        );
        when(analyticsDataPort.computeTimeSeries(any())).thenReturn(JsonNodeFactory.instance.objectNode());

        useCase.execute(input(60_000L, METRICS, List.of("CONSUMER_IP"), null));

        var captor = ArgumentCaptor.forClass(ObservabilityAnalyticsDataPort.TimeSeriesQuery.class);
        verify(analyticsDataPort).computeTimeSeries(captor.capture());
        assertThat(captor.getValue().scope().apiIds()).containsExactly("logged-api");
    }

    @Test
    void should_refuse_a_malformed_query_even_when_the_caller_can_read_no_api() {
        when(analyticsDataPort.loadAccessibleApis(ORG_ID, ENV_ID, DataKind.AGGREGATES)).thenReturn(List.of());
        doThrow(InvalidObservabilityQueryException.unknownMeasure("HTTP_REQUESTS", "MEDIAN"))
            .when(analyticsDataPort)
            .validate(any(ObservabilityAnalyticsDataPort.TimeSeriesQuery.class));

        assertThatThrownBy(() -> useCase.execute(input(60_000L, METRICS))).isInstanceOf(InvalidObservabilityQueryException.class);
        verify(analyticsDataPort, never()).emptyTimeSeriesResponse();
    }

    @Test
    void should_append_the_metric_conditions_every_metric_shares_to_the_query_filters() {
        givenTheStatusFilter();
        when(analyticsDataPort.loadAccessibleApis(ORG_ID, ENV_ID, DataKind.AGGREGATES)).thenReturn(
            List.of(new AccessibleApi("api-1", "API 1", ApiType.HTTP_PROXY))
        );
        var metrics = List.of(
            new AnalyticsFacetMetricQuery("HTTP_REQUESTS", List.of("COUNT"), List.of(), List.of(STATUS_500)),
            new AnalyticsFacetMetricQuery("HTTP_ERRORS", List.of("COUNT"), List.of(), List.of(STATUS_500))
        );

        useCase.execute(input(60_000L, metrics));

        var captor = ArgumentCaptor.forClass(ObservabilityAnalyticsDataPort.TimeSeriesQuery.class);
        verify(analyticsDataPort).computeTimeSeries(captor.capture());
        assertThat(captor.getValue().scope().filters()).contains(STATUS_500);
        assertThat(captor.getValue().conditionsNotApplied()).isEmpty();
    }

    @Test
    void should_refuse_metrics_carrying_different_conditions_before_any_data_access() {
        givenTheStatusFilter();
        var metrics = List.of(
            new AnalyticsFacetMetricQuery("HTTP_REQUESTS", List.of("COUNT"), List.of(), List.of(STATUS_500)),
            new AnalyticsFacetMetricQuery("HTTP_ERRORS", List.of("COUNT"), List.of(), List.of())
        );

        assertThatThrownBy(() -> useCase.execute(input(60_000L, metrics)))
            .isInstanceOf(InvalidObservabilityQueryException.class)
            .extracting("technicalCode")
            .isEqualTo("observability.query.differing_metric_conditions");
        verifyNoInteractions(analyticsDataPort);
    }

    private static final FilterCondition STATUS_500 = new FilterCondition("HTTP_STATUS", FilterOperator.EQ, List.of("500"));

    private void givenTheStatusFilter() {
        when(filterRegistry.getFilters(any(), any())).thenReturn(
            List.of(
                new FilterSpec(
                    "HTTP_STATUS",
                    "Status Code",
                    FilterType.NUMBER,
                    List.of(FilterOperator.EQ),
                    null,
                    new FilterSpec.Range(100, 599),
                    Set.of(Signal.ANALYTICS),
                    Set.of(ApiType.HTTP_PROXY)
                )
            )
        );
    }

    @Nested
    class Limit {

        private static final List<AnalyticsFacetMetricQuery> SORTED_REQUEST_COUNT = List.of(
            new AnalyticsFacetMetricQuery("HTTP_REQUESTS", List.of("COUNT"), List.of(new AnalyticsSortSpec("COUNT", "ASC")), List.of())
        );

        @BeforeEach
        void readableApi() {
            when(analyticsDataPort.loadAccessibleApis(ORG_ID, ENV_ID, DataKind.AGGREGATES)).thenReturn(
                List.of(new AccessibleApi("api-1", "API 1", ApiType.HTTP_PROXY))
            );
        }

        @Test
        void should_rank_the_values_over_the_whole_range_with_the_scope_of_the_time_series() throws Exception {
            when(analyticsDataPort.computeFacets(any())).thenReturn(facets("a", 3, "b", 9, "c", 5));
            when(analyticsDataPort.computeTimeSeries(any())).thenReturn(JsonNodeFactory.instance.objectNode());

            useCase.execute(input(60_000L, SORTED_REQUEST_COUNT, List.of("LLM_PROXY_MODEL"), 2));

            var facetsQuery = ArgumentCaptor.forClass(ObservabilityAnalyticsDataPort.FacetsQuery.class);
            verify(analyticsDataPort).computeFacets(facetsQuery.capture());
            var timeSeriesQuery = ArgumentCaptor.forClass(ObservabilityAnalyticsDataPort.TimeSeriesQuery.class);
            verify(analyticsDataPort).computeTimeSeries(timeSeriesQuery.capture());

            assertThat(facetsQuery.getValue().scope()).isEqualTo(timeSeriesQuery.getValue().scope());
            assertThat(facetsQuery.getValue().facets()).containsExactly("LLM_PROXY_MODEL");
            assertThat(facetsQuery.getValue().limit()).as("every candidate, not the top N of the engine's order").isEqualTo(1000);
            assertThat(facetsQuery.getValue().metrics().getFirst().sorts()).as("ranked by Gamma, not by the engine").isEmpty();
            assertThat(timeSeriesQuery.getValue().ranking().keysAt(0)).containsExactly("b", "c");
        }

        @Test
        void should_refuse_a_limit_above_the_series_cap_before_any_data_access() {
            reset(analyticsDataPort);

            assertThatThrownBy(() -> useCase.execute(input(60_000L, METRICS, List.of("API"), 101)))
                .isInstanceOf(InvalidObservabilityQueryException.class)
                .extracting("technicalCode")
                .isEqualTo("observability.query.invalid_limit");
            verifyNoInteractions(analyticsDataPort);
        }

        @Test
        void should_accept_a_limit_at_the_series_cap() throws Exception {
            when(analyticsDataPort.computeFacets(any())).thenReturn(facets("a", 3));
            when(analyticsDataPort.computeTimeSeries(any())).thenReturn(JsonNodeFactory.instance.objectNode());

            useCase.execute(input(60_000L, METRICS, List.of("API"), 100));

            verify(analyticsDataPort).computeTimeSeries(any());
        }

        @Test
        void should_answer_the_empty_time_series_without_plotting_when_no_value_occurs() throws Exception {
            var empty = JsonNodeFactory.instance.objectNode();
            when(analyticsDataPort.computeFacets(any())).thenReturn(facets());
            when(analyticsDataPort.emptyTimeSeriesResponse()).thenReturn(empty);

            var output = useCase.execute(input(60_000L, METRICS, List.of("API"), 5));

            assertThat(output.response()).isSameAs(empty);
            verify(analyticsDataPort, never()).computeTimeSeries(any());
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(ints = { 0, -1 })
        void should_leave_the_engine_its_per_bucket_default_without_a_positive_limit(Integer limit) {
            when(analyticsDataPort.computeTimeSeries(any())).thenReturn(JsonNodeFactory.instance.objectNode());

            useCase.execute(input(60_000L, METRICS, List.of("API"), limit));

            verify(analyticsDataPort, never()).computeFacets(any());
            var timeSeriesQuery = ArgumentCaptor.forClass(ObservabilityAnalyticsDataPort.TimeSeriesQuery.class);
            verify(analyticsDataPort).computeTimeSeries(timeSeriesQuery.capture());
            assertThat(timeSeriesQuery.getValue().ranking()).isNull();
        }

        @Test
        void should_not_rank_a_time_series_without_facets() {
            when(analyticsDataPort.computeTimeSeries(any())).thenReturn(JsonNodeFactory.instance.objectNode());

            useCase.execute(input(60_000L, METRICS, List.of(), 5));

            verify(analyticsDataPort, never()).computeFacets(any());
        }

        private static JsonNode facets(Object... keysAndCounts) throws Exception {
            var buckets = new StringBuilder();
            for (int i = 0; i < keysAndCounts.length; i += 2) {
                buckets
                    .append(i == 0 ? "" : ",")
                    .append(
                        "{\"key\": \"%s\", \"measures\": [{\"name\": \"COUNT\", \"value\": %s}]}".formatted(
                            keysAndCounts[i],
                            keysAndCounts[i + 1]
                        )
                    );
            }
            return new ObjectMapper().readTree("{\"metrics\": [{\"metric\": \"HTTP_REQUESTS\", \"buckets\": [" + buckets + "]}]}");
        }
    }

    private static ComputeObservabilityTimeSeriesUseCase.Input input(Long interval, List<AnalyticsFacetMetricQuery> metrics) {
        return input(interval, metrics, List.of(), null);
    }

    private static ComputeObservabilityTimeSeriesUseCase.Input input(
        Long interval,
        List<AnalyticsFacetMetricQuery> metrics,
        List<String> facets,
        Integer limit
    ) {
        return new ComputeObservabilityTimeSeriesUseCase.Input(
            ORG_ID,
            ENV_ID,
            List.of(),
            FROM,
            TO,
            interval,
            metrics,
            facets,
            limit,
            List.of()
        );
    }
}
