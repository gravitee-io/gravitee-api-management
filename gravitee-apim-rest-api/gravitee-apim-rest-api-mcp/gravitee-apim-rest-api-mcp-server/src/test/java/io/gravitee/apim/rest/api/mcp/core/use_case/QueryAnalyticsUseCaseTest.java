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

import static io.gravitee.apim.rest.api.mcp.fakes.Fakes.MCP_CALLER;
import static io.gravitee.apim.rest.api.mcp.fakes.Fakes.NOW;
import static io.gravitee.apim.rest.api.mcp.fakes.Fakes.PROD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import io.gravitee.apim.core.analytics_engine.exception.InvalidQueryException;
import io.gravitee.apim.core.analytics_engine.model.FacetBucketResponse;
import io.gravitee.apim.core.analytics_engine.model.FacetMetricMeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.FacetSpec;
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
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesResponse;
import io.gravitee.apim.core.observability.model.FilterOperator;
import io.gravitee.apim.core.observability.model.NumberRange;
import io.gravitee.apim.rest.api.mcp.core.TriageException;
import io.gravitee.apim.rest.api.mcp.core.model.AnalyticsQuery;
import io.gravitee.apim.rest.api.mcp.core.model.ApiRef;
import io.gravitee.apim.rest.api.mcp.core.model.TimeWindow;
import io.gravitee.apim.rest.api.mcp.fakes.Fakes;
import io.gravitee.common.utils.TimeProvider;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QueryAnalyticsUseCaseTest {

    private static final TimeWindow HOUR = TimeWindow.parse("1h", null, NOW);
    private static final AnalyticsQuery.Metric REQUESTS = new AnalyticsQuery.Metric("http_requests", List.of("count"), null, null);

    private final Fakes.StubAnalyticsQueries analytics = new Fakes.StubAnalyticsQueries();
    private final QueryAnalyticsUseCase useCase = new QueryAnalyticsUseCase(
        Fakes.environments(PROD),
        Fakes.apis(new ApiRef("orders-id", "Orders")),
        analytics
    );

    @BeforeEach
    void setUp() {
        TimeProvider.overrideClock(Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @AfterEach
    void tearDown() {
        TimeProvider.reset();
    }

    @Test
    void should_ask_the_engine_for_measures_over_the_window_and_map_them_by_name() {
        analytics.measures = new MeasuresResponse(
            List.of(
                new MetricMeasuresResponse(
                    MetricSpec.Name.HTTP_REQUESTS,
                    MetricSpec.Unit.NUMBER,
                    List.of(new Measure(MetricSpec.Measure.COUNT, 1200))
                )
            )
        );

        var output = useCase.execute(input("measures", List.of(REQUESTS), null, null, null, null));

        assertThat(output.mode()).isEqualTo("MEASURES");
        assertThat(output.environment()).isEqualTo(PROD);
        assertThat(output.window()).isEqualTo(HOUR);
        assertThat(output.previous()).isNull();
        assertThat(output.current()).hasSize(1);
        assertThat(output.current().getFirst().metric()).isEqualTo("HTTP_REQUESTS");
        assertThat(output.current().getFirst().unit()).isEqualTo("NUMBER");
        assertThat(output.current().getFirst().measures()).containsExactly(entry("COUNT", 1200));
        var request = analytics.measuresRequests.getFirst();
        assertThat(request.timeRange()).isEqualTo(new TimeRange(HOUR.from(), HOUR.to()));
        assertThat(request.metrics().getFirst().name()).isEqualTo(MetricSpec.Name.HTTP_REQUESTS);
        assertThat(request.metrics().getFirst().measures()).containsExactly(MetricSpec.Measure.COUNT);
        assertThat(analytics.callers.getFirst().environmentId()).isEqualTo("e-prod");
    }

    @Test
    void should_run_the_same_query_over_the_preceding_range_when_asked() {
        var output = useCase.execute(input("measures", List.of(REQUESTS), null, null, null, "previous"));

        assertThat(output.previousWindow()).isEqualTo(HOUR.baseline());
        assertThat(output.previous()).isNotNull();
        assertThat(analytics.measuresRequests)
            .extracting(request -> request.timeRange().from())
            .containsExactly(HOUR.from(), HOUR.baseline().from());
    }

    @Test
    void should_split_by_facets_with_filters_sorts_ranges_and_limit() {
        analytics.facets = new FacetsResponse(
            List.of(
                new MetricFacetsResponse(
                    MetricSpec.Name.HTTP_REQUESTS,
                    MetricSpec.Unit.NUMBER,
                    List.of(
                        new FacetBucketResponse(
                            "orders-id",
                            "Orders",
                            List.of(
                                new FacetBucketResponse(
                                    "400-499",
                                    "4xx Client Error",
                                    null,
                                    List.of(new Measure(MetricSpec.Measure.COUNT, 30))
                                )
                            ),
                            null
                        )
                    )
                )
            )
        );
        var metric = new AnalyticsQuery.Metric(
            "HTTP_REQUESTS",
            List.of("COUNT"),
            List.of(new AnalyticsQuery.Filter("HTTP_STATUS", "GTE", 400)),
            List.of(new AnalyticsQuery.Sort("COUNT", "desc"))
        );
        var input = new QueryAnalyticsUseCase.Input(
            MCP_CALLER,
            null,
            "facets",
            List.of(metric),
            List.of(new AnalyticsQuery.Filter("API", "IN", List.of("Orders")), new AnalyticsQuery.Filter("HTTP_METHOD", "EQ", "GET")),
            List.of("api", "HTTP_STATUS_CODE_GROUP"),
            null,
            5,
            List.of(new AnalyticsQuery.Range(400, 499)),
            null,
            null,
            null
        );

        var output = useCase.execute(input);

        var bucket = output.current().getFirst().buckets().getFirst();
        assertThat(bucket.key()).isEqualTo("orders-id");
        assertThat(bucket.name()).isEqualTo("Orders");
        assertThat(bucket.buckets().getFirst().key()).isEqualTo("400-499");
        assertThat(bucket.buckets().getFirst().measures()).containsExactly(entry("COUNT", 30));
        var request = analytics.facetsRequests.getFirst();
        assertThat(request.facets()).containsExactly(FacetSpec.Name.API, FacetSpec.Name.HTTP_STATUS_CODE_GROUP);
        assertThat(request.limit()).isEqualTo(5);
        assertThat(request.ranges()).containsExactly(new NumberRange(400, 499));
        assertThat(request.filters()).containsExactly(
            new Filter(FilterSpec.Name.API, FilterOperator.IN, List.of("orders-id")),
            new Filter(FilterSpec.Name.HTTP_METHOD, FilterOperator.EQ, "GET")
        );
        assertThat(request.metrics().getFirst().filters()).containsExactly(
            new Filter(FilterSpec.Name.HTTP_STATUS, FilterOperator.GTE, 400)
        );
        assertThat(request.metrics().getFirst().sorts()).containsExactly(
            new FacetMetricMeasuresRequest.Sort(MetricSpec.Measure.COUNT, FacetMetricMeasuresRequest.Sort.Order.DESC)
        );
    }

    @Test
    void should_build_a_time_series_with_the_interval_and_date_each_point() {
        long first = HOUR.from().toEpochMilli();
        analytics.timeSeries = new TimeSeriesResponse(
            List.of(
                new TimeSeriesMetricResponse(
                    MetricSpec.Name.HTTP_REQUESTS,
                    MetricSpec.Unit.NUMBER,
                    List.of(new TimeSeriesBucketResponse("t", null, first, null, List.of(new Measure(MetricSpec.Measure.COUNT, 7))))
                )
            )
        );

        var output = useCase.execute(input("time_series", List.of(REQUESTS), null, "2m", null, null));

        assertThat(analytics.timeSeriesRequests.getFirst().interval()).isEqualTo(120_000L);
        var point = output.current().getFirst().buckets().getFirst();
        assertThat(point.at()).isEqualTo(Instant.ofEpochMilli(first));
        assertThat(point.measures()).containsExactly(entry("COUNT", 7));
    }

    @Test
    void should_require_an_interval_for_a_time_series() {
        assertThatThrownBy(() -> useCase.execute(input("time_series", List.of(REQUESTS), null, null, null, null)))
            .isInstanceOf(TriageException.class)
            .hasMessageContaining("interval is required");
    }

    @Test
    void should_name_the_catalog_when_a_metric_measure_facet_or_filter_is_unknown() {
        assertThatThrownBy(() ->
            useCase.execute(
                input("measures", List.of(new AnalyticsQuery.Metric("REQUESTS", List.of("COUNT"), null, null)), null, null, null, null)
            )
        )
            .isInstanceOf(TriageException.class)
            .hasMessage("Unknown metric 'REQUESTS'. Call describe_metrics to list them.");
        assertThatThrownBy(() -> useCase.execute(input("facets", List.of(REQUESTS), List.of("country"), null, null, null)))
            .isInstanceOf(TriageException.class)
            .hasMessageContaining("Unknown facet 'country'");
    }

    @Test
    void should_turn_an_engine_validation_error_into_a_tool_error() {
        analytics.failure = new InvalidQueryException("Facet HTTP_PATH is not supported by metric MESSAGES");

        assertThatThrownBy(() -> useCase.execute(input("measures", List.of(REQUESTS), null, null, null, null)))
            .isInstanceOf(TriageException.class)
            .hasMessageContaining("Facet HTTP_PATH is not supported by metric MESSAGES");
    }

    @Test
    void should_reject_an_unknown_mode_or_comparison() {
        assertThatThrownBy(() -> useCase.execute(input("histogram", List.of(REQUESTS), null, null, null, null)))
            .isInstanceOf(TriageException.class)
            .hasMessageContaining("measures, facets or time_series");
        assertThatThrownBy(() -> useCase.execute(input("measures", List.of(REQUESTS), null, null, null, "last-week")))
            .isInstanceOf(TriageException.class)
            .hasMessageContaining("'previous'");
    }

    private static QueryAnalyticsUseCase.Input input(
        String mode,
        List<AnalyticsQuery.Metric> metrics,
        List<String> by,
        Object interval,
        Integer limit,
        String compareWith
    ) {
        return new QueryAnalyticsUseCase.Input(MCP_CALLER, null, mode, metrics, null, by, interval, limit, null, null, null, compareWith);
    }
}
