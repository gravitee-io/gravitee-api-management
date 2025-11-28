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
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.MISSING_BUCKET;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.SCRIPT;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.SORT;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.SOURCES;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.TOP_METRICS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.AGGS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.BOOL;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.FILTER;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.QUERY;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.SHOULD;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.SIZE;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.TIMESTAMP;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.TRACK_TOTAL_HITS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.BEFORE_START;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.BY_DIMENSIONS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.DERIVATIVE_PREFIX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.END_IN_RANGE;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.LATEST_VALUE_PREFIX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.MAX_PREFIX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.NON_NEGATIVE_PREFIX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.PER_INTERVAL;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Query.EXISTS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Query.GTE;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Query.LT;
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
import io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl;
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

class EventMetricsQueryAdapterTest {

    private static final String API_ID = "273f4728-1e30-4c78-bf47-281e304c78a5";
    private static final Long FROM = 1756104349879L;
    private static final Long TO = 1756190749879L;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void should_throw_when_no_aggregate_functions_specified() {
        assertThrows(IllegalArgumentException.class, () ->
            EventMetricsQueryAdapter.toESQuery(buildHistogramQuery(API_ID, Instant.now(), Instant.now(), List.of(), null, null))
        );
    }

    @Test
    void should_throw_when_more_than_one_aggregate_functions_specified() {
        Aggregation agg1 = new Aggregation("downstream-active-connections", AggregationType.VALUE);
        Aggregation agg2 = new Aggregation("upstream-active-connections", AggregationType.DELTA);
        HistogramQuery query = buildHistogramQuery(API_ID, Instant.now(), Instant.now(), List.of(agg1, agg2), null, null);

        assertThrows(IllegalArgumentException.class, () -> EventMetricsQueryAdapter.toESQuery(query));
    }

    @Test
    void should_build_latest_value_query_with_composite_dimensions() throws Exception {
        Instant to = Instant.now();
        Instant from = to.minus(Duration.ofHours(14));
        List<Aggregation> aggregations = List.of(
            new Aggregation("upstream-active-connections", AggregationType.VALUE),
            new Aggregation("downstream-active-connections", AggregationType.VALUE)
        );
        HistogramQuery query = buildHistogramQuery(API_ID, from, to, aggregations, null, null);

        String json = EventMetricsQueryAdapter.toESQuery(query);

        JsonNode node = MAPPER.readTree(json);
        // Root settings
        assertEquals(0, node.get(SIZE).asInt());
        assertFalse(node.get(TRACK_TOTAL_HITS).asBoolean());
        // Query filters (order-agnostic assertions)
        JsonNode filters = node.at("/query/bool/filter");
        assertTrue(filters.isArray());
        assertTrue(hasTermFilter(filters, "api-id", API_ID));
        assertTrue(hasTimestampRange(filters));
        assertTrue(hasExistsForAll(filters, Set.of("upstream-active-connections", "downstream-active-connections")));
        // Aggregations: composite by_dimensions
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
        assertLatestTopMetrics(subAggs, "upstream-active-connections", LATEST_VALUE_PREFIX);
        assertLatestTopMetrics(subAggs, "downstream-active-connections", LATEST_VALUE_PREFIX);
    }

