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
import static org.assertj.core.groups.Tuple.tuple;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.gravitee.elasticsearch.model.Aggregation;
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
import io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter.api.FieldResolver;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * An additional metric field on its way to Elasticsearch: the HTTP resolver reads it from the {@code additional-metrics}
 * map, every other resolver refuses it, and the HTTP queries filter and group on it by its value type.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AdditionalMetricFieldAdapterTest extends AbstractQueryAdapterTest {

    private static final AdditionalMetricField TIER = new AdditionalMetricField(ValueType.KEYWORD, "keyword_fraud-check_tier");
    private static final AdditionalMetricField FAILOVERS = new AdditionalMetricField(ValueType.LONG, "long_failover_count");
    private static final AdditionalMetricField SCORE = new AdditionalMetricField(ValueType.DOUBLE, "double_fraud-check_score");
    private static final AdditionalMetricField CACHE_HIT = new AdditionalMetricField(ValueType.BOOL, "bool_cache_hit");

    @Nested
    class Resolution {

        @Test
        void should_read_the_field_under_the_additional_metrics_map_on_the_http_documents() {
            var resolver = new HTTPFieldResolver();
            var tool = new AdditionalMetricField(ValueType.KEYWORD, "keyword_mcp-proxy_tools/call");
            var dotted = new AdditionalMetricField(ValueType.LONG, "long_a.b");

            assertThat(resolver.fromFilter(new Filter(tool, Filter.Operator.EQ, "search"))).isEqualTo(
                "additional-metrics.keyword_mcp-proxy_tools/call"
            );
            assertThat(resolver.fromFacet((FacetName) tool)).isEqualTo("additional-metrics.keyword_mcp-proxy_tools/call");
            assertThat(resolver.fromFacet((FacetName) dotted)).isEqualTo("additional-metrics.long_a.b");
        }

        @Test
        void should_resolve_a_built_in_and_the_additional_metric_it_reads_to_the_same_field() {
            var resolver = new HTTPFieldResolver();
            var tool = new AdditionalMetricField(ValueType.KEYWORD, "keyword_mcp-proxy_tools/call");

            assertThat(resolver.fromFacet((FacetName) tool)).isEqualTo(resolver.fromFacet(Facet.MCP_PROXY_TOOL));
        }

        static Stream<FieldResolver> resolversOfOtherDocuments() {
            return Stream.of(
                new MessageFieldResolver(),
                new NativeApiFieldResolver(),
                new EventMetricsFieldResolver(),
                new AuthzFieldResolver(),
                new AuthzTrafficFieldResolver()
            );
        }

        @ParameterizedTest
        @MethodSource("resolversOfOtherDocuments")
        void should_refuse_an_additional_metric_on_any_other_document(FieldResolver resolver) {
            assertThatThrownBy(() -> resolver.fromFilter(new Filter(TIER, Filter.Operator.EQ, "high")))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("additional-metrics.keyword_fraud-check_tier");
            assertThatThrownBy(() -> resolver.fromFacet((FacetName) TIER))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("additional-metrics.keyword_fraud-check_tier");
        }
    }

    @Nested
    class Filters {

        private final FilterAdapter adapter = new FilterAdapter(new HTTPFieldResolver());

        @Test
        void should_filter_a_keyword_with_a_term_or_terms() {
            var filters = adapter.adaptForHTTP(
                query(List.of(new Filter(TIER, Filter.Operator.EQ, "high"), new Filter(TIER, Filter.Operator.IN, List.of("high", "low"))))
            );

            assertThat(filters).contains(
                JsonObject.of("term", JsonObject.of("additional-metrics.keyword_fraud-check_tier", "high")),
                JsonObject.of("terms", JsonObject.of("additional-metrics.keyword_fraud-check_tier", List.of("high", "low")))
            );
        }

        @Test
        void should_filter_a_number_with_a_range() {
            var filters = adapter.adaptForHTTP(
                query(List.of(new Filter(FAILOVERS, Filter.Operator.GTE, "2"), new Filter(SCORE, Filter.Operator.LTE, "0.75")))
            );

            assertThat(filters).contains(
                JsonObject.of("range", JsonObject.of("additional-metrics.long_failover_count", JsonObject.of("gte", "2"))),
                JsonObject.of("range", JsonObject.of("additional-metrics.double_fraud-check_score", JsonObject.of("lte", "0.75")))
            );
        }

        @Test
        void should_filter_a_boolean_with_a_term() {
            var filters = adapter.adaptForHTTP(query(List.of(new Filter(CACHE_HIT, Filter.Operator.EQ, "true"))));

            assertThat(filters).contains(JsonObject.of("term", JsonObject.of("additional-metrics.bool_cache_hit", "true")));
        }

        private MeasuresQuery query(List<Filter> filters) {
            return new MeasuresQuery(
                buildTimeRange(),
                filters,
                List.of(new MetricMeasuresQuery(Metric.HTTP_REQUESTS, Set.of(Measure.COUNT)))
            );
        }
    }

    @Nested
    class Facets {

        private final HTTPFacetsQueryAdapter adapter = new HTTPFacetsQueryAdapter();

        @Test
        void should_group_by_a_keyword_at_any_level_with_terms() throws JsonProcessingException {
            List<FacetName> facets = List.of(TIER, Facet.API);

            var aggs = JSON.readTree(adapter.adapt(new FacetsQuery(buildTimeRange(), buildFilters(), buildMetrics(), facets))).at("/aggs");

            var byTier = aggs.at("/HTTP_GATEWAY_LATENCY#additional-metrics.keyword_fraud-check_tier");
            assertThat(byTier.at("/terms/field").asText()).isEqualTo("additional-metrics.keyword_fraud-check_tier");
            assertThat(byTier.at("/aggs/HTTP_GATEWAY_LATENCY#API/terms/field").asText()).isEqualTo("api-id");
        }

        @Test
        void should_keep_the_default_bucket_count_of_a_keyword_when_the_request_sets_no_limit() throws JsonProcessingException {
            List<FacetName> facets = List.of(TIER);

            var aggs = JSON.readTree(adapter.adapt(new FacetsQuery(buildTimeRange(), buildFilters(), buildMetrics(), facets))).at("/aggs");

            assertThat(aggs.at("/HTTP_GATEWAY_LATENCY#additional-metrics.keyword_fraud-check_tier/terms").has("size")).isFalse();
        }

        @Test
        void should_cap_the_buckets_of_a_keyword_at_the_requested_limit() throws JsonProcessingException {
            List<FacetName> facets = List.of(TIER);

            var aggs = JSON.readTree(adapter.adapt(new FacetsQuery(buildTimeRange(), buildFilters(), buildMetrics(), facets, 5))).at(
                "/aggs"
            );

            assertThat(aggs.at("/HTTP_GATEWAY_LATENCY#additional-metrics.keyword_fraud-check_tier/terms/size").asInt()).isEqualTo(5);
        }

        @Test
        void should_group_a_whole_number_by_ranges_including_their_upper_bound() throws JsonProcessingException {
            List<FacetName> facets = List.of(FAILOVERS);
            var ranges = List.of(new NumberRange(0, 1), new NumberRange(2, 10));

            var aggs = JSON.readTree(adapter.adapt(new FacetsQuery(buildTimeRange(), buildFilters(), buildMetrics(), facets, ranges))).at(
                "/aggs"
            );

            var range = aggs.at("/HTTP_GATEWAY_LATENCY#additional-metrics.long_failover_count/range");
            assertThat(range.at("/field").asText()).isEqualTo("additional-metrics.long_failover_count");
            assertThat(range.at("/ranges/1/from").asLong()).isEqualTo(2);
            assertThat(range.at("/ranges/1/to").asLong()).isEqualTo(11);
        }

        @Test
        void should_group_a_decimal_by_ranges_including_their_upper_bound() throws JsonProcessingException {
            List<FacetName> facets = List.of(SCORE);
            var ranges = List.of(new NumberRange(0, 0.5));

            var aggs = JSON.readTree(adapter.adapt(new FacetsQuery(buildTimeRange(), buildFilters(), buildMetrics(), facets, ranges))).at(
                "/aggs"
            );

            var to = aggs.at("/HTTP_GATEWAY_LATENCY#additional-metrics.double_fraud-check_score/range/ranges/0/to").asDouble();
            assertThat(to).isEqualTo(Math.nextUp(0.5));
        }

        @Test
        void should_group_a_boolean_with_terms() throws JsonProcessingException {
            List<FacetName> facets = List.of(CACHE_HIT);

            var aggs = JSON.readTree(adapter.adapt(new FacetsQuery(buildTimeRange(), buildFilters(), buildMetrics(), facets))).at("/aggs");

            assertThat(aggs.at("/HTTP_GATEWAY_LATENCY#additional-metrics.bool_cache_hit/terms/field").asText()).isEqualTo(
                "additional-metrics.bool_cache_hit"
            );
        }

        @Test
        void should_key_the_buckets_of_a_boolean_true_and_false() {
            var query = new FacetsQuery(
                buildTimeRange(),
                List.of(),
                List.of(new MetricMeasuresQuery(Metric.HTTP_REQUESTS, Set.of(Measure.COUNT))),
                List.of(CACHE_HIT)
            );
            var hit = JSON.createObjectNode().put("key", 1).put("key_as_string", "true").put("doc_count", 8);
            hit.putObject("HTTP_REQUESTS#COUNT").put("value", 8);
            var miss = JSON.createObjectNode().put("key", 0).put("key_as_string", "false").put("doc_count", 2);
            miss.putObject("HTTP_REQUESTS#COUNT").put("value", 2);
            var facetAgg = new Aggregation();
            facetAgg.setBuckets(List.of(hit, miss));

            var result = AggregationAdapter.toMetricsAndBuckets(Map.of("HTTP_REQUESTS#additional-metrics.bool_cache_hit", facetAgg), query);

            assertThat(result.getFirst().buckets())
                .extracting(bucket -> bucket.key(), bucket -> bucket.measures().get(Measure.COUNT).longValue())
                .containsExactly(tuple("true", 8L), tuple("false", 2L));
        }
    }
}
