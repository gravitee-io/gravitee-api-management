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
package io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.repository.analytics.engine.api.metric.Measure;
import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.Filter;
import io.gravitee.repository.analytics.engine.api.query.MeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.TimeRange;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
class FilterAdapterTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final Long FROM = 1756104349879L;
    private static final Long TO = 1756190749879L;
    private static final String API_ID = "273f4728-1e30-4c78-bf47-281e304c78a5";

    private final HTTPMeasuresQueryAdapter measuresAdapter = new HTTPMeasuresQueryAdapter();

    private TimeRange buildTimeRange() {
        return new TimeRange(Instant.ofEpochMilli(FROM), Instant.ofEpochMilli(TO));
    }

    @Nested
    class StandardFilters {

        @Test
        void should_generate_term_query_for_eq_filter() throws JsonProcessingException {
            var filters = List.of(new Filter(Filter.Name.API, Filter.Operator.EQ, API_ID));
            var metrics = List.of(new MetricMeasuresQuery(Metric.HTTP_REQUESTS, Set.of(Measure.COUNT)));
            var query = new MeasuresQuery(buildTimeRange(), filters, metrics);

            var queryString = measuresAdapter.adapt(query);
            var jsonQuery = JSON.readTree(queryString);

            var filter = jsonQuery.at("/query/bool/filter");
            assertThat(filter.isMissingNode()).isFalse();

            // Find the term filter (index 1, after the time range filter at index 0)
            var termFilter = filter.at("/1/term/api-id");
            assertThat(termFilter.isMissingNode()).isFalse();
            assertThat(termFilter.asText()).isEqualTo(API_ID);
        }

        @Test
        void should_generate_terms_query_for_in_filter() throws JsonProcessingException {
            var apiIds = List.of("api-1", "api-2", "api-3");
            var filters = List.of(new Filter(Filter.Name.API, Filter.Operator.IN, apiIds));
            var metrics = List.of(new MetricMeasuresQuery(Metric.HTTP_REQUESTS, Set.of(Measure.COUNT)));
            var query = new MeasuresQuery(buildTimeRange(), filters, metrics);

            var queryString = measuresAdapter.adapt(query);
            var jsonQuery = JSON.readTree(queryString);

            var termsFilter = jsonQuery.at("/query/bool/filter/1/terms/api-id");
            assertThat(termsFilter.isMissingNode()).isFalse();
            assertThat(termsFilter.isArray()).isTrue();
            assertThat(termsFilter.size()).isEqualTo(3);
            assertThat(termsFilter.get(0).asText()).isEqualTo("api-1");
            assertThat(termsFilter.get(1).asText()).isEqualTo("api-2");
            assertThat(termsFilter.get(2).asText()).isEqualTo("api-3");
        }

        @Test
        void should_generate_term_query_when_http_status_uses_eq_with_numeric_value() throws JsonProcessingException {
            var filters = List.of(new Filter(Filter.Name.HTTP_STATUS, Filter.Operator.EQ, 404));
            var metrics = List.of(new MetricMeasuresQuery(Metric.HTTP_REQUESTS, Set.of(Measure.COUNT)));
            var query = new MeasuresQuery(buildTimeRange(), filters, metrics);

            var queryString = measuresAdapter.adapt(query);
            var jsonQuery = JSON.readTree(queryString);

            // HTTP_STATUS should go through the generic term path, NOT the range path
            var termFilter = jsonQuery.at("/query/bool/filter/1/term/status");
            assertThat(termFilter.isMissingNode()).isFalse();
            assertThat(termFilter.asInt()).isEqualTo(404);

            // Verify no range query was generated
            var rangeFilter = jsonQuery.at("/query/bool/filter/1/range/status");
            assertThat(rangeFilter.isMissingNode()).isTrue();
        }
    }

    @Nested
    class StatusCodeGroupFilters {

        @Test
        void should_generate_range_query_for_eq_status_code_group_filter() throws JsonProcessingException {
            var filters = List.of(new Filter(Filter.Name.HTTP_STATUS_CODE_GROUP, Filter.Operator.EQ, "2XX"));
            var metrics = List.of(new MetricMeasuresQuery(Metric.HTTP_REQUESTS, Set.of(Measure.COUNT)));
            var query = new MeasuresQuery(buildTimeRange(), filters, metrics);

            var queryString = measuresAdapter.adapt(query);
            var jsonQuery = JSON.readTree(queryString);

            var rangeFilter = jsonQuery.at("/query/bool/filter/1/range/status");
            assertThat(rangeFilter.isMissingNode()).isFalse();
            assertThat(rangeFilter.get("gte").asInt()).isEqualTo(200);
            assertThat(rangeFilter.get("lte").asInt()).isEqualTo(299);
        }

        @Test
        void should_generate_bool_should_range_query_for_in_status_code_group_filter() throws JsonProcessingException {
            var filters = List.of(new Filter(Filter.Name.HTTP_STATUS_CODE_GROUP, Filter.Operator.IN, List.of("2XX", "5XX")));
            var metrics = List.of(new MetricMeasuresQuery(Metric.HTTP_REQUESTS, Set.of(Measure.COUNT)));
            var query = new MeasuresQuery(buildTimeRange(), filters, metrics);

            var queryString = measuresAdapter.adapt(query);
            var jsonQuery = JSON.readTree(queryString);

            var boolShould = jsonQuery.at("/query/bool/filter/1/bool/should");
            assertThat(boolShould.isMissingNode()).isFalse();
            assertThat(boolShould.isArray()).isTrue();
            assertThat(boolShould.size()).isEqualTo(2);

            // First range: 2XX → 200-299
            var range2xx = boolShould.get(0).at("/range/status");
            assertThat(range2xx.get("gte").asInt()).isEqualTo(200);
            assertThat(range2xx.get("lte").asInt()).isEqualTo(299);

            // Second range: 5XX → 500-599
            var range5xx = boolShould.get(1).at("/range/status");
            assertThat(range5xx.get("gte").asInt()).isEqualTo(500);
            assertThat(range5xx.get("lte").asInt()).isEqualTo(599);

            var minimumShouldMatch = jsonQuery.at("/query/bool/filter/1/bool/minimum_should_match");
            assertThat(minimumShouldMatch.asInt()).isEqualTo(1);
        }

        @Test
        void should_generate_range_query_for_single_value_in_list() throws JsonProcessingException {
            var filters = List.of(new Filter(Filter.Name.HTTP_STATUS_CODE_GROUP, Filter.Operator.IN, List.of("4XX")));
            var metrics = List.of(new MetricMeasuresQuery(Metric.HTTP_REQUESTS, Set.of(Measure.COUNT)));
            var query = new MeasuresQuery(buildTimeRange(), filters, metrics);

            var queryString = measuresAdapter.adapt(query);
            var jsonQuery = JSON.readTree(queryString);

            // Single-element IN should produce a direct range query without bool/should wrapper
            var rangeFilter = jsonQuery.at("/query/bool/filter/1/range/status");
            assertThat(rangeFilter.isMissingNode()).isFalse();
            assertThat(rangeFilter.get("gte").asInt()).isEqualTo(400);
            assertThat(rangeFilter.get("lte").asInt()).isEqualTo(499);
        }

        @Test
        void should_generate_range_query_when_status_code_group_is_lowercase() throws JsonProcessingException {
            var filters = List.of(new Filter(Filter.Name.HTTP_STATUS_CODE_GROUP, Filter.Operator.EQ, "2xx"));
            var metrics = List.of(new MetricMeasuresQuery(Metric.HTTP_REQUESTS, Set.of(Measure.COUNT)));
            var query = new MeasuresQuery(buildTimeRange(), filters, metrics);

            var queryString = measuresAdapter.adapt(query);
            var jsonQuery = JSON.readTree(queryString);

            var rangeFilter = jsonQuery.at("/query/bool/filter/1/range/status");
            assertThat(rangeFilter.isMissingNode()).isFalse();
            assertThat(rangeFilter.get("gte").asInt()).isEqualTo(200);
            assertThat(rangeFilter.get("lte").asInt()).isEqualTo(299);
        }

        @Test
        void should_throw_for_unknown_status_code_group() {
            var filterAdapter = new FilterAdapter(new HTTPFieldResolver());
            var filter = new Filter(Filter.Name.HTTP_STATUS_CODE_GROUP, Filter.Operator.EQ, "9XX");

            assertThatThrownBy(() -> filterAdapter.adaptMetricFilters(List.of(filter)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown status code group: 9XX");
        }

        @Test
        void should_throw_for_null_status_code_group() {
            var filterAdapter = new FilterAdapter(new HTTPFieldResolver());
            var filter = new Filter(Filter.Name.HTTP_STATUS_CODE_GROUP, Filter.Operator.EQ, null);

            assertThatThrownBy(() -> filterAdapter.adaptMetricFilters(List.of(filter)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Status code group must not be null");
        }

        @Test
        void should_generate_match_none_for_empty_collection() throws JsonProcessingException {
            var filters = List.of(new Filter(Filter.Name.HTTP_STATUS_CODE_GROUP, Filter.Operator.IN, List.of()));
            var metrics = List.of(new MetricMeasuresQuery(Metric.HTTP_REQUESTS, Set.of(Measure.COUNT)));
            var query = new MeasuresQuery(buildTimeRange(), filters, metrics);

            var queryString = measuresAdapter.adapt(query);
            var jsonQuery = JSON.readTree(queryString);

            var matchNone = jsonQuery.at("/query/bool/filter/1/match_none");
            assertThat(matchNone.isMissingNode()).isFalse();
        }

        @Test
        void should_generate_match_none_for_null_collection() throws JsonProcessingException {
            var filters = List.of(new Filter(Filter.Name.HTTP_STATUS_CODE_GROUP, Filter.Operator.IN, null));
            var metrics = List.of(new MetricMeasuresQuery(Metric.HTTP_REQUESTS, Set.of(Measure.COUNT)));
            var query = new MeasuresQuery(buildTimeRange(), filters, metrics);

            var queryString = measuresAdapter.adapt(query);
            var jsonQuery = JSON.readTree(queryString);

            var matchNone = jsonQuery.at("/query/bool/filter/1/match_none");
            assertThat(matchNone.isMissingNode()).isFalse();
        }

        @Test
        void should_generate_all_five_ranges_for_in_with_all_groups() throws JsonProcessingException {
            var allGroups = List.of("1XX", "2XX", "3XX", "4XX", "5XX");
            var filters = List.of(new Filter(Filter.Name.HTTP_STATUS_CODE_GROUP, Filter.Operator.IN, allGroups));
            var metrics = List.of(new MetricMeasuresQuery(Metric.HTTP_REQUESTS, Set.of(Measure.COUNT)));
            var query = new MeasuresQuery(buildTimeRange(), filters, metrics);

            var queryString = measuresAdapter.adapt(query);
            var jsonQuery = JSON.readTree(queryString);

            var boolShould = jsonQuery.at("/query/bool/filter/1/bool/should");
            assertThat(boolShould.size()).isEqualTo(5);

            assertThat(boolShould.get(0).at("/range/status/gte").asInt()).isEqualTo(100);
            assertThat(boolShould.get(0).at("/range/status/lte").asInt()).isEqualTo(199);

            assertThat(boolShould.get(1).at("/range/status/gte").asInt()).isEqualTo(200);
            assertThat(boolShould.get(1).at("/range/status/lte").asInt()).isEqualTo(299);

            assertThat(boolShould.get(2).at("/range/status/gte").asInt()).isEqualTo(300);
            assertThat(boolShould.get(2).at("/range/status/lte").asInt()).isEqualTo(399);

            assertThat(boolShould.get(3).at("/range/status/gte").asInt()).isEqualTo(400);
            assertThat(boolShould.get(3).at("/range/status/lte").asInt()).isEqualTo(499);

            assertThat(boolShould.get(4).at("/range/status/gte").asInt()).isEqualTo(500);
            assertThat(boolShould.get(4).at("/range/status/lte").asInt()).isEqualTo(599);
        }
    }

    @Nested
    class MetricLevelStatusCodeGroupFilters {

        @Test
        void should_generate_range_query_for_metric_level_status_code_group_filter() throws JsonProcessingException {
            var topLevelFilters = List.of(new Filter(Filter.Name.API, Filter.Operator.IN, List.of(API_ID)));
            var metricFilters = List.of(new Filter(Filter.Name.HTTP_STATUS_CODE_GROUP, Filter.Operator.IN, List.of("2XX", "4XX")));
            var metrics = List.of(new MetricMeasuresQuery(Metric.HTTP_REQUESTS, Set.of(Measure.COUNT), metricFilters, List.of()));

            var query = new MeasuresQuery(buildTimeRange(), topLevelFilters, metrics);

            var queryString = measuresAdapter.adapt(query);
            var jsonQuery = JSON.readTree(queryString);

            var filterAgg = jsonQuery.at("/aggs/HTTP_REQUESTS#__FILTER__");
            assertThat(filterAgg.isMissingNode()).isFalse();

            var filterClause = filterAgg.at("/filter/bool/must/0/bool/should");
            assertThat(filterClause.isMissingNode()).isFalse();
            assertThat(filterClause.isArray()).isTrue();
            assertThat(filterClause.size()).isEqualTo(2);

            // First range: 2XX → 200-299
            var range2xx = filterClause.get(0).at("/range/status");
            assertThat(range2xx.get("gte").asInt()).isEqualTo(200);
            assertThat(range2xx.get("lte").asInt()).isEqualTo(299);

            // Second range: 4XX → 400-499
            var range4xx = filterClause.get(1).at("/range/status");
            assertThat(range4xx.get("gte").asInt()).isEqualTo(400);
            assertThat(range4xx.get("lte").asInt()).isEqualTo(499);
        }
    }
}
