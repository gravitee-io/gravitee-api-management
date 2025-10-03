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

import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.COMPOSITE;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.DATE_HISTOGRAM;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.FIXED_INTERVAL;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.METRICS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.SORT;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.SOURCES;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.TOP_HITS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.TOP_METRICS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.AGGS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.FILTER;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.SIZE;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.SOURCE;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.TIMESTAMP;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.TRACK_TOTAL_HITS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.END_BUCKET_PREFIX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.LATEST_BUCKET_PREFIX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.PER_INTERVAL;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.START_BUCKET_PREFIX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Query.EXISTS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Query.GTE;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Query.LTE;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Query.RANGE;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Query.TERM;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Sort.ASC;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Sort.DESC;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Sort.ORDER;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Tokens.FIELD;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Tokens.TERMS;
import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.TopHitsAggregationQueryAdapter.adapt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.gravitee.repository.log.v4.model.analytics.Aggregation;
import io.gravitee.repository.log.v4.model.analytics.AggregationType;
import io.gravitee.repository.log.v4.model.analytics.HistogramQuery;
import io.gravitee.repository.log.v4.model.analytics.SearchTermId;
import io.gravitee.repository.log.v4.model.analytics.Term;
import io.gravitee.repository.log.v4.model.analytics.TimeRange;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class TopHitsAggregationQueryAdapterTest {

    private static final String API_ID = "273f4728-1e30-4c78-bf47-281e304c78a5";
    private static final Long FROM = 1756104349879L;
    private static final Long TO = 1756190749879L;
    private static final String APP_ID = "1";
    private static final String PLAN_ID = "ec3c2f14-b669-4b4c-bc2f-14b6694b4c10";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void should_throw_when_no_aggregate_functions_specified() {
        assertThrows(IllegalArgumentException.class, () -> adapt(getHistogramQuery(API_ID, Instant.now(), Instant.now(), List.of())));
    }

    @Test
    void should_throw_when_more_than_one_aggregate_functions_specified() {
        Aggregation agg1 = new Aggregation("downstream-active-connections", AggregationType.VALUE);
        Aggregation agg2 = new Aggregation("upstream-active-connections", AggregationType.DELTA);
        HistogramQuery query = getHistogramQuery(API_ID, Instant.now(), Instant.now(), List.of(agg1, agg2));

        assertThrows(IllegalArgumentException.class, () -> adapt(query));
    }

    @Test
    void should_adapt_trend_query() throws JsonProcessingException {
        Aggregation agg1 = new Aggregation("downstream-publish-messages-total", AggregationType.TREND);
        Aggregation agg2 = new Aggregation("upstream-publish-messages-total", AggregationType.TREND);
        HistogramQuery query = getHistogramQuery(API_ID, Instant.now(), Instant.now(), List.of(agg1, agg2));

        String json = adapt(query);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(json);
        JsonNode aggs = node.get(AGGS);
        assertNotNull(aggs);
        assertFalse(aggs.isEmpty());

        // Per interval aggregation
        JsonNode perInterval = aggs.get(PER_INTERVAL);
        assertNotNull(perInterval);
        JsonNode histogram = perInterval.get(DATE_HISTOGRAM);
        assertNotNull(histogram);
        assertEquals(TIMESTAMP, histogram.get(FIELD).asText());
        assertNotNull(histogram.get(FIXED_INTERVAL));
        assertEquals("60000ms", histogram.get(FIXED_INTERVAL).asText());
        JsonNode perIntervalAggs = perInterval.get(AGGS);
        assertNotNull(perIntervalAggs);

        // Downstream publish messages total delta
        JsonNode downstreamDelta = perIntervalAggs.get("downstream-publish-messages-total_delta");
        assertNotNull(downstreamDelta);
        assertTrue(downstreamDelta.get(FILTER).has(EXISTS));
        assertEquals("downstream-publish-messages-total", downstreamDelta.get(FILTER).get(EXISTS).get(FIELD).asText());
        JsonNode downstreamStart = downstreamDelta.get(AGGS).get(START_BUCKET_PREFIX).get(TOP_HITS);
        assertEquals(1, downstreamStart.get(SIZE).asInt());
        assertEquals(ASC, downstreamStart.get(SORT).get(0).get(TIMESTAMP).get(ORDER).asText());
        assertEquals("downstream-publish-messages-total", downstreamStart.get(SOURCE).get(0).asText());
        JsonNode downstreamEnd = downstreamDelta.get(AGGS).get(END_BUCKET_PREFIX).get(TOP_HITS);
        assertEquals(1, downstreamEnd.get(SIZE).asInt());
        assertEquals(DESC, downstreamEnd.get(SORT).get(0).get(TIMESTAMP).get(ORDER).asText());
        assertEquals("downstream-publish-messages-total", downstreamEnd.get(SOURCE).get(0).asText());

        // Upstream publish messages total delta
        JsonNode upstreamDelta = perIntervalAggs.get("upstream-publish-messages-total_delta");
        assertNotNull(upstreamDelta);
        assertTrue(upstreamDelta.get(FILTER).has(EXISTS));
        assertEquals("upstream-publish-messages-total", upstreamDelta.get(FILTER).get(EXISTS).get(FIELD).asText());
        JsonNode upstreamStart = upstreamDelta.get(AGGS).get(START_BUCKET_PREFIX).get(TOP_HITS);
        assertEquals(1, upstreamStart.get(SIZE).asInt());
        assertEquals(ASC, upstreamStart.get(SORT).get(0).get(TIMESTAMP).get(ORDER).asText());
        assertEquals("upstream-publish-messages-total", upstreamStart.get(SOURCE).get(0).asText());
        JsonNode upstreamEnd = upstreamDelta.get(AGGS).get(END_BUCKET_PREFIX).get(TOP_HITS);
        assertEquals(1, upstreamEnd.get(SIZE).asInt());
        assertEquals(DESC, upstreamEnd.get(SORT).get(0).get(TIMESTAMP).get(ORDER).asText());
        assertEquals("upstream-publish-messages-total", upstreamEnd.get(SOURCE).get(0).asText());

        // Assert query filters
        JsonNode filters = node.get("query").get("bool").get(FILTER);
        assertFalse(filters.isEmpty());
        // Assert term filters
        assertEquals(API_ID, filters.get(0).get(TERM).get("api-id").asText());
        // Assert time range filters
        assertEquals(FROM, filters.get(1).get(RANGE).get(TIMESTAMP).get(GTE).asLong());
        assertEquals(TO, filters.get(1).get(RANGE).get(TIMESTAMP).get(LTE).asLong());
    }

    @Test
    void should_apply_filters() throws JsonProcessingException {
        Aggregation agg1 = new Aggregation("downstream-publish-messages-total", AggregationType.DELTA);
        Aggregation agg2 = new Aggregation("upstream-publish-messages-total", AggregationType.DELTA);
        Term appIdFilter1 = new Term("app-id", APP_ID);
        Term planIdFilter1 = new Term("plan-id", PLAN_ID);
        Term appIdFilter2 = new Term("app-id", "xxxx-xxxx-xxxx-xxxx");
        Term planIdFilter2 = new Term("plan-id", "yyyy-yyyy-yyyy-yyyy");
        var query = buildHistogramQuery(List.of(agg1, agg2), List.of(appIdFilter1, planIdFilter1, appIdFilter2, planIdFilter2));

        String json = adapt(query);

        JsonNode node = MAPPER.readTree(json);
        JsonNode aggs = node.get(AGGS);
        assertNotNull(aggs);
        assertFalse(aggs.isEmpty());
        // Assert query filters
        JsonNode filters = node.get("query").get("bool").get(FILTER);
        assertFalse(filters.isEmpty());
        // Assert default filters
        assertEquals(API_ID, filters.get(0).get(TERM).get("api-id").asText());
        assertEquals(FROM, filters.get(1).get(RANGE).get(TIMESTAMP).get(GTE).asLong());
        assertEquals(TO, filters.get(1).get(RANGE).get(TIMESTAMP).get(LTE).asLong());
        // Assert optional filters
        ArrayNode appTermsArray = (ArrayNode) filters.get(2).get("terms").get("app-id");
        List<String> appTermValues = StreamSupport.stream(appTermsArray.spliterator(), false).map(JsonNode::asText).toList();
        assertTrue(appTermValues.containsAll(List.of(APP_ID, "xxxx-xxxx-xxxx-xxxx")));
        ArrayNode planTermsArray = (ArrayNode) filters.get(3).get("terms").get("plan-id");
        List<String> planTermValues = StreamSupport.stream(planTermsArray.spliterator(), false).map(JsonNode::asText).toList();
        assertTrue(planTermValues.containsAll(List.of(PLAN_ID, "yyyy-yyyy-yyyy-yyyy")));
    }

    @Test
    void should_adapt_composite_latest_by_dimensions_query() throws Exception {
        // Given
        String apiId = "0a252b3c-c129-4cce-a52b-3cc129fcced7";
        Instant to = Instant.now();
        Instant from = to.minus(Duration.ofHours(14));

        List<Aggregation> aggregations = List.of(
            new Aggregation("upstream-active-connections", AggregationType.VALUE),
            new Aggregation("downstream-active-connections", AggregationType.VALUE)
        );
        HistogramQuery query = getHistogramQuery(apiId, from, to, aggregations);

        // When
        String json = TopHitsAggregationQueryAdapter.adapt(query);

        // Then
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(json);

        // Root settings
        assertEquals(0, node.get(SIZE).asInt());
        assertFalse(node.get(TRACK_TOTAL_HITS).asBoolean());

        // Query filters
        JsonNode filters = node.at("/query/bool/filter");
        assertTrue(filters.isArray());
        assertTrue(filters.size() >= 6);

        // api-id term filter
        assertEquals(apiId, filters.get(0).get(TERM).get("api-id").asText());

        // time range filter
        JsonNode range = filters.get(1).get(RANGE).get(TIMESTAMP);
        assertNotNull(range);
        assertTrue(range.hasNonNull(GTE));
        assertTrue(range.hasNonNull(LTE));

        // exists filters for metric fields (added by adapter)
        assertEquals("upstream-active-connections", filters.get(2).get(EXISTS).get(FIELD).asText());
        assertEquals("downstream-active-connections", filters.get(3).get(EXISTS).get(FIELD).asText());

        // multi-value terms filters (app-id then plan-id)
        JsonNode appTerms = filters.get(4).get(TERMS).get("app-id");
        assertTrue(appTerms.isArray());
        assertEquals("1", appTerms.get(0).asText());

        JsonNode planTerms = filters.get(5).get(TERMS).get("plan-id");
        assertTrue(planTerms.isArray());
        assertEquals("6d57d1f2-93e7-4240-97d1-f293e7c24052", planTerms.get(0).asText());

        // Aggregations: composite by_dimensions
        JsonNode byDimensions = node.at("/aggs/by_dimensions");
        assertFalse(byDimensions.isMissingNode());

        JsonNode composite = byDimensions.get(COMPOSITE);
        assertNotNull(composite);
        assertEquals(1000, composite.get(SIZE).asInt());

        JsonNode sources = composite.get(SOURCES);
        assertTrue(sources.isArray());
        assertEquals(4, sources.size());

        // Validate each source contains the expected field name (wrapper object key is not asserted)
        Set<String> expectedFields = Set.of("gw-id", "api-id", "org-id", "env-id");
        Set<String> actualFields = new HashSet<>();

        for (JsonNode src : sources) {
            assertTrue(src.fieldNames().hasNext());
            String wrapperKey = src.fieldNames().next();
            JsonNode termsNode = src.get(wrapperKey).get(TERMS);
            assertNotNull(termsNode);
            String fieldName = termsNode.get(FIELD).asText();
            actualFields.add(fieldName);
        }
        assertEquals(expectedFields, actualFields);

        // Sub-aggregations: top_metrics per metric, named "latest_" + fieldName
        JsonNode subAggs = byDimensions.get(AGGS);
        assertNotNull(subAggs);

        // upstream active connections
        String upstreamAggName = LATEST_BUCKET_PREFIX + "upstream-active-connections";
        JsonNode latestUpstream = subAggs.get(upstreamAggName);
        assertNotNull(latestUpstream);
        assertEquals("upstream-active-connections", latestUpstream.get(TOP_METRICS).get(METRICS).get(FIELD).asText());
        assertEquals(DESC, latestUpstream.get(TOP_METRICS).get(SORT).get(TIMESTAMP).asText());

        // downstream active connections
        String downstreamAggName = LATEST_BUCKET_PREFIX + "downstream-active-connections";
        JsonNode latestDownstream = subAggs.get(downstreamAggName);
        assertNotNull(latestDownstream);
        assertEquals("downstream-active-connections", latestDownstream.get(TOP_METRICS).get(METRICS).get(FIELD).asText());
        assertEquals(DESC, latestDownstream.get(TOP_METRICS).get(SORT).get(TIMESTAMP).asText());
    }

    @Test
    void should_adapt_composite_delta_by_dimensions_query() throws JsonProcessingException {
        // Given
        String apiId = API_ID;
        Instant from = Instant.ofEpochMilli(FROM);
        Instant to = Instant.ofEpochMilli(TO);
        List<Aggregation> aggregations = List.of(
                new io.gravitee.repository.log.v4.model.analytics.Aggregation(
                        "downstream-publish-messages-total",
                        io.gravitee.repository.log.v4.model.analytics.AggregationType.DELTA
                ),
                new io.gravitee.repository.log.v4.model.analytics.Aggregation(
                        "upstream-publish-messages-total",
                        io.gravitee.repository.log.v4.model.analytics.AggregationType.DELTA
                )
        );
        HistogramQuery query = getHistogramQuery(apiId, from, to, aggregations);

        // When
        String json = TopHitsAggregationQueryAdapter.adapt(query);

        // Then
        JsonNode node = MAPPER.readTree(json);

        // Root settings
        assertEquals(0, node.get(SIZE).asInt());
        assertFalse(node.get(TRACK_TOTAL_HITS).asBoolean());

        // Query filters
        JsonNode filters = node.at("/query/bool/filter");
        assertTrue(filters.isArray());
        assertTrue(filters.size() >= 4);

        // api-id term filter
        assertEquals(apiId, filters.get(0).get(TERM).get("api-id").asText());

        // time range filter (assert presence like the 'latest_by_dimensions' test)
        JsonNode range = filters.get(1).get(RANGE).get(TIMESTAMP);
        assertNotNull(range);
        assertTrue(range.hasNonNull(GTE));
        assertTrue(range.hasNonNull(LTE));

        // exists filters for metric fields (added by adapter)
        assertEquals("downstream-publish-messages-total", filters.get(2).get(EXISTS).get(FIELD).asText());
        assertEquals("upstream-publish-messages-total", filters.get(3).get(EXISTS).get(FIELD).asText());

        // Aggregations: composite by_dimensions
        JsonNode byDimensions = node.at("/aggs/by_dimensions");
        assertFalse(byDimensions.isMissingNode());

        JsonNode composite = byDimensions.get(COMPOSITE);
        assertNotNull(composite);
        assertEquals(1000, composite.get(SIZE).asInt());

        // Verify composite sources contain the expected fields (order-agnostic)
        JsonNode sources = composite.get(SOURCES);
        assertTrue(sources.isArray());
        assertEquals(7, sources.size());
        Set<String> expectedFields = Set.of("gw-id", "app-id", "org-id", "env-id", "api-id", "plan-id", "topic");
        Set<String> actualFields = new HashSet<>();

        for (JsonNode src : sources) {
            assertTrue(src.fieldNames().hasNext());
            String wrapperKey = src.fieldNames().next();
            JsonNode termsNode = src.get(wrapperKey).get(TERMS);
            assertNotNull(termsNode);
            actualFields.add(termsNode.get(FIELD).asText());
        }
        assertEquals(expectedFields, actualFields);

        // Sub-aggregations: start_/end_ top_metrics per metric
        JsonNode subAggs = byDimensions.get(AGGS);
        assertNotNull(subAggs);

        assertDeltaTopMetrics(subAggs, "downstream-publish-messages-total");
        assertDeltaTopMetrics(subAggs, "upstream-publish-messages-total");
    }

    private void assertDeltaTopMetrics(JsonNode subAggs, String field) {
        // start_<field>
        String startName = START_BUCKET_PREFIX + field;
        JsonNode start = subAggs.get(startName);
        JsonNode startTm = start.get(TOP_METRICS);
        assertEquals(field, startTm.get(METRICS).get(FIELD).asText());
        assertEquals(ASC, startTm.get(SORT).get(TIMESTAMP).asText());
        assertEquals(1, startTm.get(SIZE).asInt());

        // end_<field>
        String endName = END_BUCKET_PREFIX + field;
        JsonNode end = subAggs.get(endName);
        JsonNode endTm = end.get(TOP_METRICS);
        assertEquals(field, endTm.get(METRICS).get(FIELD).asText());
        assertEquals(DESC, endTm.get(SORT).get(TIMESTAMP).asText());
        assertEquals(1, endTm.get(SIZE).asInt());
    }

    private static @NotNull HistogramQuery getHistogramQuery(String apiId, Instant from, Instant to, List<Aggregation> aggregations) {
        var searchTermId = new SearchTermId(SearchTermId.SearchTerm.API, apiId);
        var timeRange = new TimeRange(from, to);

        // Optional filters (multi-value)
        var terms = List.of(new Term("app-id", "1"), new Term("plan-id", "6d57d1f2-93e7-4240-97d1-f293e7c24052"));

        return new HistogramQuery(searchTermId, timeRange, aggregations, Optional.empty(), terms);
    }

    private static @NotNull HistogramQuery buildHistogramQuery(List<Aggregation> aggregations, List<Term> terms) {
        return new HistogramQuery(
            new SearchTermId(SearchTermId.SearchTerm.API, API_ID),
            new TimeRange(Instant.ofEpochMilli(FROM), Instant.ofEpochMilli(TO), Duration.ofMillis(60000)),
            aggregations,
            null,
            terms
        );
    }
}
