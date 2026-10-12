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
package io.gravitee.apim.rest.api.mcp.infra;

import static io.gravitee.apim.rest.api.mcp.fakes.Fakes.CALLER;
import static io.gravitee.apim.rest.api.mcp.fakes.Fakes.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.analytics_engine.model.FacetBucketResponse;
import io.gravitee.apim.core.analytics_engine.model.FacetMetricMeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.FacetSpec;
import io.gravitee.apim.core.analytics_engine.model.FacetsRequest;
import io.gravitee.apim.core.analytics_engine.model.FacetsResponse;
import io.gravitee.apim.core.analytics_engine.model.Filter;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.Measure;
import io.gravitee.apim.core.analytics_engine.model.MeasuresResponse;
import io.gravitee.apim.core.analytics_engine.model.MetricFacetsResponse;
import io.gravitee.apim.core.analytics_engine.model.MetricMeasuresResponse;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.analytics_engine.model.TimeRange;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesBucketResponse;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesMetricResponse;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesRequest;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesResponse;
import io.gravitee.apim.core.analytics_engine.use_case.ComputeFacetsUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.ComputeMeasuresUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.ComputeTimeSeriesUseCase;
import io.gravitee.apim.core.observability.model.FilterOperator;
import io.gravitee.apim.rest.api.mcp.core.model.Dimension;
import io.gravitee.apim.rest.api.mcp.core.model.StatusGroup;
import io.gravitee.apim.rest.api.mcp.core.model.TimeWindow;
import io.gravitee.apim.rest.api.mcp.core.model.TimelinePoint;
import io.gravitee.apim.rest.api.mcp.core.model.TrafficSample;
import io.gravitee.apim.rest.api.mcp.core.model.TrafficScope;
import io.gravitee.apim.rest.api.mcp.core.model.TrafficStats;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The engine answers are shaped as its Elasticsearch adapter builds them: status groups are range buckets keyed
 * "400-499", a facet split further carries nested buckets, and only leaf buckets carry measures.
 */
class ComputeTrafficAnalyticsTest {

    private static final TimeWindow WINDOW = TimeWindow.parse("15m", null, NOW);

    private final ComputeMeasuresUseCase measures = mock(ComputeMeasuresUseCase.class);
    private final ComputeFacetsUseCase facets = mock(ComputeFacetsUseCase.class);
    private final ComputeTimeSeriesUseCase timeSeries = mock(ComputeTimeSeriesUseCase.class);
    private final ComputeTrafficAnalytics analytics = new ComputeTrafficAnalytics(measures, facets, timeSeries);

    private final List<FacetsRequest> facetsRequests = new ArrayList<>();
    private final List<TimeSeriesRequest> timeSeriesRequests = new ArrayList<>();

    @BeforeEach
    void setUp() {
        when(measures.execute(any())).thenReturn(
            new ComputeMeasuresUseCase.Output(
                new MeasuresResponse(
                    List.of(
                        p95(MetricSpec.Name.HTTP_GATEWAY_RESPONSE_TIME, 120.44),
                        p95(MetricSpec.Name.HTTP_ENDPOINT_RESPONSE_TIME, 100.0)
                    )
                )
            )
        );
    }

    @Test
    void should_total_requests_per_status_class_and_read_the_response_times() {
        givenFacets(requests(count("100-199", 0), count("200-299", 900), count("300-399", 0), count("400-499", 80), count("500-599", 20)));

        var total = analytics.total(CALLER, WINDOW, TrafficScope.ENVIRONMENT);

        assertThat(total).isEqualTo(TrafficStats.of(1000, 80, 20, 120.4, 100.0));
        var request = facetsRequests.getFirst();
        assertThat(request.timeRange()).isEqualTo(new TimeRange(WINDOW.from(), WINDOW.to()));
        assertThat(request.facets()).containsExactly(FacetSpec.Name.HTTP_STATUS_CODE_GROUP);
        assertThat(request.filters()).isEmpty();
        assertThat(request.metrics())
            .extracting(FacetMetricMeasuresRequest::name, FacetMetricMeasuresRequest::measures)
            .containsExactly(org.assertj.core.groups.Tuple.tuple(MetricSpec.Name.HTTP_REQUESTS, List.of(MetricSpec.Measure.COUNT)));
    }

    @Test
    void should_report_no_response_time_without_traffic() {
        givenFacets(requests());

        assertThat(analytics.total(CALLER, WINDOW, TrafficScope.ENVIRONMENT)).isEqualTo(TrafficStats.NONE);
    }

    @Test
    void should_restrict_the_query_to_the_api_and_status_class_of_the_scope() {
        givenFacets(requests());

        analytics.total(CALLER, WINDOW, new TrafficScope("orders-id", StatusGroup.CLIENT_ERROR));

        assertThat(facetsRequests.getFirst().filters()).containsExactly(
            new Filter(FilterSpec.Name.API, FilterOperator.EQ, "orders-id"),
            new Filter(FilterSpec.Name.HTTP_STATUS_CODE_GROUP, FilterOperator.EQ, "4XX")
        );
    }

