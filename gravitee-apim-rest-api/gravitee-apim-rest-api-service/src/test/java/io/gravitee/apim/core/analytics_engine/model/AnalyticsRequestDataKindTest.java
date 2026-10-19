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
package io.gravitee.apim.core.analytics_engine.model;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.core.analytics.model.ApiDataKind;
import io.gravitee.apim.core.observability.model.FilterOperator;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AnalyticsRequestDataKindTest {

    private static final TimeRange TIME_RANGE = new TimeRange(Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-02T00:00:00Z"));
    private static final Filter BY_METHOD = new Filter(FilterSpec.Name.HTTP_METHOD, FilterOperator.EQ, "GET");
    private static final Filter BY_CONSUMER_IP = new Filter(FilterSpec.Name.CONSUMER_IP, FilterOperator.EQ, "10.0.0.1");

    @Nested
    class Measures {

        @Test
        void should_read_the_aggregates_when_no_field_identifies_a_caller() {
            var request = new MeasuresRequest(TIME_RANGE, List.of(BY_METHOD), List.of(requests(List.of(BY_METHOD))));

            assertThat(request.dataKind()).isEqualTo(ApiDataKind.AGGREGATES);
        }

        @Test
        void should_read_the_requests_when_a_condition_identifies_a_caller() {
            var request = new MeasuresRequest(TIME_RANGE, List.of(BY_CONSUMER_IP), List.of(requests(List.of())));

            assertThat(request.dataKind()).isEqualTo(ApiDataKind.PER_REQUEST);
        }

        @Test
        void should_read_the_requests_when_a_metric_condition_identifies_a_caller() {
            var request = new MeasuresRequest(TIME_RANGE, List.of(), List.of(requests(List.of(BY_CONSUMER_IP))));

            assertThat(request.dataKind()).isEqualTo(ApiDataKind.PER_REQUEST);
        }

        @Test
        void should_read_the_aggregates_when_the_request_carries_no_condition_at_all() {
            var request = new MeasuresRequest(TIME_RANGE, null, null);

            assertThat(request.dataKind()).isEqualTo(ApiDataKind.AGGREGATES);
        }
    }

    @Nested
    class Facets {

        @Test
        void should_read_the_aggregates_when_no_facet_identifies_a_caller() {
            var request = new FacetsRequest(TIME_RANGE, List.of(), List.of(), List.of(FacetSpec.Name.HTTP_STATUS), 10, List.of());

            assertThat(request.dataKind()).isEqualTo(ApiDataKind.AGGREGATES);
        }

        @Test
        void should_read_the_requests_when_a_facet_identifies_a_caller() {
            var request = new FacetsRequest(TIME_RANGE, List.of(), List.of(), List.of(FacetSpec.Name.AUTHZ_SUBJECT_ID), 10, List.of());

            assertThat(request.dataKind()).isEqualTo(ApiDataKind.PER_REQUEST);
        }

        @Test
        void should_read_the_requests_when_a_metric_condition_identifies_a_caller() {
            var request = new FacetsRequest(
                TIME_RANGE,
                List.of(),
                List.of(
                    new FacetMetricMeasuresRequest(
                        MetricSpec.Name.HTTP_REQUESTS,
                        List.of(MetricSpec.Measure.COUNT),
                        List.of(BY_CONSUMER_IP),
                        List.of()
                    )
                ),
                List.of(FacetSpec.Name.HTTP_STATUS),
                10,
                List.of()
            );

            assertThat(request.dataKind()).isEqualTo(ApiDataKind.PER_REQUEST);
        }
    }

    @Nested
    class TimeSeries {

        @Test
        void should_read_the_aggregates_when_no_field_identifies_a_caller() {
            var request = new TimeSeriesRequest(TIME_RANGE, 3_600_000L, List.of(BY_METHOD));

            assertThat(request.dataKind()).isEqualTo(ApiDataKind.AGGREGATES);
        }

        @Test
        void should_read_the_requests_when_a_condition_identifies_a_caller() {
            var request = new TimeSeriesRequest(
                TIME_RANGE,
                3_600_000L,
                List.of(new Filter(FilterSpec.Name.NATIVE_CLIENT_ID, FilterOperator.EQ, "c-1"))
            );

            assertThat(request.dataKind()).isEqualTo(ApiDataKind.PER_REQUEST);
        }

        @Test
        void should_read_the_requests_when_a_facet_identifies_a_caller() {
            var request = new TimeSeriesRequest(
                TIME_RANGE,
                3_600_000L,
                List.of(),
                List.of(),
                List.of(FacetSpec.Name.CONSUMER_IP),
                5,
                List.of()
            );

            assertThat(request.dataKind()).isEqualTo(ApiDataKind.PER_REQUEST);
        }
    }

    private static MetricMeasuresRequest requests(List<Filter> filters) {
        return new MetricMeasuresRequest(MetricSpec.Name.HTTP_REQUESTS, List.of(MetricSpec.Measure.COUNT), filters);
    }
}
