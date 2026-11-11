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
import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.repository.analytics.engine.api.metric.AdditionalMetric;
import io.gravitee.repository.analytics.engine.api.metric.Measure;
import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.metric.MetricName;
import io.gravitee.repository.analytics.engine.api.query.AdditionalMetricField;
import io.gravitee.repository.analytics.engine.api.query.AdditionalMetricField.ValueType;
import io.gravitee.repository.analytics.engine.api.query.Facet;
import io.gravitee.repository.analytics.engine.api.query.FacetName;
import io.gravitee.repository.analytics.engine.api.query.FacetsQuery;
import io.gravitee.repository.analytics.engine.api.query.Filter;
import io.gravitee.repository.analytics.engine.api.query.MeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.NumberRange;
import io.gravitee.repository.analytics.engine.api.query.TimeSeriesQuery;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter.api.FieldResolver;
import io.vertx.core.json.JsonObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * An additional metric field on its way to Elasticsearch: the HTTP resolver reads it from the {@code additional-metrics}
 * map, every other resolver refuses it, and the HTTP queries filter and group on it by its value type, and measure it
 * when it holds a number.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AdditionalMetricFieldAdapterTest extends AbstractQueryAdapterTest {

    private static final AdditionalMetricField TIER = new AdditionalMetricField(ValueType.KEYWORD, "keyword_fraud-check_tier");
    private static final AdditionalMetricField FAILOVERS = new AdditionalMetricField(ValueType.LONG, "long_failover_count");
    private static final AdditionalMetricField SCORE = new AdditionalMetricField(ValueType.DOUBLE, "double_fraud-check_score");
    private static final AdditionalMetricField CACHE_HIT = new AdditionalMetricField(ValueType.BOOL, "bool_cache_hit");
    private static final AdditionalMetric FAILOVER_METRIC = new AdditionalMetric(FAILOVERS);

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
            assertThatThrownBy(() -> resolver.fromMetric((MetricName) FAILOVER_METRIC))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("additional-metrics.long_failover_count");
        }

        @Test
        void should_measure_a_number_field_under_the_additional_metrics_map_on_the_http_documents() {
            assertThat(new HTTPFieldResolver().fromMetric((MetricName) FAILOVER_METRIC)).isEqualTo(
                "additional-metrics.long_failover_count"
            );
        }

        static Stream<Function<MeasuresQuery, String>> measuresOfOtherDocuments() {
            return Stream.of(
                new AuthzMeasuresQueryAdapter()::adapt,
                new AuthzTrafficMeasuresQueryAdapter()::adapt,
                new EventMetricsMeasuresQueryAdapter()::adapt,
                query -> new MessageMeasuresQueryAdapter().adapt(query, Set.of("req-1"))
            );
        }

        @ParameterizedTest
        @MethodSource("measuresOfOtherDocuments")
        void should_refuse_to_measure_a_number_field_on_any_other_search_path(Function<MeasuresQuery, String> adapt) {
            var query = new MeasuresQuery(
                buildTimeRange(),
                List.of(),
                List.of(new MetricMeasuresQuery(FAILOVER_METRIC, Set.of(Measure.AVG)))
            );

            assertThatThrownBy(() -> adapt.apply(query))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("additional-metrics.long_failover_count");
        }

        @ParameterizedTest
        @MethodSource("notNumbers")
        void should_refuse_to_measure_a_field_that_holds_no_number(AdditionalMetricField field) {
            assertThatThrownBy(() -> new AdditionalMetric(field))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(field.name());
        }

        static Stream<AdditionalMetricField> notNumbers() {
            return Stream.of(
                TIER,
                CACHE_HIT,
                new AdditionalMetricField(ValueType.STRING, "string_note"),
                new AdditionalMetricField(ValueType.JSON, "json_payload")
            );
        }
    }

    @Nested
    class Measures {

        private static final Set<Measure> MEASURED = Set.of(
            Measure.AVG,
            Measure.MIN,
            Measure.MAX,
            Measure.SUM,
            Measure.COUNT,
            Measure.P50,
            Measure.P90,
            Measure.P95,
            Measure.P99
        );

        private final HTTPMeasuresQueryAdapter adapter = new HTTPMeasuresQueryAdapter();

        @Test
        void should_aggregate_only_the_requests_carrying_the_field() throws JsonProcessingException {
            var aggs = JSON.readTree(adapter.adapt(measures(new MetricMeasuresQuery(FAILOVER_METRIC, MEASURED)))).at("/aggs");

            var field = "additional-metrics.long_failover_count";
            var measured = aggs.at("/additional-metrics_long_failover_count#__MEASURED__/aggs");
            var counted = aggs.at("/additional-metrics_long_failover_count#__COUNTED__/aggs");
            assertThat(measured.at("/" + field + "#AVG")).isEqualTo(JSON.readTree("{\"avg\":{\"field\":\"" + field + "\"}}"));
            assertThat(measured.at("/" + field + "#MIN")).isEqualTo(JSON.readTree("{\"min\":{\"field\":\"" + field + "\"}}"));
            assertThat(measured.at("/" + field + "#MAX")).isEqualTo(JSON.readTree("{\"max\":{\"field\":\"" + field + "\"}}"));
            assertThat(measured.at("/" + field + "#SUM")).isEqualTo(JSON.readTree("{\"sum\":{\"field\":\"" + field + "\"}}"));
            assertThat(counted.at("/" + field + "#COUNT")).isEqualTo(JSON.readTree("{\"value_count\":{\"field\":\"" + field + "\"}}"));
            assertThat(measured.at("/" + field + "#P90")).isEqualTo(
                JSON.readTree("{\"percentiles\":{\"field\":\"" + field + "\",\"percents\":[90.0]}}")
            );
            assertThat(aggs.findValues("missing")).isEmpty();
        }

        @Test
        void should_keep_counting_a_document_without_the_field_as_zero_for_a_built_in() throws JsonProcessingException {
            var builtIn = new MetricMeasuresQuery(Metric.LLM_PROMPT_TOKEN_SENT, Set.of(Measure.AVG, Measure.P95, Measure.SUM));

            var aggs = JSON.readTree(adapter.adapt(measures(builtIn))).at("/aggs");

            assertThat(aggs.at("/LLM_PROMPT_TOKEN_SENT#__MEASURED__/aggs/LLM_PROMPT_TOKEN_SENT#AVG/avg/missing").asInt(-1)).isZero();
            assertThat(
                aggs.at("/LLM_PROMPT_TOKEN_SENT#__MEASURED__/aggs/LLM_PROMPT_TOKEN_SENT#P95/percentiles/missing").asInt(-1)
            ).isZero();
            assertThat(aggs.at("/LLM_PROMPT_TOKEN_SENT#__MEASURED__/aggs/LLM_PROMPT_TOKEN_SENT#SUM/sum/missing").asInt(-1)).isZero();
        }

        @Test
        void should_not_aggregate_a_percentage_of_a_field() throws JsonProcessingException {
            var aggs = JSON.readTree(adapter.adapt(measures(new MetricMeasuresQuery(FAILOVER_METRIC, Set.of(Measure.PERCENTAGE))))).at(
                "/aggs"
            );

            assertThat(aggs.isEmpty()).isTrue();
        }

        @Test
        void should_read_no_value_where_elasticsearch_answers_none_or_nan() throws JsonProcessingException {
            var query = measures(new MetricMeasuresQuery(FAILOVER_METRIC, Set.of(Measure.AVG, Measure.MAX, Measure.P95, Measure.SUM)));
            var aggregations = Map.of(
                "additional-metrics.long_failover_count#AVG",
                JSON.readValue("{\"value\":null}", Aggregation.class),
                "additional-metrics.long_failover_count#MAX",
                JSON.readValue("{\"value\":\"NaN\"}", Aggregation.class),
                "additional-metrics.long_failover_count#P95",
                JSON.readValue("{\"values\":{\"95.0\":null}}", Aggregation.class),
                "additional-metrics.long_failover_count#SUM",
                JSON.readValue("{\"value\":0.0}", Aggregation.class)
            );

            var measures = AggregationAdapter.toMetricsAndMeasures(aggregations, query).get(FAILOVER_METRIC);

            assertThat(measures)
                .containsEntry(Measure.AVG, null)
                .containsEntry(Measure.MAX, null)
                .containsEntry(Measure.P95, null)
                .containsEntry(Measure.SUM, 0.0);
        }

        @Test
        void should_keep_reading_zero_for_a_built_in_elasticsearch_answers_none_for() throws JsonProcessingException {
            var query = measures(new MetricMeasuresQuery(Metric.HTTP_GATEWAY_LATENCY, Set.of(Measure.AVG)));
            var aggregations = Map.of("HTTP_GATEWAY_LATENCY#AVG", JSON.readValue("{\"value\":null}", Aggregation.class));

            var measures = AggregationAdapter.toMetricsAndMeasures(aggregations, query).get(Metric.HTTP_GATEWAY_LATENCY);

            assertThat(measures).containsEntry(Measure.AVG, 0.0);
        }

        @Test
        void should_read_the_measures_of_an_unanswered_query_as_elasticsearch_would_over_no_document() {
            var query = measures(
                new MetricMeasuresQuery(FAILOVER_METRIC, MEASURED),
                new MetricMeasuresQuery(Metric.HTTP_GATEWAY_LATENCY, Set.of(Measure.AVG))
            );

            var result = new MeasuresResponseAdapter().empty(query).measures();

            var expected = new HashMap<Measure, Number>();
            MEASURED.forEach(measure -> expected.put(measure, null));
            expected.put(Measure.SUM, 0);
            expected.put(Measure.COUNT, 0);
            assertThat(result)
                .extracting(r -> r.metric(), r -> r.measures())
                .containsExactly(tuple(FAILOVER_METRIC, expected), tuple(Metric.HTTP_GATEWAY_LATENCY, Map.of(Measure.AVG, 0)));
        }

        @Test
        void should_read_the_measures_of_a_bucket_without_their_aggregation_as_elasticsearch_would_over_no_document() {
            var query = new FacetsQuery(
                buildTimeRange(),
                List.of(),
                List.of(new MetricMeasuresQuery(FAILOVER_METRIC, Set.of(Measure.AVG, Measure.COUNT))),
                List.of(TIER)
            );
            var empty = JSON.createObjectNode().put("key", "low").put("doc_count", 0);
            var facetAgg = new Aggregation();
            facetAgg.setBuckets(List.of(empty));

            var result = AggregationAdapter.toMetricsAndBuckets(
                Map.of("additional-metrics.long_failover_count#additional-metrics.keyword_fraud-check_tier", facetAgg),
                query
            );

            assertThat(result.getFirst().metric()).isEqualTo(FAILOVER_METRIC);
            assertThat(result.getFirst().buckets().getFirst().measures()).containsEntry(Measure.AVG, null).containsEntry(Measure.COUNT, 0);
        }

        @Test
        void should_order_facet_buckets_by_a_percentile_through_its_percent() throws JsonProcessingException {
            var builtIn = sorted(Metric.HTTP_GATEWAY_LATENCY, Measure.P90);
            var additional = sorted(FAILOVER_METRIC, Measure.P90);

            assertThat(order(builtIn)).isEqualTo(
                JSON.readTree("{\"HTTP_GATEWAY_LATENCY#__MEASURED__>HTTP_GATEWAY_LATENCY#P90[90.0]\":\"desc\"}")
            );
            assertThat(order(additional)).isEqualTo(
                JSON.readTree(
                    "{\"additional-metrics_long_failover_count#__MEASURED__>additional-metrics.long_failover_count#P90[90.0]\":\"desc\"}"
                )
            );
        }

        @Test
        void should_order_facet_buckets_by_the_value_of_a_field_whose_name_holds_dots() throws JsonProcessingException {
            assertThat(order(sorted(FAILOVER_METRIC, Measure.AVG))).isEqualTo(
                JSON.readTree(
                    "{\"additional-metrics_long_failover_count#__MEASURED__>additional-metrics.long_failover_count#AVG[value]\":\"desc\"}"
                )
            );
            assertThat(order(sorted(Metric.HTTP_GATEWAY_LATENCY, Measure.AVG))).isEqualTo(
                JSON.readTree("{\"HTTP_GATEWAY_LATENCY#__MEASURED__>HTTP_GATEWAY_LATENCY#AVG\":\"desc\"}")
            );
        }

        @Test
        void should_group_a_number_field_over_the_documents_carrying_it_only() throws JsonProcessingException {
            var query = new FacetsQuery(
                buildTimeRange(),
                List.of(),
                List.of(
                    new MetricMeasuresQuery(FAILOVER_METRIC, Set.of(Measure.MIN)),
                    new MetricMeasuresQuery(Metric.HTTP_REQUESTS, Set.of(Measure.COUNT))
                ),
                List.of(TIER, Facet.API)
            );

            var aggs = JSON.readTree(new HTTPFacetsQueryAdapter().adapt(query)).at("/aggs");

            var scope = aggs.at("/additional-metrics.long_failover_count#__FILTER__");
            assertThat(scope.at("/filter")).isEqualTo(JSON.readTree("{\"exists\":{\"field\":\"additional-metrics.long_failover_count\"}}"));
            assertThat(
                scope.at("/aggs/additional-metrics.long_failover_count#additional-metrics.keyword_fraud-check_tier/terms/field").asText()
            ).isEqualTo("additional-metrics.keyword_fraud-check_tier");
            assertThat(aggs.has("HTTP_REQUESTS#__FILTER__")).isFalse();
            assertThat(aggs.has("HTTP_REQUESTS#additional-metrics.keyword_fraud-check_tier")).isTrue();
        }

        @Test
        void should_name_the_time_series_of_a_field_after_its_wire_name() throws JsonProcessingException {
            var query = new TimeSeriesQuery(
                buildTimeRange(),
                List.of(),
                60_000L,
                List.of(new MetricMeasuresQuery(FAILOVER_METRIC, Set.of(Measure.AVG)))
            );

            var aggs = JSON.readTree(new HTTPTimeSeriesQueryAdapter().adapt(query)).at("/aggs");

            assertThat(
                aggs
                    .at(
                        "/additional-metrics.long_failover_count#TIME_SERIES/aggs/additional-metrics_long_failover_count#__MEASURED__/aggs/additional-metrics.long_failover_count#AVG/avg/field"
                    )
                    .asText()
            ).isEqualTo("additional-metrics.long_failover_count");
        }

        private MeasuresQuery measures(MetricMeasuresQuery... metrics) {
            return new MeasuresQuery(buildTimeRange(), List.of(), List.of(metrics));
        }

        private static MetricMeasuresQuery sorted(MetricName metric, Measure measure) {
            return new MetricMeasuresQuery(
                metric,
                Set.of(measure),
                List.of(new MetricMeasuresQuery.Sort(measure, MetricMeasuresQuery.Sort.Order.DESC))
            );
        }

        private JsonNode order(MetricMeasuresQuery metric) throws JsonProcessingException {
            var query = new FacetsQuery(buildTimeRange(), List.of(), List.of(metric), List.of(TIER));
            var aggs = JSON.readTree(new HTTPFacetsQueryAdapter().adapt(query)).at("/aggs");
            var facets = metric.metric() instanceof AdditionalMetric
                ? aggs.at("/" + AuthzMeasuresQueryAdapter.scopeAggName(metric.metric()) + "/aggs")
                : aggs;
            return facets.at("/" + AggregationAdapter.adaptName(metric.metric(), TIER).replace("/", "~1") + "/terms/order");
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
