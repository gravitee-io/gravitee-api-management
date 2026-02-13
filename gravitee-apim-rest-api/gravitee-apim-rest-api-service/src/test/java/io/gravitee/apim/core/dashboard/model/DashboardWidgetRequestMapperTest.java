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
package io.gravitee.apim.core.dashboard.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.apim.core.analytics_engine.exception.InvalidQueryException;
import io.gravitee.apim.core.analytics_engine.model.FacetsRequest;
import io.gravitee.apim.core.analytics_engine.model.MeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesRequest;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DashboardWidgetRequestMapperTest {

    private static DashboardWidget.TimeRange validTimeRange() {
        return DashboardWidget.TimeRange.builder().from("2025-10-07T06:50:30Z").to("2025-12-07T11:35:30Z").build();
    }

    private static DashboardWidget.MetricRequest metricRequest(String name, String... measures) {
        return DashboardWidget.MetricRequest.builder().name(name).measures(List.of(measures)).build();
    }

    @Nested
    class ToAnalyticsRequest {

        @Test
        void should_throw_when_request_is_null() {
            assertThatThrownBy(() -> DashboardWidgetRequestMapper.toAnalyticsRequest(null))
                .isInstanceOf(InvalidQueryException.class)
                .hasMessageContaining("Widget request cannot be null");
        }

        @Test
        void should_throw_when_type_is_null() {
            var request = DashboardWidget.Request.builder()
                .type(null)
                .timeRange(validTimeRange())
                .metrics(List.of(metricRequest("HTTP_REQUESTS", "COUNT")))
                .build();
            assertThatThrownBy(() -> DashboardWidgetRequestMapper.toAnalyticsRequest(request))
                .isInstanceOf(InvalidQueryException.class)
                .hasMessageContaining("type is required");
        }

        @Test
        void should_throw_when_type_is_unknown() {
            var request = DashboardWidget.Request.builder()
                .type("unknown")
                .timeRange(validTimeRange())
                .metrics(List.of(metricRequest("HTTP_REQUESTS", "COUNT")))
                .build();
            assertThatThrownBy(() -> DashboardWidgetRequestMapper.toAnalyticsRequest(request))
                .isInstanceOf(InvalidQueryException.class)
                .hasMessageContaining("Unknown widget request type");
        }
    }

    @Nested
    class ToMeasuresRequest {

        @Test
        void should_map_valid_measures_request() {
            var request = DashboardWidget.Request.builder()
                .type("measures")
                .timeRange(validTimeRange())
                .metrics(List.of(metricRequest("HTTP_REQUESTS", "COUNT")))
                .build();

            var result = DashboardWidgetRequestMapper.toMeasuresRequest(request);

            assertThat(result).isInstanceOf(MeasuresRequest.class);
            var measuresRequest = (MeasuresRequest) result;
            assertThat(measuresRequest.timeRange().from()).isNotNull();
            assertThat(measuresRequest.timeRange().to()).isNotNull();
            assertThat(measuresRequest.metrics()).hasSize(1);
            assertThat(measuresRequest.metrics().get(0).name().name()).isEqualTo("HTTP_REQUESTS");
            assertThat(measuresRequest.metrics().get(0).measures()).containsExactly(
                io.gravitee.apim.core.analytics_engine.model.MetricSpec.Measure.COUNT
            );
        }

        @Test
        void should_throw_when_time_range_is_null() {
            var request = DashboardWidget.Request.builder()
                .type("measures")
                .timeRange(null)
                .metrics(List.of(metricRequest("HTTP_REQUESTS", "COUNT")))
                .build();
            assertThatThrownBy(() -> DashboardWidgetRequestMapper.toMeasuresRequest(request))
                .isInstanceOf(InvalidQueryException.class)
                .hasMessageContaining("timeRange");
        }

        @Test
        void should_throw_when_metrics_empty() {
            var request = DashboardWidget.Request.builder().type("measures").timeRange(validTimeRange()).metrics(List.of()).build();
            assertThatThrownBy(() -> DashboardWidgetRequestMapper.toMeasuresRequest(request))
                .isInstanceOf(InvalidQueryException.class)
                .hasMessageContaining("at least one metric");
        }

        @Test
        void should_throw_when_metric_has_unknown_name() {
            var request = DashboardWidget.Request.builder()
                .type("measures")
                .timeRange(validTimeRange())
                .metrics(List.of(metricRequest("UNKNOWN_METRIC", "COUNT")))
                .build();
            assertThatThrownBy(() -> DashboardWidgetRequestMapper.toMeasuresRequest(request))
                .isInstanceOf(InvalidQueryException.class)
                .hasMessageContaining("Unknown metric name");
        }

        @Test
        void should_throw_when_measure_is_unknown() {
            var request = DashboardWidget.Request.builder()
                .type("measures")
                .timeRange(validTimeRange())
                .metrics(
                    List.of(DashboardWidget.MetricRequest.builder().name("HTTP_REQUESTS").measures(List.of("UNKNOWN_MEASURE")).build())
                )
                .build();
            assertThatThrownBy(() -> DashboardWidgetRequestMapper.toMeasuresRequest(request))
                .isInstanceOf(InvalidQueryException.class)
                .hasMessageContaining("Unknown measure name");
        }
    }

    @Nested
    class ToFacetsRequest {

        @Test
        void should_map_valid_facets_request() {
            var request = DashboardWidget.Request.builder()
                .type("facets")
                .timeRange(validTimeRange())
                .metrics(List.of(metricRequest("HTTP_REQUESTS", "COUNT")))
                .by(List.of("HTTP_STATUS_CODE_GROUP"))
                .limit(10)
                .build();

            var result = DashboardWidgetRequestMapper.toFacetsRequest(request);

            assertThat(result).isInstanceOf(FacetsRequest.class);
            var facetsRequest = (FacetsRequest) result;
            assertThat(facetsRequest.facets()).hasSize(1);
            assertThat(facetsRequest.facets().get(0).name()).isEqualTo("HTTP_STATUS_CODE_GROUP");
            assertThat(facetsRequest.limit()).isEqualTo(10);
        }

        @Test
        void should_throw_when_facet_name_unknown() {
            var request = DashboardWidget.Request.builder()
                .type("facets")
                .timeRange(validTimeRange())
                .metrics(List.of(metricRequest("HTTP_REQUESTS", "COUNT")))
                .by(List.of("UNKNOWN_FACET"))
                .build();
            assertThatThrownBy(() -> DashboardWidgetRequestMapper.toFacetsRequest(request))
                .isInstanceOf(InvalidQueryException.class)
                .hasMessageContaining("Unknown facet name");
        }
    }

    @Nested
    class ToTimeSeriesRequest {

        @Test
        void should_map_valid_time_series_request() {
            var request = DashboardWidget.Request.builder()
                .type("time-series")
                .timeRange(validTimeRange())
                .interval(3600000L)
                .metrics(List.of(metricRequest("HTTP_REQUESTS", "RPS")))
                .by(List.of("API"))
                .limit(5)
                .build();

            var result = DashboardWidgetRequestMapper.toTimeSeriesRequest(request);

            assertThat(result).isInstanceOf(TimeSeriesRequest.class);
            var timeSeriesRequest = (TimeSeriesRequest) result;
            assertThat(timeSeriesRequest.interval()).isEqualTo(3600000L);
            assertThat(timeSeriesRequest.facets()).hasSize(1);
            assertThat(timeSeriesRequest.facets().get(0).name()).isEqualTo("API");
        }

        @Test
        void should_throw_when_interval_null() {
            var request = DashboardWidget.Request.builder()
                .type("time-series")
                .timeRange(validTimeRange())
                .interval(null)
                .metrics(List.of(metricRequest("HTTP_REQUESTS", "COUNT")))
                .build();
            assertThatThrownBy(() -> DashboardWidgetRequestMapper.toTimeSeriesRequest(request))
                .isInstanceOf(InvalidQueryException.class)
                .hasMessageContaining("Negative or zero intervals");
        }

        @Test
        void should_throw_when_interval_zero() {
            var request = DashboardWidget.Request.builder()
                .type("time-series")
                .timeRange(validTimeRange())
                .interval(0L)
                .metrics(List.of(metricRequest("HTTP_REQUESTS", "COUNT")))
                .build();
            assertThatThrownBy(() -> DashboardWidgetRequestMapper.toTimeSeriesRequest(request))
                .isInstanceOf(InvalidQueryException.class)
                .hasMessageContaining("Negative or zero intervals");
        }
    }
}