    @Test
    void should_build_delta_query_for_composite_dimensions() throws JsonProcessingException {
        String apiId = API_ID;
        Instant from = Instant.ofEpochMilli(FROM);
        Instant to = Instant.ofEpochMilli(TO);
        List<Aggregation> aggregations = List.of(
            new Aggregation("downstream-publish-messages-total", AggregationType.DELTA),
            new Aggregation("upstream-publish-messages-total", AggregationType.DELTA)
        );
        HistogramQuery query = buildHistogramQuery(apiId, from, to, aggregations, null, null);

        String json = EventMetricsQueryAdapter.toESQuery(query);

        JsonNode node = MAPPER.readTree(json);
        // Root settings
        assertEquals(0, node.get(SIZE).asInt());
        assertFalse(node.get(TRACK_TOTAL_HITS).asBoolean());
        // Query filters (order-agnostic assertions)
        JsonNode filters = node.at("/query/bool/filter");
        assertTrue(filters.isArray());
        // api-id term filter is present
        assertTrue(hasTermFilter(filters, "api-id", apiId));
        // no root time range for DELTA
        assertFalse(hasAnyTimestampRange(filters));
        // exists for both fields
        assertTrue(hasExistsForAll(filters, Set.of("downstream-publish-messages-total", "upstream-publish-messages-total")));
        // Aggregations: composite with
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
        Aggregation agg1 = new Aggregation("downstream-publish-messages-total", AggregationType.TREND);
        Aggregation agg2 = new Aggregation("upstream-publish-messages-total", AggregationType.TREND);
        HistogramQuery query = buildHistogramQuery(
            API_ID,
            Instant.ofEpochMilli(FROM),
            Instant.ofEpochMilli(TO),
            List.of(agg1, agg2),
            Duration.ofMillis(60_000),
            null
        );

        String json = EventMetricsQueryAdapter.toESQuery(query);

        JsonNode node = MAPPER.readTree(json);
        // Root settings
        assertEquals(0, node.get(SIZE).asInt());
        assertFalse(node.get(TRACK_TOTAL_HITS).asBoolean());
        // Query filters: api-id term
        JsonNode filters = node.get(QUERY).get(BOOL).get(FILTER);
        assertTrue(filters.isArray());
        assertTrue(hasTermFilter(filters, "api-id", API_ID));
        // time range:TREND includes [from,to]
        assertTrue(hasTimestampRange(filters));
        // exists filters for both fields
        assertTrue(hasExistsForAll(filters, Set.of("downstream-publish-messages-total", "upstream-publish-messages-total")));
        // Aggregations: composite
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

    @Test
    void should_apply_optional_terms_filters_with_or_semantics() throws Exception {
        // Given
        Instant from = Instant.ofEpochMilli(FROM);
        Instant to = Instant.ofEpochMilli(TO);
        List<Aggregation> aggregations = List.of(
            new Aggregation("downstream-publish-messages-total", AggregationType.DELTA),
            new Aggregation("upstream-publish-messages-total", AggregationType.DELTA)
        );
        List<Term> terms = List.of(new Term("app-id", "1"), new Term("app-id", "2"), new Term("plan-id", "p1"));
        HistogramQuery query = buildHistogramQuery(API_ID, from, to, aggregations, Duration.ofMillis(60_000), terms);

        // When
        String json = EventMetricsQueryAdapter.toESQuery(query);

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

    private static boolean hasTimestampRange(JsonNode filters) {
        for (JsonNode f : filters) {
            if (f.has(RANGE) && f.get(RANGE).has(TIMESTAMP)) {
                JsonNode ts = f.get(RANGE).get(TIMESTAMP);
                boolean ok = (ts.hasNonNull(GTE)) && (ts.hasNonNull(LT));

                if (ok) return true;
            }
        }

        return false;
    }

    private static boolean hasExistsForAll(JsonNode filters, Set<String> expectedFields) {
        // OR semantics block: check exists under a bool.should with minimum_should_match=1
        // First, collect root-level exists (legacy AND path)
        Set<String> found = new HashSet<>();

        for (JsonNode f : filters) {
            if (f.has(EXISTS) && f.get(EXISTS).has(FIELD)) {
                found.add(f.get(EXISTS).get(FIELD).asText());
            }
            // Also inspect bool.should containers
            if (f.has(BOOL) && f.get(BOOL).has(SHOULD)) {
                JsonNode shouldArr = f.get(BOOL).get(SHOULD);

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

    private static void assertTrendSeries(JsonNode perIntervalAggs, String field) {
        String maxName = MAX_PREFIX + field;
        String derivativeName = DERIVATIVE_PREFIX + field;
        String nonNegativeName = NON_NEGATIVE_PREFIX + field;
        // max_<field>
        JsonNode maxNode = perIntervalAggs.get(maxName);
        assertNotNull(maxNode);
        assertEquals(field, maxNode.get(MAX).get(FIELD).asText());
        // derivative_<field>
        JsonNode derivativeNode = perIntervalAggs.get(derivativeName);
        assertNotNull(derivativeNode);
        assertTrue(derivativeNode.has(DERIVATIVE));
        assertEquals(maxName, derivativeNode.get(DERIVATIVE).get(BUCKETS_PATH).asText());
        assertEquals(INSERT_ZEROS, derivativeNode.get(DERIVATIVE).get(GAP_POLICY).asText());
        // non_negative_<field>
        JsonNode nonNegativeNode = perIntervalAggs.get(nonNegativeName);
        assertNotNull(nonNegativeNode);
        assertTrue(nonNegativeNode.has(BUCKET_SCRIPT));
        JsonNode bucketScript = nonNegativeNode.get(BUCKET_SCRIPT);
        assertTrue(bucketScript.has(BUCKETS_PATH));
        assertEquals(derivativeName, bucketScript.get(BUCKETS_PATH).get("d").asText());
        assertTrue(bucketScript.has(SCRIPT));
        assertEquals("params.d != null ? Math.max(params.d, 0) : 0", bucketScript.get(SCRIPT).asText());
    }

    private static boolean hasTermsFilter(JsonNode filters, String field, Set<String> expectedValues) {
        for (JsonNode filter : filters) {
            if (filter.has(TERMS) && filter.get(TERMS).has(field)) {
                JsonNode values = filter.get(TERMS).get(field);
                Set<String> found = new java.util.HashSet<>();

                if (values.isArray()) {
                    for (JsonNode value : values) found.add(value.asText());
                } else {
                    // ES allows both array and non-array in some serializers; accept both
                    found.add(values.asText());
                }

                return found.equals(expectedValues);
            }
        }

        return false;
    }

    private void assertStartTopMetrics(JsonNode aggregations, String field) {
        assertLatestTopMetrics(aggregations, field, ElasticsearchDsl.Names.START_BUCKET_PREFIX);
    }

    private void assertEndTopMetrics(JsonNode parentAggs, String field) {
        assertLatestTopMetrics(parentAggs, field, ElasticsearchDsl.Names.END_BUCKET_PREFIX);
    }

    private void assertLatestTopMetrics(JsonNode parentAggs, String field, String prefix) {
        String latestName = prefix + field;
        JsonNode latest = parentAggs.get(latestName);
        JsonNode latestTm = latest.get(TOP_METRICS);
        assertEquals(field, latestTm.get(METRICS).get(FIELD).asText());
        assertEquals(io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Sort.DESC, latestTm.get(SORT).get(TIMESTAMP).asText());
    }

    private static Set<String> compositeSourceFields(JsonNode sources) {
        Set<String> fields = new HashSet<>();

        if (sources != null && sources.isArray()) {
            for (JsonNode src : sources) {
                String wrapperKey = src.fieldNames().next();
                fields.add(src.get(wrapperKey).get(TERMS).get(FIELD).asText());

                if (wrapperKey.equals("topic")) {
                    assertTrue(src.get(wrapperKey).get(TERMS).get(MISSING_BUCKET).asBoolean());
                }

                if (wrapperKey.equals("app-id")) {
                    assertTrue(src.get(wrapperKey).get(TERMS).get(MISSING_BUCKET).asBoolean());
                }

                if (wrapperKey.equals("plan-id")) {
                    assertTrue(src.get(wrapperKey).get(TERMS).get(MISSING_BUCKET).asBoolean());
                }
            }
        }

        return fields;
    }

    private static @NotNull HistogramQuery buildHistogramQuery(
        String apiId,
        Instant from,
        Instant to,
        List<Aggregation> aggregations,
        Duration interval,
        List<Term> terms
    ) {
        var searchTermId = new SearchTermId(SearchTermId.SearchTerm.API, apiId);
        var timeRange = new io.gravitee.repository.log.v4.model.analytics.TimeRange(
            from,
            to,
            interval == null ? Optional.empty() : Optional.of(interval)
        );

        return new HistogramQuery(searchTermId, timeRange, aggregations, Optional.empty(), terms);
    }
}
