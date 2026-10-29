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
import static org.assertj.core.groups.Tuple.tuple;

import io.gravitee.elasticsearch.client.Client;
import io.gravitee.repository.analytics.engine.api.metric.Measure;
import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.AdditionalMetricField;
import io.gravitee.repository.analytics.engine.api.query.AdditionalMetricField.ValueType;
import io.gravitee.repository.analytics.engine.api.query.Facet;
import io.gravitee.repository.analytics.engine.api.query.FacetName;
import io.gravitee.repository.analytics.engine.api.query.FacetsQuery;
import io.gravitee.repository.analytics.engine.api.query.Filter;
import io.gravitee.repository.analytics.engine.api.query.MeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.NumberRange;
import io.gravitee.repository.analytics.engine.api.query.TimeRange;
import io.gravitee.repository.analytics.engine.api.query.TimeSeriesQuery;
import io.gravitee.repository.analytics.engine.api.result.FacetBucketResult;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.elasticsearch.AbstractElasticsearchRepositoryTest;
import io.gravitee.repository.elasticsearch.TimeProvider;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/**
 * Real request documents carrying additional metrics, filtered and grouped through the HTTP path. They live in a day
 * of their own, ten days back, under an API of their own, so the fixtures other suites count are untouched.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@TestPropertySource(properties = "reporters.elasticsearch.template_mapping.path=src/test/resources/freemarker-v4-analytics")
class AnalyticsElasticsearchRepositoryAdditionalMetricsTest extends AbstractElasticsearchRepositoryTest {

    private static final QueryContext QUERY_CONTEXT = new QueryContext("DEFAULT", "DEFAULT");
    private static final String API_ID = "additional-metrics-api-001";
    private static final Instant DAY = TimeProvider.now().truncatedTo(ChronoUnit.DAYS).minus(Duration.ofDays(10));
    private static final TimeRange TIME_RANGE = new TimeRange(DAY, DAY.plus(Duration.ofDays(1)));

    private static final AdditionalMetricField TIER = new AdditionalMetricField(ValueType.KEYWORD, "keyword_fraud-check_tier");
    private static final AdditionalMetricField FAILOVERS = new AdditionalMetricField(ValueType.LONG, "long_failover_count");
    private static final AdditionalMetricField SCORE = new AdditionalMetricField(ValueType.DOUBLE, "double_fraud-check_score");
    private static final AdditionalMetricField CACHE_HIT = new AdditionalMetricField(ValueType.BOOL, "bool_cache_hit");
    private static final AdditionalMetricField NEVER_WRITTEN = new AdditionalMetricField(ValueType.KEYWORD, "keyword_never_written");

    private static final MetricMeasuresQuery REQUESTS = new MetricMeasuresQuery(Metric.HTTP_REQUESTS, Set.of(Measure.COUNT));
    private static final Filter BY_API = new Filter(Filter.Name.API, Filter.Operator.IN, List.of(API_ID));

    private static boolean indexed;

    @Autowired
    private AnalyticsElasticsearchRepository cut;

    @Autowired
    private Client client;

    @BeforeEach
    void index_the_documents_once() {
        if (indexed) {
            return;
        }
        var index = "gravitee-v4-metrics-" + DateTimeFormatter.ofPattern("yyyy.MM.dd").withZone(ZoneOffset.UTC).format(DAY);
        var bulk = new StringBuilder();
        document(
            bulk,
            index,
            "am-001",
            1,
            JsonObject.of(TIER.rawKey(), "high", FAILOVERS.rawKey(), 0, SCORE.rawKey(), 0.9, CACHE_HIT.rawKey(), true)
        );
        document(
            bulk,
            index,
            "am-002",
            2,
            JsonObject.of(TIER.rawKey(), "high", FAILOVERS.rawKey(), 2, SCORE.rawKey(), 0.4, CACHE_HIT.rawKey(), false)
        );
        document(
            bulk,
            index,
            "am-003",
            3,
            JsonObject.of(TIER.rawKey(), "low", FAILOVERS.rawKey(), 5, SCORE.rawKey(), 0.1, CACHE_HIT.rawKey(), true)
        );
        document(bulk, index, "am-004", 4, JsonObject.of());
        client.bulk(List.of(Buffer.buffer(bulk.toString())), true).blockingGet();
        indexed = true;
    }

    @Test
    void should_count_only_the_requests_matching_a_keyword() {
        var filter = new Filter(TIER, Filter.Operator.EQ, "high");

        assertThat(count(List.of(BY_API, filter))).isEqualTo(2L);
    }

    @Test
    void should_count_only_the_requests_matching_one_of_several_keywords() {
        var filter = new Filter(TIER, Filter.Operator.IN, List.of("high", "low"));

        assertThat(count(List.of(BY_API, filter))).isEqualTo(3L);
    }

    @Test
    void should_count_only_the_requests_within_number_bounds() {
        var atLeastOne = new Filter(FAILOVERS, Filter.Operator.GTE, "1");
        var belowHalf = new Filter(SCORE, Filter.Operator.LTE, "0.5");

        assertThat(count(List.of(BY_API, atLeastOne))).isEqualTo(2L);
        assertThat(count(List.of(BY_API, belowHalf))).isEqualTo(2L);
    }

    @Test
    void should_count_only_the_requests_matching_a_boolean() {
        var filter = new Filter(CACHE_HIT, Filter.Operator.EQ, "true");

        assertThat(count(List.of(BY_API, filter))).isEqualTo(2L);
    }

    @Test
    void should_count_nothing_on_a_field_no_document_carries() {
        var filter = new Filter(NEVER_WRITTEN, Filter.Operator.EQ, "anything");

        assertThat(count(List.of(BY_API, filter))).isZero();
    }

    @Test
    void should_return_one_bucket_per_observed_keyword() {
        var result = cut.searchHTTPFacets(QUERY_CONTEXT, new FacetsQuery(TIME_RANGE, List.of(BY_API), List.of(REQUESTS), List.of(TIER)));

        assertThat(result.metrics().getFirst().buckets())
            .extracting(FacetBucketResult::key, bucket -> bucket.measures().get(Measure.COUNT).longValue())
            .containsExactlyInAnyOrder(tuple("high", 2L), tuple("low", 1L));
    }

    @Test
    void should_group_by_a_keyword_under_a_built_in_facet() {
        List<FacetName> facets = List.of(Facet.API, TIER);

        var result = cut.searchHTTPFacets(QUERY_CONTEXT, new FacetsQuery(TIME_RANGE, List.of(BY_API), List.of(REQUESTS), facets));

        var api = result.metrics().getFirst().buckets().getFirst();
        assertThat(api.key()).isEqualTo(API_ID);
        assertThat(api.buckets()).extracting(FacetBucketResult::key).containsExactlyInAnyOrder("high", "low");
    }

    @Test
    void should_return_two_buckets_for_a_boolean() {
        var result = cut.searchHTTPFacets(
            QUERY_CONTEXT,
            new FacetsQuery(TIME_RANGE, List.of(BY_API), List.of(REQUESTS), List.of(CACHE_HIT))
        );

        assertThat(result.metrics().getFirst().buckets())
            .extracting(FacetBucketResult::key, bucket -> bucket.measures().get(Measure.COUNT).longValue())
            .containsExactlyInAnyOrder(tuple("true", 2L), tuple("false", 1L));
    }

    @Test
    void should_group_a_number_by_the_requested_ranges() {
        var ranges = List.of(new NumberRange(0, 1), new NumberRange(2, 10));

        var result = cut.searchHTTPFacets(
            QUERY_CONTEXT,
            new FacetsQuery(TIME_RANGE, List.of(BY_API), List.of(REQUESTS), List.of(FAILOVERS), ranges)
        );

        assertThat(result.metrics().getFirst().buckets())
            .extracting(FacetBucketResult::key, bucket -> bucket.measures().get(Measure.COUNT).longValue())
            .containsExactly(tuple("0-1", 1L), tuple("2-10", 2L));
    }

    @Test
    void should_include_the_upper_bound_of_a_decimal_range() {
        var ranges = List.of(new NumberRange(0, 0.4));

        var result = cut.searchHTTPFacets(
            QUERY_CONTEXT,
            new FacetsQuery(TIME_RANGE, List.of(BY_API), List.of(REQUESTS), List.of(SCORE), ranges)
        );

        assertThat(result.metrics().getFirst().buckets())
            .extracting(FacetBucketResult::key, bucket -> bucket.measures().get(Measure.COUNT).longValue())
            .containsExactly(tuple("0-0.4", 2L));
    }

    @Test
    void should_return_no_bucket_for_a_field_no_document_carries() {
        var result = cut.searchHTTPFacets(
            QUERY_CONTEXT,
            new FacetsQuery(TIME_RANGE, List.of(BY_API), List.of(REQUESTS), List.of(NEVER_WRITTEN))
        );

        assertThat(result.metrics().getFirst().buckets()).isEmpty();
    }

    @Test
    void should_break_a_time_series_down_by_a_keyword() {
        List<FacetName> facets = List.of(TIER);

        var result = cut.searchHTTPTimeSeries(
            QUERY_CONTEXT,
            new TimeSeriesQuery(TIME_RANGE, List.of(BY_API), Duration.ofDays(1).toMillis(), List.of(REQUESTS), facets)
        );

        var keys = result
            .metrics()
            .getFirst()
            .buckets()
            .stream()
            .flatMap(bucket -> bucket.buckets() == null ? Stream.empty() : bucket.buckets().stream())
            .map(FacetBucketResult::key)
            .distinct()
            .toList();
        assertThat(keys).containsExactlyInAnyOrder("high", "low");
    }

    private long count(List<Filter> filters) {
        var result = cut.searchHTTPMeasures(QUERY_CONTEXT, new MeasuresQuery(TIME_RANGE, filters, List.of(REQUESTS)));
        return result.measures().getFirst().measures().get(Measure.COUNT).longValue();
    }

    private static void document(StringBuilder bulk, String index, String id, int minute, JsonObject additionalMetrics) {
        bulk.append(JsonObject.of("index", JsonObject.of("_index", index, "_id", id)).encode()).append('\n');
        bulk
            .append(
                JsonObject.of(
                    "@timestamp",
                    DAY.plus(Duration.ofHours(8).plusMinutes(minute)).toString(),
                    "entrypoint-id",
                    "http-proxy",
                    "request-id",
                    id,
                    "api-id",
                    API_ID,
                    "status",
                    200,
                    "gateway-response-time-ms",
                    10,
                    "additional-metrics",
                    additionalMetrics
                ).encode()
            )
            .append('\n');
    }
}
