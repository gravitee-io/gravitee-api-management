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
package io.gravitee.repository.elasticsearch.v4.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.analytics.engine.api.metric.Measure;
import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.Facet;
import io.gravitee.repository.analytics.engine.api.query.FacetsQuery;
import io.gravitee.repository.analytics.engine.api.query.Filter;
import io.gravitee.repository.analytics.engine.api.query.MeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery.Sort;
import io.gravitee.repository.analytics.engine.api.query.TimeRange;
import io.gravitee.repository.analytics.engine.api.query.TimeSeriesQuery;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.elasticsearch.AbstractElasticsearchRepositoryTest;
import io.gravitee.repository.elasticsearch.TimeProvider;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@TestPropertySource(properties = "reporters.elasticsearch.template_mapping.path=src/test/resources/freemarker-v4-analytics")
class AnalyticsElasticsearchRepositoryAuthzTrafficTest extends AbstractElasticsearchRepositoryTest {

    private static final String AUTHZ_TRAFFIC_API = "authz-traffic-api-001";
    private static final String TOP_LIST_API = "authz-traffic-api-top-list";

    private static final QueryContext QUERY_CONTEXT = new QueryContext("DEFAULT", "DEFAULT");

    @Autowired
    private AnalyticsElasticsearchRepository cut;

    private static TimeRange buildTimeRange() {
        var now = TimeProvider.now().truncatedTo(ChronoUnit.DAYS);
        return new TimeRange(now.minus(Duration.ofDays(1)), now.plus(Duration.ofDays(1)));
    }

    private static Filter api() {
        return new Filter(Filter.Name.API, Filter.Operator.IN, List.of(AUTHZ_TRAFFIC_API));
    }

    private long count(Metric metric, Filter... filters) {
        var metrics = List.of(new MetricMeasuresQuery(metric, Set.of(Measure.COUNT)));
        var allFilters = Stream.concat(Stream.of(api()), Stream.of(filters)).toList();
        var result = cut.searchAuthzTrafficMeasures(QUERY_CONTEXT, new MeasuresQuery(buildTimeRange(), allFilters, metrics));
        return result.measures().getFirst().measures().get(Measure.COUNT).longValue();
    }

    @Test
    void should_count_searches_from_pdp_api_traffic() {
        assertThat(count(Metric.AUTHZ_SEARCHES)).isEqualTo(2L);
    }

    @Test
    void should_count_every_authzen_request_as_an_operation_and_ignore_other_traffic() {
        assertThat(count(Metric.AUTHZ_OPERATIONS)).isEqualTo(3L);
    }

    @Test
    void should_count_no_operation_when_filtered_on_a_caller_that_only_decisions_carry() {
        assertThat(count(Metric.AUTHZ_OPERATIONS, new Filter(Filter.Name.AUTHZ_CALLER, Filter.Operator.EQ, "pep"))).isZero();
    }

    @Test
    void should_count_no_search_when_filtered_on_a_verdict() {
        assertThat(count(Metric.AUTHZ_SEARCHES, new Filter(Filter.Name.AUTHZ_DECISION, Filter.Operator.EQ, "PERMIT"))).isZero();
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = { "AUTHZ_SUBJECT_ID | User::\"alice\"", "AUTHZ_ACTION | read", "AUTHZ_RESOURCE_ID | Document::\"doc1\"" }
    )
    void should_count_no_operation_when_filtered_on_a_field_only_searches_carry(Filter.Name name, String value) {
        assertThat(count(Metric.AUTHZ_OPERATIONS, new Filter(name, Filter.Operator.EQ, value))).isZero();
    }

    @Test
    void should_filter_searches_on_a_typed_subject() {
        assertThat(count(Metric.AUTHZ_SEARCHES, new Filter(Filter.Name.AUTHZ_SUBJECT_ID, Filter.Operator.EQ, "User::\"alice\""))).isEqualTo(
            1L
        );
    }

    @Test
    void should_filter_searches_on_a_typed_resource() {
        assertThat(
            count(Metric.AUTHZ_SEARCHES, new Filter(Filter.Name.AUTHZ_RESOURCE_ID, Filter.Operator.EQ, "Document::\"doc1\""))
        ).isEqualTo(1L);
    }

