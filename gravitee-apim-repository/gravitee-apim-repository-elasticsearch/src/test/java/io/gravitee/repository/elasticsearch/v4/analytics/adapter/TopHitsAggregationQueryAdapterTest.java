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
package io.gravitee.repository.elasticsearch.v4.analytics.adapter;

import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.BUCKETS_PATH;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.BUCKET_SCRIPT;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.COMPOSITE;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.DATE_HISTOGRAM;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.DERIVATIVE;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.FIXED_INTERVAL;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.GAP_POLICY;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.INSERT_ZEROS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.MAX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.METRICS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.SCRIPT;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.SORT;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.SOURCES;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.TOP_METRICS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.AGGS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.BOOL;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.FILTER;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.QUERY;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.SIZE;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.TIMESTAMP;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.TRACK_TOTAL_HITS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.BEFORE_START;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.BY_DIMENSIONS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.DERIVATIVE_PREFIX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.END_BUCKET_PREFIX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.END_IN_RANGE;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.LATEST_BUCKET_PREFIX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.MAX_PREFIX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.NON_NEGATIVE_PREFIX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.PER_INTERVAL;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.START_BUCKET_PREFIX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Query.EXISTS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Query.GTE;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Query.LT;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Query.LTE;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Query.RANGE;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Query.TERM;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Tokens.FIELD;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Tokens.TERMS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.repository.log.v4.model.analytics.Aggregation;
import io.gravitee.repository.log.v4.model.analytics.AggregationType;
import io.gravitee.repository.log.v4.model.analytics.HistogramQuery;
import io.gravitee.repository.log.v4.model.analytics.SearchTermId;
import io.gravitee.repository.log.v4.model.analytics.Term;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class TopHitsAggregationQueryAdapterTest {

    private static final String API_ID = "273f4728-1e30-4c78-bf47-281e304c78a5";
    private static final Long FROM = 1756104349879L;
    private static final Long TO = 1756190749879L;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void should_throw_when_no_aggregate_functions_specified() {
        assertThrows(IllegalArgumentException.class, () ->
            TopHitsAggregationQueryAdapter.adapt(getHistogramQuery(API_ID, Instant.now(), Instant.now(), List.of(), null))
        );
    }

    @Test
    void should_throw_when_more_than_one_aggregate_functions_specified() {
        Aggregation agg1 = new Aggregation("downstream-active-connections", AggregationType.VALUE);
        Aggregation agg2 = new Aggregation("upstream-active-connections", AggregationType.DELTA);
        HistogramQuery query = getHistogramQuery(API_ID, Instant.now(), Instant.now(), List.of(agg1, agg2), null);

        assertThrows(IllegalArgumentException.class, () -> TopHitsAggregationQueryAdapter.adapt(query));
    }

    @Test
    void should_build_value_query_with_composite_dimensions_and_terms_filters() throws Exception {
        // Given
        Instant to = Instant.now();
        Instant from = to.minus(Duration.ofHours(14));
        List<Aggregation> aggregations = List.of(
            new Aggregation("upstream-active-connections", AggregationType.VALUE),
            new Aggregation("downstream-active-connections", AggregationType.VALUE)
        );
        // include single-value optional terms (now expect "terms" even for single value)
        HistogramQuery query = getHistogramQuery(API_ID, from, to, aggregations, null);

        // When
        String json = TopHitsAggregationQueryAdapter.adapt(query);

        // Then
        JsonNode node = MAPPER.readTree(json);
        // Root settings
        assertEquals(0, node.get(SIZE).asInt());
        assertFalse(node.get(TRACK_TOTAL_HITS).asBoolean());

        // Query filters (order-agnostic assertions)
        JsonNode filters = node.at("/query/bool/filter");
        assertTrue(filters.isArray());

        assertTrue(hasTermFilter(filters, "api-id", API_ID));
        assertTrue(hasTimestampRange(filters, true, true)); // VALUE includes [from,to]
        assertTrue(hasExistsForAll(filters, Set.of("upstream-active-connections", "downstream-active-connections")));

        // Optional filters now use "terms" even for single values
        assertTrue(hasTermsFilter(filters, "app-id", Set.of("1")));
        assertTrue(hasTermsFilter(filters, "plan-id", Set.of("6d57d1f2-93e7-4240-97d1-f293e7c24052")));

        // Aggregations: composite by_dimensions with 4 default keys
        JsonNode byDimensions = node.at("/aggs/by_dimensions");
        assertFalse(byDimensions.isMissingNode());
        JsonNode composite = byDimensions.get(COMPOSITE);
        assertNotNull(composite);
        assertEquals(1000, composite.get(SIZE).asInt());

        Set<String> expectedFields = Set.of("gw-id", "api-id", "org-id", "env-id");
        assertEquals(expectedFields, compositeSourceFields(composite.get(SOURCES)));

        // Sub-aggregations: top_metrics per metric, named "latest_" + field
        JsonNode subAggs = byDimensions.get(AGGS);
        assertNotNull(subAggs);

        assertLatestTopMetrics(subAggs, "upstream-active-connections");
        assertLatestTopMetrics(subAggs, "downstream-active-connections");
    }

    @Test
    void should_build_delta_query_with_start_end_windows_and_composite_dimensions() throws JsonProcessingException {
        // Given
        String apiId = API_ID;
        Instant from = Instant.ofEpochMilli(FROM);
        Instant to = Instant.ofEpochMilli(TO);
        List<Aggregation> aggregations = List.of(
            new Aggregation("downstream-publish-messages-total", AggregationType.DELTA),
            new Aggregation("upstream-publish-messages-total", AggregationType.DELTA)
        );
        HistogramQuery query = getHistogramQuery(apiId, from, to, aggregations, null);

        // When
        String json = TopHitsAggregationQueryAdapter.adapt(query);

        // Then
        JsonNode node = MAPPER.readTree(json);

        // Root settings
        assertEquals(0, node.get(SIZE).asInt());
        assertFalse(node.get(TRACK_TOTAL_HITS).asBoolean());

        // Query filters (order-agnostic assertions)
        JsonNode filters = node.at("/query/bool/filter");
        assertTrue(filters.isArray());

        // api-id term filter is present, no root range for DELTA, exists for both fields
        assertTrue(hasTermFilter(filters, "api-id", apiId));
        assertFalse(hasAnyTimestampRange(filters));
        assertTrue(hasExistsForAll(filters, Set.of("downstream-publish-messages-total", "upstream-publish-messages-total")));

        // Aggregations: composite with 7 keys (default + app-id, plan-id, topic)
        JsonNode byDimensions = node.at("/aggs/by_dimensions");
        assertFalse(byDimensions.isMissingNode());

        JsonNode composite = byDimensions.get(COMPOSITE);
        assertNotNull(composite);
        assertEquals(1000, composite.get(SIZE).asInt());

        Set<String> expectedFields = Set.of("gw-id", "org-id", "env-id", "api-id", "app-id", "plan-id", "topic");
        assertEquals(expectedFields, compositeSourceFields(composite.get(SOURCES)));

        // Sub-aggregations: before_start_time, end_in_range with top_metrics per field
        JsonNode subAggs = byDimensions.get(AGGS);
        assertNotNull(subAggs);

        // before_start_time: lt FROM
        JsonNode beforeStart = subAggs.get(BEFORE_START);
        assertNotNull(beforeStart);
        JsonNode beforeStartRange = beforeStart.get(FILTER).get(RANGE).get(TIMESTAMP);
        assertNotNull(beforeStartRange);
        assertEquals(FROM, beforeStartRange.get(LT).asLong());
        JsonNode beforeStartAggs = beforeStart.get(AGGS);
        assertStartTopMetrics(beforeStartAggs, "downstream-publish-messages-total");
        assertStartTopMetrics(beforeStartAggs, "upstream-publish-messages-total");

        // end_in_range: gte FROM, lt TO
        JsonNode endInRange = subAggs.get(END_IN_RANGE);
        assertNotNull(endInRange);
        JsonNode endInRangeRange = endInRange.get(FILTER).get(RANGE).get(TIMESTAMP);
        assertNotNull(endInRangeRange);
        assertEquals(FROM, endInRangeRange.get(GTE).asLong());
        assertEquals(TO, endInRangeRange.get(LT).asLong());
        JsonNode endInRangeAggs = endInRange.get(AGGS);
        assertEndTopMetrics(endInRangeAggs, "downstream-publish-messages-total");
        assertEndTopMetrics(endInRangeAggs, "upstream-publish-messages-total");
    }

    @Test
    void should_build_trend_query_with_fixed_interval_and_composite_dimensions() throws JsonProcessingException {
        // Given
        Aggregation agg1 = new Aggregation("downstream-publish-messages-total", AggregationType.TREND);
        Aggregation agg2 = new Aggregation("upstream-publish-messages-total", AggregationType.TREND);
        HistogramQuery query = getHistogramQuery(
            API_ID,
            Instant.ofEpochMilli(FROM),
            Instant.ofEpochMilli(TO),
            List.of(agg1, agg2),
            Duration.ofMillis(60_000)
        );

        // When
        String json = TopHitsAggregationQueryAdapter.adapt(query);

        // Then
        JsonNode node = MAPPER.readTree(json);
        // Root settings
        assertEquals(0, node.get(SIZE).asInt());
        assertFalse(node.get(TRACK_TOTAL_HITS).asBoolean());

        // Query filters: api-id term + time range + exists filters for both fields
        JsonNode filters = node.get(QUERY).get(BOOL).get(FILTER);
        assertTrue(filters.isArray());
        assertTrue(hasTermFilter(filters, "api-id", API_ID));
        assertTrue(hasTimestampRange(filters, true, true)); // TREND includes [from,to]
        assertTrue(hasExistsForAll(filters, Set.of("downstream-publish-messages-total", "upstream-publish-messages-total")));

        // Aggregations: composite with 7 keys
        JsonNode byDimensions = node.get(AGGS).get(BY_DIMENSIONS);
        assertNotNull(byDimensions);
        JsonNode composite = byDimensions.get(COMPOSITE);
        assertNotNull(composite);
        assertEquals(1000, composite.get(SIZE).asInt());

        Set<String> expectedFields = Set.of("gw-id", "app-id", "plan-id", "org-id", "env-id", "api-id", "topic");
        assertEquals(expectedFields, compositeSourceFields(composite.get(SOURCES)));

        // Per interval aggregation
        JsonNode perInterval = byDimensions.get(AGGS).get(PER_INTERVAL);
        assertNotNull(perInterval);
        JsonNode histogram = perInterval.get(DATE_HISTOGRAM);
        assertNotNull(histogram);
        assertEquals(TIMESTAMP, histogram.get(FIELD).asText());
        assertEquals("60000ms", histogram.get(FIXED_INTERVAL).asText());

        JsonNode perIntervalAggs = perInterval.get(AGGS);
        assertNotNull(perIntervalAggs);
        assertTrendSeries(perIntervalAggs, "downstream-publish-messages-total");
        assertTrendSeries(perIntervalAggs, "upstream-publish-messages-total");
    }

    // ----------------- Helpers -----------------

    private static boolean hasTermFilter(JsonNode filters, String field, String value) {
        for (JsonNode f : filters) {
            if (f.has(TERM) && f.get(TERM).has(field) && value.equals(f.get(TERM).get(field).asText())) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasAnyTimestampRange(JsonNode filters) {
        for (JsonNode f : filters) {
            if (f.has(RANGE) && f.get(RANGE).has(TIMESTAMP)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasTimestampRange(JsonNode filters, boolean expectGte, boolean expectLte) {
        for (JsonNode f : filters) {
            if (f.has(RANGE) && f.get(RANGE).has(TIMESTAMP)) {
                JsonNode ts = f.get(RANGE).get(TIMESTAMP);
                boolean ok = (!expectGte || ts.hasNonNull(GTE)) && (!expectLte || ts.hasNonNull(LTE));
                if (ok) return true;
            }
        }
        return false;
    }

    private static boolean hasExistsForAll(JsonNode filters, Set<String> expectedFields) {
        // Adjusted for OR semantics block: check exists under a bool.should with minimum_should_match=1
        // First, collect root-level exists (legacy AND path)
        Set<String> found = new HashSet<>();
        for (JsonNode f : filters) {
            if (f.has(EXISTS) && f.get(EXISTS).has(FIELD)) {
                found.add(f.get(EXISTS).get(FIELD).asText());
            }
            // Also inspect bool.should containers
            if (f.has("bool") && f.get("bool").has("should")) {
                JsonNode shouldArr = f.get("bool").get("should");
                if (shouldArr.isArray()) {
                    for (JsonNode s : shouldArr) {
                        if (s.has(EXISTS) && s.get(EXISTS).has(FIELD)) {
                            found.add(s.get(EXISTS).get(FIELD).asText());
                        }
                    }
                }
            }
        }
        return found.containsAll(expectedFields);
    }

    private static Set<String> compositeSourceFields(JsonNode sources) {
        Set<String> fields = new HashSet<>();
        if (sources != null && sources.isArray()) {
            for (JsonNode src : sources) {
                String wrapperKey = src.fieldNames().next();
                fields.add(src.get(wrapperKey).get(TERMS).get(FIELD).asText());
            }
        }
        return fields;
    }

    private static void assertTrendSeries(JsonNode perIntervalAggs, String field) {
        String maxName = MAX_PREFIX + field;
        String derivativeName = DERIVATIVE_PREFIX + field;
        String nonNegativeName = NON_NEGATIVE_PREFIX + field;
        // max_<field>
        JsonNode maxNode = perIntervalAggs.get(maxName);
        assertNotNull(maxNode, "Missing " + maxName);
        assertEquals(field, maxNode.get(MAX).get(FIELD).asText());
        // <field>_derivative
        JsonNode derivativeNode = perIntervalAggs.get(derivativeName);
        assertNotNull(derivativeNode, "Missing " + derivativeName);
        assertTrue(derivativeNode.has(DERIVATIVE));
        assertEquals(maxName, derivativeNode.get(DERIVATIVE).get(BUCKETS_PATH).asText());
        assertEquals(INSERT_ZEROS, derivativeNode.get(DERIVATIVE).get(GAP_POLICY).asText());
        // <field>_non_negative
        JsonNode nnNode = perIntervalAggs.get(nonNegativeName);
        assertNotNull(nnNode, "Missing " + nonNegativeName);
        assertTrue(nnNode.has(BUCKET_SCRIPT));
        JsonNode bucketScript = nnNode.get(BUCKET_SCRIPT);
        assertTrue(bucketScript.has(BUCKETS_PATH));
        assertEquals(derivativeName, bucketScript.get(BUCKETS_PATH).get("d").asText());
        assertTrue(bucketScript.has(SCRIPT));
        assertEquals("params.d != null ? Math.max(params.d, 0) : 0", bucketScript.get(SCRIPT).asText());
    }

    @Test
    void should_apply_optional_terms_filters_with_or_semantics_for_delta() throws Exception {
        // Given
        Instant from = Instant.ofEpochMilli(FROM);
        Instant to = Instant.ofEpochMilli(TO);
        List<Aggregation> aggregations = List.of(
            new Aggregation("downstream-publish-messages-total", AggregationType.DELTA),
            new Aggregation("upstream-publish-messages-total", AggregationType.DELTA)
        );
        // Two values for same key -> expect a single "terms" filter (OR semantics)
        HistogramQuery query = new HistogramQuery(
            new SearchTermId(SearchTermId.SearchTerm.API, API_ID),
            new io.gravitee.repository.log.v4.model.analytics.TimeRange(from, to, Optional.empty()),
            aggregations,
            Optional.empty(),
            List.of(new Term("app-id", "1"), new Term("app-id", "2"), new Term("plan-id", "p1"))
        );

        // When
        String json = TopHitsAggregationQueryAdapter.adapt(query);

        // Then
        JsonNode node = MAPPER.readTree(json);
        JsonNode filters = node.at("/query/bool/filter");
        assertTrue(filters.isArray());

        // DELTA should not have a root time range
        assertFalse(hasAnyTimestampRange(filters));
        // Exists filters should be present for both fields
        assertTrue(hasExistsForAll(filters, Set.of("downstream-publish-messages-total", "upstream-publish-messages-total")));

        // Optional filters: grouped "app-id" terms and single-value "plan-id" also as "terms"
        assertTrue(hasTermsFilter(filters, "app-id", Set.of("1", "2")));
        assertTrue(hasTermsFilter(filters, "plan-id", Set.of("p1")));
    }

    @Test
    void should_apply_optional_terms_filters_with_or_semantics_for_trend() throws Exception {
        // Given
        Instant from = Instant.ofEpochMilli(FROM);
        Instant to = Instant.ofEpochMilli(TO);
        List<Aggregation> aggregations = List.of(
            new Aggregation("downstream-publish-messages-total", AggregationType.TREND),
            new Aggregation("upstream-publish-messages-total", AggregationType.TREND)
        );
        // Two values for same key -> expect a single "terms" filter (OR semantics)
        HistogramQuery query = new HistogramQuery(
            new SearchTermId(SearchTermId.SearchTerm.API, API_ID),
            new io.gravitee.repository.log.v4.model.analytics.TimeRange(from, to, Optional.of(Duration.ofMinutes(1))),
            aggregations,
            Optional.empty(),
            List.of(new Term("app-id", "1"), new Term("app-id", "2"), new Term("plan-id", "p1"))
        );

        // When
        String json = TopHitsAggregationQueryAdapter.adapt(query);

        // Then
        JsonNode node = MAPPER.readTree(json);
        JsonNode filters = node.at("/query/bool/filter");
        assertTrue(filters.isArray());

        // TREND should include a root time range [gte, lte]
        assertTrue(hasTimestampRange(filters, true, true));
        // Exists filters should be present for both fields
        assertTrue(hasExistsForAll(filters, Set.of("downstream-publish-messages-total", "upstream-publish-messages-total")));

        // Optional filters: grouped "app-id" terms and single-value "plan-id" also as "terms"
        assertTrue(hasTermsFilter(filters, "app-id", Set.of("1", "2")));
        assertTrue(hasTermsFilter(filters, "plan-id", Set.of("p1")));
    }

    // Helper for "terms" filter existence, similar style to hasTermFilter
    private static boolean hasTermsFilter(JsonNode filters, String field, Set<String> expectedValues) {
        for (JsonNode f : filters) {
            if (f.has(TERMS) && f.get(TERMS).has(field)) {
                JsonNode arrOrVals = f.get(TERMS).get(field);
                Set<String> found = new java.util.HashSet<>();

                if (arrOrVals.isArray()) {
                    for (JsonNode v : arrOrVals) found.add(v.asText());
                } else {
                    // ES allows both array and non-array in some serializers; accept both
                    found.add(arrOrVals.asText());
                }

                return found.equals(expectedValues);
            }
        }

        return false;
    }

    private void assertStartTopMetrics(JsonNode parentAggs, String field) {
        String startName = START_BUCKET_PREFIX + field;
        JsonNode start = parentAggs.get(startName);
        JsonNode startTm = start.get(TOP_METRICS);
        assertEquals(field, startTm.get(METRICS).get(FIELD).asText());
        assertEquals(io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Sort.DESC, startTm.get(SORT).get(TIMESTAMP).asText());
        // Do not assert SIZE: adapter relies on ES default (1)
    }

    private void assertEndTopMetrics(JsonNode parentAggs, String field) {
        String endName = END_BUCKET_PREFIX + field;
        JsonNode end = parentAggs.get(endName);
        JsonNode endTm = end.get(TOP_METRICS);
        assertEquals(field, endTm.get(METRICS).get(FIELD).asText());
        assertEquals(io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Sort.DESC, endTm.get(SORT).get(TIMESTAMP).asText());
        // Do not assert SIZE: adapter relies on ES default (1)
    }

    private void assertLatestTopMetrics(JsonNode parentAggs, String field) {
        String latestName = LATEST_BUCKET_PREFIX + field;
        JsonNode latest = parentAggs.get(latestName);
        JsonNode latestTm = latest.get(TOP_METRICS);
        assertEquals(field, latestTm.get(METRICS).get(FIELD).asText());
        assertEquals(io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Sort.DESC, latestTm.get(SORT).get(TIMESTAMP).asText());
    }

    private static @NotNull HistogramQuery getHistogramQuery(
        String apiId,
        Instant from,
        Instant to,
        List<Aggregation> aggregations,
        Duration interval
    ) {
        var searchTermId = new SearchTermId(SearchTermId.SearchTerm.API, apiId);
        var timeRange = new io.gravitee.repository.log.v4.model.analytics.TimeRange(
            from,
            to,
            interval == null ? Optional.empty() : Optional.of(interval)
        );
        // optional filters (expressed as terms even when single value)
        var terms = List.of(new Term("app-id", "1"), new Term("plan-id", "6d57d1f2-93e7-4240-97d1-f293e7c24052"));

        return new HistogramQuery(searchTermId, timeRange, aggregations, Optional.empty(), terms);
    }
}
