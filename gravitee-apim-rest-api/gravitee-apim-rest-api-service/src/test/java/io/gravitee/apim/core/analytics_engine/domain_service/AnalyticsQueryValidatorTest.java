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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.analytics_engine.exception.InvalidQueryException;
import io.gravitee.apim.core.analytics_engine.model.*;
import io.gravitee.apim.core.analytics_engine.query_service.AnalyticsDefinitionQueryService;
import io.gravitee.apim.core.observability.model.FilterOperator;
import io.gravitee.apim.infra.domain_service.analytics_engine.definition.AnalyticsDefinitionYAMLQueryService;
import java.time.Instant;
import java.util.Arrays;
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

    /** The real catalog: which filters analytics supports is now read from it rather than from a copy. */
    private static final AnalyticsDefinitionYAMLQueryService CATALOG = new AnalyticsDefinitionYAMLQueryService();

    @BeforeEach
    void setUp() {
        var definitionQueryService = mock(AnalyticsDefinitionQueryService.class);
        when(definitionQueryService.findMetric(any())).thenAnswer(invocation -> CATALOG.findMetric(invocation.getArgument(0)));
        when(definitionQueryService.findMetric(MetricSpec.Name.HTTP_REQUESTS)).thenReturn(Optional.of(HTTP_REQUESTS_SPEC));
        when(definitionQueryService.getAllFilters()).thenReturn(CATALOG.getAllFilters());
        when(definitionQueryService.findFilter(any())).thenAnswer(invocation -> CATALOG.findFilter(invocation.getArgument(0)));
        validator = new AnalyticsQueryValidator(definitionQueryService);
    }

    @Nested
    class NullFilterValidation {

        @Test
        void should_reject_null_filter_value_in_measures_request() {
            var nullValueFilter = new Filter(FilterSpec.Name.HTTP_METHOD, FilterOperator.EQ, null);
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

    @Nested
    class UnsupportedAnalyticsFilters {

        @Test
        void should_reject_payload_filter_in_measures_request() {
            var payloadFilter = new Filter(FilterSpec.Name.PAYLOAD, FilterOperator.CONTAINS, "error");
            var request = new MeasuresRequest(
                VALID_TIME_RANGE,
                List.of(payloadFilter),
                List.of(new MetricMeasuresRequest(MetricSpec.Name.HTTP_REQUESTS, List.of(MetricSpec.Measure.COUNT)))
            );

            assertThatThrownBy(() -> validator.validateMeasuresRequest(request))
                .isInstanceOf(InvalidQueryException.class)
                .hasMessageContaining("not supported for analytics queries");
        }

        @Test
        void should_reject_exactly_the_filters_the_catalog_withholds_from_analytics() {
            // Guards the move from a hand-kept denylist to the catalog's signal axis: the set of rejected
            // filters must be exactly the logs-only ones, no more and no less. NATIVE_CLIENT_SOFTWARE_NAME
            // was among them while the analytics engine had no dimension for it; it now has one
            // (NativeApiFieldResolver), so it is accepted here like any other analytics filter.
            var rejected = Arrays.stream(FilterSpec.Name.values())
                .filter(name -> !accepts(name))
                .toList();

            assertThat(rejected).containsExactlyInAnyOrder(
                FilterSpec.Name.PAYLOAD,
                FilterSpec.Name.ERROR_KEY,
                FilterSpec.Name.REQUEST_ID,
                FilterSpec.Name.TRANSACTION_ID,
                // A version detached from its library is not a readable grouping — several clients share
                // version numbers — so it narrows a library on the logs screen and has no analytics dimension.
                FilterSpec.Name.NATIVE_CLIENT_SOFTWARE_VERSION
            );
        }

        @Test
        void should_keep_accepting_names_the_catalog_does_not_describe() {
            // MESSAGE_CONNECTOR_ID and the edge dimensions are known to the engines but absent from the catalog
            // (pinned by AnalyticsDefinitionYAMLQueryServiceTest). Deriving from the catalog must not start
            // rejecting them.
            assertThat(accepts(FilterSpec.Name.MESSAGE_CONNECTOR_ID)).isTrue();
            assertThat(accepts(FilterSpec.Name.EDGE_VERSION)).isTrue();
        }

        private boolean accepts(FilterSpec.Name name) {
            var request = new MeasuresRequest(
                VALID_TIME_RANGE,
                List.of(new Filter(name, FilterOperator.EQ, "value")),
                List.of(new MetricMeasuresRequest(MetricSpec.Name.HTTP_REQUESTS, List.of(MetricSpec.Measure.COUNT)))
            );
            try {
                validator.validateMeasuresRequest(request);
                return true;
            } catch (InvalidQueryException e) {
                return !e.getMessage().contains("not supported for analytics queries");
            }
        }
    }

    @Test
    void should_accept_an_authz_decision_facet_on_the_authz_decisions_metric() {
        var request = new FacetsRequest(
            VALID_TIME_RANGE,
            List.of(),
            List.of(new FacetMetricMeasuresRequest(MetricSpec.Name.AUTHZ_DECISIONS, List.of(MetricSpec.Measure.COUNT), List.of())),
            List.of(FacetSpec.Name.AUTHZ_DECISION),
            null,
            List.of()
        );

        assertThatCode(() -> validator.validateFacetsRequest(request)).doesNotThrowAnyException();
    }

    @Test
    void should_reject_an_http_facet_on_an_authz_metric() {
        var request = new FacetsRequest(
            VALID_TIME_RANGE,
            List.of(),
            List.of(new FacetMetricMeasuresRequest(MetricSpec.Name.AUTHZ_DECISIONS, List.of(MetricSpec.Measure.COUNT), List.of())),
            List.of(FacetSpec.Name.HTTP_STATUS),
            null,
            List.of()
        );

        assertThatThrownBy(() -> validator.validateFacetsRequest(request)).isInstanceOf(InvalidQueryException.class);
    }
}