    @Test
    void should_still_count_searches_next_to_operations_filtered_on_a_subject() {
        var metrics = List.of(
            new MetricMeasuresQuery(Metric.AUTHZ_OPERATIONS, Set.of(Measure.COUNT)),
            new MetricMeasuresQuery(Metric.AUTHZ_SEARCHES, Set.of(Measure.COUNT))
        );
        var filters = List.of(api(), new Filter(Filter.Name.AUTHZ_SUBJECT_ID, Filter.Operator.EQ, "User::\"alice\""));

        var result = cut.searchAuthzTrafficMeasures(QUERY_CONTEXT, new MeasuresQuery(buildTimeRange(), filters, metrics));

        assertThat(result.measures()).satisfiesExactlyInAnyOrder(
            measure -> {
                assertThat(measure.metric()).isEqualTo(Metric.AUTHZ_OPERATIONS);
                assertThat(measure.measures().get(Measure.COUNT).longValue()).isZero();
            },
            measure -> {
                assertThat(measure.metric()).isEqualTo(Metric.AUTHZ_SEARCHES);
                assertThat(measure.measures().get(Measure.COUNT).longValue()).isEqualTo(1L);
            }
        );
    }

    @Test
    void should_facet_no_operation_when_filtered_on_a_subject() {
        var metrics = List.of(new MetricMeasuresQuery(Metric.AUTHZ_OPERATIONS, Set.of(Measure.COUNT)));
        var filters = List.of(api(), new Filter(Filter.Name.AUTHZ_SUBJECT_ID, Filter.Operator.EQ, "User::\"alice\""));

        var result = cut.searchAuthzTrafficFacets(
            QUERY_CONTEXT,
            new FacetsQuery(buildTimeRange(), filters, metrics, List.of(Facet.AUTHZ_OPERATION))
        );

        assertThat(result.metrics().getFirst().buckets()).isEmpty();
    }

    @Test
    void should_facet_searches_by_search_type() {
        var metrics = List.of(new MetricMeasuresQuery(Metric.AUTHZ_SEARCHES, Set.of(Measure.COUNT)));

        var result = cut.searchAuthzTrafficFacets(
            QUERY_CONTEXT,
            new FacetsQuery(buildTimeRange(), List.of(api()), metrics, List.of(Facet.AUTHZ_SEARCH_TYPE))
        );

        assertThat(result.metrics().getFirst().buckets()).satisfiesExactlyInAnyOrder(
            bucket -> {
                assertThat(bucket.key()).isEqualTo("subject");
                assertThat(bucket.measures().get(Measure.COUNT).longValue()).isEqualTo(1L);
            },
            bucket -> {
                assertThat(bucket.key()).isEqualTo("resource");
                assertThat(bucket.measures().get(Measure.COUNT).longValue()).isEqualTo(1L);
            }
        );
    }

    @Test
    void should_leave_out_the_operations_a_search_facet_never_counted() {
        var metrics = List.of(new MetricMeasuresQuery(Metric.AUTHZ_SEARCHES, Set.of(Measure.COUNT)));

        var result = cut.searchAuthzTrafficFacets(
            QUERY_CONTEXT,
            new FacetsQuery(buildTimeRange(), List.of(api()), metrics, List.of(Facet.AUTHZ_OPERATION))
        );

        assertThat(result.metrics().getFirst().buckets()).satisfiesExactly(bucket -> {
            assertThat(bucket.key()).isEqualTo("search");
            assertThat(bucket.measures().get(Measure.COUNT).longValue()).isEqualTo(2L);
        });
    }

    private static MetricMeasuresQuery searchesSortedAscending() {
        return new MetricMeasuresQuery(Metric.AUTHZ_SEARCHES, Set.of(Measure.COUNT), List.of(new Sort(Measure.COUNT, Sort.Order.ASC)));
    }

    @ParameterizedTest
    @CsvSource({ "AUTHZ_OPERATION, search", "AUTHZ_ACTION, read" })
    void should_limit_an_ascending_search_top_list_to_the_buckets_searches_counted(Facet facet, String searchedKey) {
        var result = cut.searchAuthzTrafficFacets(
            QUERY_CONTEXT,
            new FacetsQuery(buildTimeRange(), List.of(api()), List.of(searchesSortedAscending()), List.of(facet), 1)
        );

        assertThat(result.metrics().getFirst().buckets()).satisfiesExactly(bucket -> {
            assertThat(bucket.key()).isEqualTo(searchedKey);
            assertThat(bucket.measures().get(Measure.COUNT).longValue()).isEqualTo(2L);
        });
    }