    @Test
    void should_split_a_dimension_by_status_class_and_attach_its_response_times() {
        givenFacets(
            new FacetsResponse(
                List.of(
                    new MetricFacetsResponse(
                        MetricSpec.Name.HTTP_GATEWAY_RESPONSE_TIME,
                        MetricSpec.Unit.MILLISECONDS,
                        List.of(new FacetBucketResponse("app-id", "Mobile app", null, List.of(new Measure(MetricSpec.Measure.P95, 250.0))))
                    )
                )
            ),
            requests(
                new FacetBucketResponse("app-id", "Mobile app", List.of(count("200-299", 590), count("500-599", 10)), null),
                new FacetBucketResponse("1", "Unknown", List.of(count("400-499", 300)), null)
            )
        );

        var samples = analytics.by(CALLER, WINDOW, TrafficScope.ENVIRONMENT, Dimension.APPLICATION);

        assertThat(samples).containsExactly(
            new TrafficSample("app-id", "Mobile app", TrafficStats.of(600, 0, 10, 250.0, null)),
            new TrafficSample("1", "Unknown", TrafficStats.of(300, 300, 0, null, null))
        );
        assertThat(facetsRequests.getFirst().facets()).containsExactly(FacetSpec.Name.APPLICATION);
        assertThat(facetsRequests.getLast().facets()).containsExactly(FacetSpec.Name.APPLICATION, FacetSpec.Name.HTTP_STATUS_CODE_GROUP);
    }

    @Test
    void should_break_down_by_status_code_busiest_first() {
        givenFacets(requests(count("401", 300), count("200", 40), count("502", 7)));

        var samples = analytics.by(CALLER, WINDOW, TrafficScope.ENVIRONMENT, Dimension.STATUS);

        assertThat(samples)
            .extracting(TrafficSample::key, sample -> sample.stats().clientErrors(), sample -> sample.stats().serverErrors())
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple("401", 300L, 0L),
                org.assertj.core.groups.Tuple.tuple("200", 0L, 0L),
                org.assertj.core.groups.Tuple.tuple("502", 0L, 7L)
            );
        var request = facetsRequests.getFirst();
        assertThat(request.facets()).containsExactly(FacetSpec.Name.HTTP_STATUS);
        assertThat(request.metrics().getFirst().sorts()).containsExactly(
            new FacetMetricMeasuresRequest.Sort(MetricSpec.Measure.COUNT, FacetMetricMeasuresRequest.Sort.Order.DESC)
        );
    }

    @Test
    void should_build_one_timeline_point_per_interval() {
        long first = WINDOW.from().toEpochMilli();
        long second = first + 30_000;
        when(timeSeries.execute(any())).thenAnswer(invocation -> {
            ComputeTimeSeriesUseCase.Input input = invocation.getArgument(0);
            timeSeriesRequests.add(input.request());
            var metric = input.request().facets().isEmpty()
                ? new TimeSeriesMetricResponse(
                    MetricSpec.Name.HTTP_GATEWAY_RESPONSE_TIME,
                    MetricSpec.Unit.MILLISECONDS,
                    List.of(new TimeSeriesBucketResponse("t1", null, first, null, List.of(new Measure(MetricSpec.Measure.P95, 80.0))))
                )
                : new TimeSeriesMetricResponse(
                    MetricSpec.Name.HTTP_REQUESTS,
                    MetricSpec.Unit.NUMBER,
                    List.of(
                        new TimeSeriesBucketResponse("t1", null, first, List.of(count("200-299", 100)), null),
                        new TimeSeriesBucketResponse("t2", null, second, List.of(count("200-299", 20), count("400-499", 80)), null)
                    )
                );
            return new ComputeTimeSeriesUseCase.Output(new TimeSeriesResponse(List.of(metric)));
        });

        var points = analytics.timeline(CALLER, WINDOW, Duration.ofSeconds(30), new TrafficScope("orders-id", null));

        assertThat(points).containsExactly(
            new TimelinePoint(WINDOW.from(), TrafficStats.of(100, 0, 0, 80.0, null)),
            new TimelinePoint(WINDOW.from().plusSeconds(30), TrafficStats.of(100, 80, 0, null, null))
        );
        assertThat(timeSeriesRequests).allSatisfy(request -> {
            assertThat(request.interval()).isEqualTo(30_000L);
            assertThat(request.filters()).containsExactly(new Filter(FilterSpec.Name.API, FilterOperator.EQ, "orders-id"));
        });
    }

    private void givenFacets(FacetsResponse... responses) {
        var remaining = new ArrayList<>(List.of(responses));
        when(facets.execute(any())).thenAnswer(invocation -> {
            ComputeFacetsUseCase.Input input = invocation.getArgument(0);
            facetsRequests.add(input.request());
            return new ComputeFacetsUseCase.Output(remaining.size() > 1 ? remaining.removeFirst() : remaining.getFirst());
        });
    }

    private static FacetsResponse requests(FacetBucketResponse... buckets) {
        return new FacetsResponse(
            List.of(new MetricFacetsResponse(MetricSpec.Name.HTTP_REQUESTS, MetricSpec.Unit.NUMBER, List.of(buckets)))
        );
    }

    private static FacetBucketResponse count(String key, long count) {
        return new FacetBucketResponse(key, key, null, List.of(new Measure(MetricSpec.Measure.COUNT, count)));
    }

    private static MetricMeasuresResponse p95(MetricSpec.Name metric, double value) {
        return new MetricMeasuresResponse(metric, MetricSpec.Unit.MILLISECONDS, List.of(new Measure(MetricSpec.Measure.P95, value)));
    }
}
