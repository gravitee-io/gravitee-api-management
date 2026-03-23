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
package io.gravitee.apim.core.analytics_engine.domain_service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.analytics_engine.exception.InvalidQueryException;
import io.gravitee.apim.core.analytics_engine.model.*;
import io.gravitee.apim.core.analytics_engine.query_service.AnalyticsDefinitionQueryService;
import io.gravitee.apim.core.observability.model.FilterOperator;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AnalyticsQueryValidatorTest {

    private static final TimeRange VALID_TIME_RANGE = new TimeRange(
        Instant.parse("2025-01-01T00:00:00Z"),
        Instant.parse("2025-01-02T00:00:00Z")
    );

    private static final MetricSpec HTTP_REQUESTS_SPEC = new MetricSpec(
        MetricSpec.Name.HTTP_REQUESTS,
        "HTTP Requests",
        List.of(),
        MetricSpec.Unit.NUMBER,
        List.of(MetricSpec.Measure.COUNT),
        List.of(),
        List.of(FacetSpec.Name.HTTP_STATUS)
    );

    private AnalyticsQueryValidator validator;

    @BeforeEach
    void setUp() {
        var definitionQueryService = mock(AnalyticsDefinitionQueryService.class);
        when(definitionQueryService.findMetric(any())).thenReturn(Optional.of(HTTP_REQUESTS_SPEC));
        validator = new AnalyticsQueryValidator(definitionQueryService);
    }

    @Nested
    class NullFilterValidation {

        @Test
        void should_reject_null_filter_value_in_measures_request() {
            var nullValueFilter = new Filter(FilterSpec.Name.API_TYPE, FilterOperator.EQ, null);
            var request = new MeasuresRequest(
                VALID_TIME_RANGE,
                List.of(nullValueFilter),
                List.of(new MetricMeasuresRequest(MetricSpec.Name.HTTP_REQUESTS, List.of(MetricSpec.Measure.COUNT)))
            );

            assertThatThrownBy(() -> validator.validateMeasuresRequest(request))
                .isInstanceOf(InvalidQueryException.class)
                .hasMessageContaining("requires a non-null value");
        }

        @Test
        void should_reject_null_filter_value_in_facets_request() {
            var nullValueFilter = new Filter(FilterSpec.Name.APPLICATION, FilterOperator.EQ, null);
            var request = new FacetsRequest(
                VALID_TIME_RANGE,
                List.of(nullValueFilter),
                List.of(new FacetMetricMeasuresRequest(MetricSpec.Name.HTTP_REQUESTS, List.of(MetricSpec.Measure.COUNT), List.of())),
                List.of(FacetSpec.Name.HTTP_STATUS),
                null,
                List.of()
            );

            assertThatThrownBy(() -> validator.validateFacetsRequest(request))
                .isInstanceOf(InvalidQueryException.class)
                .hasMessageContaining("requires a non-null value");
        }

        @Test
        void should_reject_null_filter_value_in_time_series_request() {
            var nullValueFilter = new Filter(FilterSpec.Name.GATEWAY, FilterOperator.EQ, null);
            var request = new TimeSeriesRequest(
                VALID_TIME_RANGE,
                3600000L,
                List.of(nullValueFilter),
                List.of(new FacetMetricMeasuresRequest(MetricSpec.Name.HTTP_REQUESTS, List.of(MetricSpec.Measure.COUNT), List.of())),
                List.of(),
                null,
                List.of()
            );

            assertThatThrownBy(() -> validator.validateTimeSeriesRequest(request))
                .isInstanceOf(InvalidQueryException.class)
                .hasMessageContaining("requires a non-null value");
        }

        @Test
        void should_reject_null_filter_value_in_metric_level_filters() {
            var nullValueFilter = new Filter(FilterSpec.Name.API, FilterOperator.EQ, null);
            var request = new MeasuresRequest(
                VALID_TIME_RANGE,
                List.of(),
                List.of(
                    new MetricMeasuresRequest(MetricSpec.Name.HTTP_REQUESTS, List.of(MetricSpec.Measure.COUNT), List.of(nullValueFilter))
                )
            );

            assertThatThrownBy(() -> validator.validateMeasuresRequest(request))
                .isInstanceOf(InvalidQueryException.class)
                .hasMessageContaining("requires a non-null value");
        }

        @Test
        void should_reject_null_filter_name() {
            var nullNameFilter = new Filter(null, FilterOperator.EQ, "some-value");
            var request = new MeasuresRequest(
                VALID_TIME_RANGE,
                List.of(nullNameFilter),
                List.of(new MetricMeasuresRequest(MetricSpec.Name.HTTP_REQUESTS, List.of(MetricSpec.Measure.COUNT)))
            );

            assertThatThrownBy(() -> validator.validateMeasuresRequest(request))
                .isInstanceOf(InvalidQueryException.class)
                .hasMessageContaining("Filter name cannot be null");
        }

        @Test
        void should_accept_valid_filters() {
            var validFilter = new Filter(FilterSpec.Name.API, FilterOperator.EQ, "api-1");
            var request = new MeasuresRequest(
                VALID_TIME_RANGE,
                List.of(validFilter),
                List.of(new MetricMeasuresRequest(MetricSpec.Name.HTTP_REQUESTS, List.of(MetricSpec.Measure.COUNT)))
            );

            validator.validateMeasuresRequest(request);
        }

        @Test
        void should_accept_null_filters_list() {
            var request = new MeasuresRequest(
                VALID_TIME_RANGE,
                null,
                List.of(new MetricMeasuresRequest(MetricSpec.Name.HTTP_REQUESTS, List.of(MetricSpec.Measure.COUNT)))
            );

            validator.validateMeasuresRequest(request);
        }
    }
}