    @Test
    void should_limit_an_ascending_search_time_series_facet_to_the_buckets_searches_counted() {
        var result = cut.searchAuthzTrafficTimeSeries(
            QUERY_CONTEXT,
            new TimeSeriesQuery(
                buildTimeRange(),
                List.of(api()),
                Duration.ofDays(1).toMillis(),
                List.of(searchesSortedAscending()),
                List.of(Facet.AUTHZ_OPERATION),
                1,
                null
            )
        );

        var facetBuckets = result
            .metrics()
            .getFirst()
            .buckets()
            .stream()
            .flatMap(bucket -> bucket.buckets().stream())
            .toList();
        assertThat(facetBuckets).satisfiesExactly(bucket -> {
            assertThat(bucket.key()).isEqualTo("search");
            assertThat(bucket.measures().get(Measure.COUNT).longValue()).isEqualTo(2L);
        });
    }

    @Test
    void should_rank_an_unsorted_search_top_list_by_searches_rather_than_by_traffic() {
        var topListApi = new Filter(Filter.Name.API, Filter.Operator.IN, List.of(TOP_LIST_API));
        var metrics = List.of(new MetricMeasuresQuery(Metric.AUTHZ_SEARCHES, Set.of(Measure.COUNT)));

        var result = cut.searchAuthzTrafficFacets(
            QUERY_CONTEXT,
            new FacetsQuery(buildTimeRange(), List.of(topListApi), metrics, List.of(Facet.AUTHZ_ACTION), 1)
        );

        assertThat(result.metrics().getFirst().buckets()).satisfiesExactly(bucket -> {
            assertThat(bucket.key()).isEqualTo("read");
            assertThat(bucket.measures().get(Measure.COUNT).longValue()).isEqualTo(1L);
        });
    }

    @Test
    void should_rank_an_unsorted_search_time_series_facet_by_searches_rather_than_by_traffic() {
        var topListApi = new Filter(Filter.Name.API, Filter.Operator.IN, List.of(TOP_LIST_API));
        var metrics = List.of(new MetricMeasuresQuery(Metric.AUTHZ_SEARCHES, Set.of(Measure.COUNT)));

        var result = cut.searchAuthzTrafficTimeSeries(
            QUERY_CONTEXT,
            new TimeSeriesQuery(
                buildTimeRange(),
                List.of(topListApi),
                Duration.ofDays(1).toMillis(),
                metrics,
                List.of(Facet.AUTHZ_ACTION),
                1,
                null
            )
        );

        var facetBuckets = result
            .metrics()
            .getFirst()
            .buckets()
            .stream()
            .flatMap(bucket -> bucket.buckets().stream())
            .toList();
        assertThat(facetBuckets).satisfiesExactly(bucket -> {
            assertThat(bucket.key()).isEqualTo("read");
            assertThat(bucket.measures().get(Measure.COUNT).longValue()).isEqualTo(1L);
        });
    }

    @Test
    void should_bucket_operations_over_time() {
        var metrics = List.of(new MetricMeasuresQuery(Metric.AUTHZ_OPERATIONS, Set.of(Measure.COUNT)));

        var result = cut.searchAuthzTrafficTimeSeries(
            QUERY_CONTEXT,
            new TimeSeriesQuery(buildTimeRange(), List.of(api()), Duration.ofHours(1).toMillis(), metrics)
        );

        assertThat(result.metrics()).hasSize(1);
        var counts = result
            .metrics()
            .getFirst()
            .buckets()
            .stream()
            .map(bucket -> bucket.measures().get(Measure.COUNT).longValue())
            .toList();
        assertThat(counts.stream().mapToLong(Long::longValue).sum()).isEqualTo(3L);
    }

    @Test
    void should_bucket_only_searches_over_time() {
        var metrics = List.of(new MetricMeasuresQuery(Metric.AUTHZ_SEARCHES, Set.of(Measure.COUNT)));

        var result = cut.searchAuthzTrafficTimeSeries(
            QUERY_CONTEXT,
            new TimeSeriesQuery(buildTimeRange(), List.of(api()), Duration.ofHours(1).toMillis(), metrics)
        );

        var counts = result
            .metrics()
            .getFirst()
            .buckets()
            .stream()
            .map(bucket -> bucket.measures().get(Measure.COUNT).longValue())
            .toList();
        assertThat(counts.stream().mapToLong(Long::longValue).sum()).isEqualTo(2L);
    }
}
