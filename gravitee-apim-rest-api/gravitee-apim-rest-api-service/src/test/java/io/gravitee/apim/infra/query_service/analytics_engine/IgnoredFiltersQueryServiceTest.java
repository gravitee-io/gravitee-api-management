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
package io.gravitee.apim.infra.query_service.analytics_engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.analytics_engine.model.FacetMetricMeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.FacetSpec;
import io.gravitee.apim.core.analytics_engine.model.FacetsRequest;
import io.gravitee.apim.core.analytics_engine.model.Filter;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.MeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.MetricMeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.analytics_engine.model.TimeRange;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesRequest;
import io.gravitee.apim.core.observability.model.FilterOperator;
import io.gravitee.repository.analytics.engine.api.metric.Measure;
import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.AnalyticsSearchPath;
import io.gravitee.repository.analytics.engine.api.query.FilterOutcome;
import io.gravitee.repository.analytics.engine.api.result.FacetsResult;
import io.gravitee.repository.analytics.engine.api.result.MeasuresResult;
import io.gravitee.repository.analytics.engine.api.result.MetricFacetsResult;
import io.gravitee.repository.analytics.engine.api.result.MetricMeasuresResult;
import io.gravitee.repository.analytics.engine.api.result.MetricTimeSeriesResult;
import io.gravitee.repository.analytics.engine.api.result.TimeSeriesResult;
import io.gravitee.repository.log.v4.api.AnalyticsRepository;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Each query service marks its metrics with the filters the repository says the path it searched skips.
 * The repository stub answers per path, so a service asking about the wrong path reports the wrong set.
 */
class IgnoredFiltersQueryServiceTest {

    private static final TimeRange TIME_RANGE = new TimeRange(Instant.EPOCH, Instant.EPOCH.plusSeconds(60));
    private static final Filter KAFKA_CLIENT = new Filter(FilterSpec.Name.NATIVE_CLIENT_ID, FilterOperator.EQ, "client");
    private static final Filter EDGE_TYPE = new Filter(FilterSpec.Name.EDGE_TYPE, FilterOperator.EQ, "agent");
    private static final Filter HTTP_STATUS = new Filter(FilterSpec.Name.HTTP_STATUS, FilterOperator.EQ, "500");
    private static final Filter OPERATION = new Filter(FilterSpec.Name.AUTHZ_OPERATION, FilterOperator.EQ, "check");

    private final AnalyticsRepository repository = mock(AnalyticsRepository.class);
    private final ExecutionContext context = mock(ExecutionContext.class);

    /** Ignored on its own path only, so the reported set names the path that was asked about. */
    private static final Map<AnalyticsSearchPath, io.gravitee.repository.analytics.engine.api.query.Filter.Name> IGNORED_ON = Map.of(
        AnalyticsSearchPath.HTTP,
        io.gravitee.repository.analytics.engine.api.query.Filter.Name.NATIVE_CLIENT_ID,
        AnalyticsSearchPath.EDGE,
        io.gravitee.repository.analytics.engine.api.query.Filter.Name.HTTP_STATUS
    );

    @BeforeEach
    void stub_the_repository_answer() {
        when(repository.filterOutcome(any(), any())).thenAnswer(invocation -> {
            AnalyticsSearchPath path = invocation.getArgument(0);
            io.gravitee.repository.analytics.engine.api.query.Filter.Name name = invocation.getArgument(1);
            if (name == io.gravitee.repository.analytics.engine.api.query.Filter.Name.AUTHZ_OPERATION) {
                return FilterOutcome.EMPTIES;
            }
            return name == IGNORED_ON.get(path) ? FilterOutcome.IGNORED : FilterOutcome.APPLIED;
        });
    }

    @Nested
    class HttpDataPlane {

        private final HTTPDataPlaneAnalyticsQueryService service = new HTTPDataPlaneAnalyticsQueryService(repository);

        @Test
        void should_mark_measures_with_what_the_http_path_skips() {
            when(repository.searchHTTPMeasures(any(), any())).thenReturn(
                new MeasuresResult(List.of(new MetricMeasuresResult(Metric.HTTP_REQUESTS, Map.of(Measure.COUNT, 1))))
            );

            var response = service.searchMeasures(context, measures(MetricSpec.Name.HTTP_REQUESTS, KAFKA_CLIENT, EDGE_TYPE));

            assertThat(response.metrics())
                .singleElement()
                .satisfies(metric -> assertThat(metric.ignoredFilters()).containsExactly(FilterSpec.Name.NATIVE_CLIENT_ID));
        }

        @Test
        void should_mark_time_series_with_what_the_http_path_skips() {
            when(repository.searchHTTPTimeSeries(any(), any())).thenReturn(
                new TimeSeriesResult(List.of(new MetricTimeSeriesResult(Metric.EDGE_TOKENS_IN, List.of())))
            );

            var response = service.searchTimeSeries(context, timeSeries(MetricSpec.Name.EDGE_TOKENS_IN, KAFKA_CLIENT, HTTP_STATUS));

            assertThat(response.metrics())
                .singleElement()
                .satisfies(metric -> assertThat(metric.ignoredFilters()).containsExactly(FilterSpec.Name.NATIVE_CLIENT_ID));
        }

        @Test
        void should_ask_about_the_edge_path_when_every_facet_metric_is_an_edge_metric() {
            when(repository.searchEdgeFacets(any(), any())).thenReturn(
                new FacetsResult(List.of(new MetricFacetsResult(Metric.EDGE_DETECTION_COUNT, List.of())))
            );

            var response = service.searchFacets(context, facets(MetricSpec.Name.EDGE_DETECTION_COUNT, KAFKA_CLIENT, HTTP_STATUS));

            assertThat(response.metrics())
                .singleElement()
                .satisfies(metric -> assertThat(metric.ignoredFilters()).containsExactly(FilterSpec.Name.HTTP_STATUS));
        }

        @Test
        void should_ask_about_the_http_path_for_other_facets() {
            when(repository.searchHTTPFacets(any(), any())).thenReturn(
                new FacetsResult(List.of(new MetricFacetsResult(Metric.HTTP_REQUESTS, List.of())))
            );

            var response = service.searchFacets(context, facets(MetricSpec.Name.HTTP_REQUESTS, KAFKA_CLIENT, HTTP_STATUS));

            assertThat(response.metrics())
                .singleElement()
                .satisfies(metric -> assertThat(metric.ignoredFilters()).containsExactly(FilterSpec.Name.NATIVE_CLIENT_ID));
        }

        @Test
        void should_report_nothing_when_every_filter_applies() {
            when(repository.searchHTTPMeasures(any(), any())).thenReturn(
                new MeasuresResult(List.of(new MetricMeasuresResult(Metric.HTTP_REQUESTS, Map.of(Measure.COUNT, 1))))
            );

            var response = service.searchMeasures(context, measures(MetricSpec.Name.HTTP_REQUESTS, HTTP_STATUS));

            assertThat(response.metrics())
                .singleElement()
                .satisfies(metric -> assertThat(metric.ignoredFilters()).isEmpty());
        }
    }

    @Test
    void should_report_nothing_when_a_condition_empties_the_result() {
        var service = new AuthzAnalyticsQueryService(repository);
        when(repository.searchAuthzMeasures(any(), any())).thenReturn(
            new MeasuresResult(List.of(new MetricMeasuresResult(Metric.AUTHZ_DECISIONS, Map.of(Measure.COUNT, 0))))
        );
        when(
            repository.filterOutcome(AnalyticsSearchPath.AUTHZ, io.gravitee.repository.analytics.engine.api.query.Filter.Name.HTTP_STATUS)
        ).thenReturn(FilterOutcome.IGNORED);

        var response = service.searchMeasures(context, measures(MetricSpec.Name.AUTHZ_DECISIONS, HTTP_STATUS, OPERATION));

        assertThat(response.metrics())
            .singleElement()
            .satisfies(metric -> assertThat(metric.ignoredFilters()).isEmpty());
    }

    @Test
    void should_ask_each_service_about_its_own_path() {
        record Case(AnalyticsSearchPath path, Runnable search) {}
        var cases = List.of(
            new Case(AnalyticsSearchPath.MESSAGE, () ->
                new MessageDataPlaneQueryService(repository).searchMeasures(context, measures(MetricSpec.Name.MESSAGES, HTTP_STATUS))
            ),
            new Case(AnalyticsSearchPath.NATIVE, () ->
                new NativeApiAnalyticsQueryService(repository).searchFacets(
                    context,
                    facets(MetricSpec.Name.NATIVE_CONNECTIONS_SUMMARY, HTTP_STATUS)
                )
            ),
            new Case(AnalyticsSearchPath.EVENT_METRICS, () ->
                new EventMetricsAnalyticsQueryService(repository).searchMeasures(
                    context,
                    measures(MetricSpec.Name.NATIVE_MESSAGES_PRODUCED_DOWNSTREAM, HTTP_STATUS)
                )
            ),
            new Case(AnalyticsSearchPath.AUTHZ, () ->
                new AuthzAnalyticsQueryService(repository).searchMeasures(context, measures(MetricSpec.Name.AUTHZ_DECISIONS, HTTP_STATUS))
            ),
            new Case(AnalyticsSearchPath.AUTHZ_TRAFFIC, () ->
                new AuthzTrafficAnalyticsQueryService(repository).searchMeasures(
                    context,
                    measures(MetricSpec.Name.AUTHZ_OPERATIONS, HTTP_STATUS)
                )
            )
        );

        for (var aCase : cases) {
            org.mockito.Mockito.clearInvocations(repository);
            aCase.search().run();
            org.mockito.Mockito.verify(repository).filterOutcome(
                aCase.path(),
                io.gravitee.repository.analytics.engine.api.query.Filter.Name.HTTP_STATUS
            );
        }
    }

    private static MeasuresRequest measures(MetricSpec.Name metric, Filter... filters) {
        return new MeasuresRequest(
            TIME_RANGE,
            List.of(filters),
            List.of(new MetricMeasuresRequest(metric, List.of(MetricSpec.Measure.COUNT), null))
        );
    }

    private static FacetsRequest facets(MetricSpec.Name metric, Filter... filters) {
        return new FacetsRequest(
            TIME_RANGE,
            List.of(filters),
            List.of(new FacetMetricMeasuresRequest(metric, List.of(MetricSpec.Measure.COUNT), null, null)),
            List.of(FacetSpec.Name.API),
            null,
            null
        );
    }

    private static TimeSeriesRequest timeSeries(MetricSpec.Name metric, Filter... filters) {
        return new TimeSeriesRequest(
            TIME_RANGE,
            60_000L,
            List.of(filters),
            List.of(new FacetMetricMeasuresRequest(metric, List.of(MetricSpec.Measure.COUNT), null, null)),
            null,
            null,
            null
        );
    }
}
