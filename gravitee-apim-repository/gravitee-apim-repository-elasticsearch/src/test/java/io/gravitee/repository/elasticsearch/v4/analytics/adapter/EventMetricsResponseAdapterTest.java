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

import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.DOC_COUNT;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.KEY;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.FILTERED_PREFIX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.LATEST_PREFIX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.TOTAL_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.analytics.query.events.EventAnalyticsAggregate;
import io.gravitee.repository.log.v4.model.analytics.AggregationType;
import io.gravitee.repository.log.v4.model.analytics.HistogramQuery;
import io.gravitee.repository.log.v4.model.analytics.Term;
import io.gravitee.repository.log.v4.model.analytics.TimeRange;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class EventMetricsResponseAdapterTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String FIELD_DOWNSTREAM_ACTIVE_CONNECTIONS = "downstream-active-connections";
    private static final String FIELD_UPSTREAM_ACTIVE_CONNECTIONS = "upstream-active-connections";
    private static final String FIELD_DOWNSTREAM_PUBLISH_MESSAGES_COUNT_INCREMENT = "downstream-publish-messages-count-increment";
    private static final String FIELD_UPSTREAM_PUBLISH_MESSAGES_COUNT_INCREMENT = "upstream-publish-messages-count-increment";

    @Nested
    class AdaptResponse {

        @Test
        void should_return_empty_when_response_is_null() {
            var result = EventMetricsResponseAdapter.adapt(null, null);

            assertTrue(result.isEmpty());
        }

        @Test
        void should_return_empty_when_no_aggs_or_type_unknown() {
            SearchResponse response = new SearchResponse();
            response.setTimedOut(false);
            response.setAggregations(Collections.emptyMap());

            var result1 = EventMetricsResponseAdapter.adapt(response, buildHistogramQuery(Collections.emptyList()));

            assertTrue(result1.isEmpty());

            var mixed = List.of(
                new io.gravitee.repository.log.v4.model.analytics.Aggregation(FIELD_DOWNSTREAM_ACTIVE_CONNECTIONS, AggregationType.VALUE),
                new io.gravitee.repository.log.v4.model.analytics.Aggregation(FIELD_UPSTREAM_ACTIVE_CONNECTIONS, AggregationType.DELTA)
            );

            var result2 = EventMetricsResponseAdapter.adapt(response, buildHistogramQuery(mixed));

            assertTrue(result2.isEmpty());
        }
    }

    @Nested
    class ValueTests {

        @Test
        void should_adapt_value_response() {
            var response = new SearchResponse();
            response.setTimedOut(false);
            var aggregations = new HashMap<String, Aggregation>();
            aggregations.put(
                FILTERED_PREFIX + FIELD_DOWNSTREAM_ACTIVE_CONNECTIONS,
                buildValueAggregation(FIELD_DOWNSTREAM_ACTIVE_CONNECTIONS, 2.0)
            );
            aggregations.put(
                FILTERED_PREFIX + FIELD_UPSTREAM_ACTIVE_CONNECTIONS,
                buildValueAggregation(FIELD_UPSTREAM_ACTIVE_CONNECTIONS, 3.0)
            );
            aggregations.put(FILTERED_PREFIX + "no-hit", buildValueAggregationWithNoHit("no-hit"));
            response.setAggregations(aggregations);

            var query = buildHistogramQuery(
                List.of(
                    new io.gravitee.repository.log.v4.model.analytics.Aggregation(
                        FIELD_DOWNSTREAM_ACTIVE_CONNECTIONS,
                        AggregationType.VALUE
                    ),
                    new io.gravitee.repository.log.v4.model.analytics.Aggregation(FIELD_UPSTREAM_ACTIVE_CONNECTIONS, AggregationType.VALUE),
                    new io.gravitee.repository.log.v4.model.analytics.Aggregation("no-hit", AggregationType.VALUE)
                )
            );

            Optional<EventAnalyticsAggregate> result = EventMetricsResponseAdapter.adapt(response, query);

            assertTrue(result.isPresent());
            Map<String, List<Double>> values = result.get().values();
            assertEquals(List.of(2.0), values.get(FIELD_DOWNSTREAM_ACTIVE_CONNECTIONS));
            assertEquals(List.of(3.0), values.get(FIELD_UPSTREAM_ACTIVE_CONNECTIONS));
            var expected = new ArrayList<Double>();
            expected.add(null);
            assertEquals(expected, values.get("no-hit"));
        }

        private Aggregation buildValueAggregation(String field, double value) {
            final ObjectNode source = MAPPER.createObjectNode().put(field, value);
            final ObjectNode hit = MAPPER.createObjectNode().set("_source", source);
            final ObjectNode hits = MAPPER.createObjectNode().set("hits", MAPPER.createArrayNode().add(hit));
            final ObjectNode total = MAPPER.createObjectNode().put("value", 1);
            hits.set("total", total);

            var latestAgg = new HashMap<String, Object>();
            latestAgg.put("hits", hits);

            var filteredAgg = new Aggregation();
            filteredAgg.setAggregation(LATEST_PREFIX + field, latestAgg);
            return filteredAgg;
        }

        private Aggregation buildValueAggregationWithNoHit(String field) {
            final ObjectNode hits = MAPPER.createObjectNode().set("hits", MAPPER.createArrayNode());
            final ObjectNode total = MAPPER.createObjectNode().put("value", 0);
            hits.set("total", total);

            var latestAgg = new HashMap<String, Object>();
            latestAgg.put("hits", hits);

            var filteredAgg = new Aggregation();
            filteredAgg.setAggregation(LATEST_PREFIX + field, latestAgg);
            return filteredAgg;
        }
    }

    @Nested
    class DeltaTests {

        @Test
        void should_adapt_delta_response() {
            var response = new SearchResponse();
            response.setTimedOut(false);
            var aggregations = new HashMap<String, Aggregation>();
            aggregations.put(
                FILTERED_PREFIX + FIELD_DOWNSTREAM_PUBLISH_MESSAGES_COUNT_INCREMENT,
                buildDeltaAggregation(FIELD_DOWNSTREAM_PUBLISH_MESSAGES_COUNT_INCREMENT, 15.0)
            );
            aggregations.put(
                FILTERED_PREFIX + FIELD_UPSTREAM_PUBLISH_MESSAGES_COUNT_INCREMENT,
                buildDeltaAggregation(FIELD_UPSTREAM_PUBLISH_MESSAGES_COUNT_INCREMENT, 2.0)
            );
            aggregations.put(FILTERED_PREFIX + "no-value", buildDeltaAggregationWithNoValue("no-value"));
            response.setAggregations(aggregations);

            var query = buildHistogramQuery(
                List.of(
                    new io.gravitee.repository.log.v4.model.analytics.Aggregation(
                        FIELD_DOWNSTREAM_PUBLISH_MESSAGES_COUNT_INCREMENT,
                        AggregationType.DELTA
                    ),
                    new io.gravitee.repository.log.v4.model.analytics.Aggregation(
                        FIELD_UPSTREAM_PUBLISH_MESSAGES_COUNT_INCREMENT,
                        AggregationType.DELTA
                    ),
                    new io.gravitee.repository.log.v4.model.analytics.Aggregation("no-value", AggregationType.DELTA)
                )
            );

            Optional<EventAnalyticsAggregate> result = EventMetricsResponseAdapter.adapt(response, query);

            assertTrue(result.isPresent());
            Map<String, List<Double>> values = result.get().values();
            assertEquals(List.of(15.0), values.get(FIELD_DOWNSTREAM_PUBLISH_MESSAGES_COUNT_INCREMENT));
            assertEquals(List.of(2.0), values.get(FIELD_UPSTREAM_PUBLISH_MESSAGES_COUNT_INCREMENT));
            var expected = new ArrayList<Double>();
            expected.add(null);
            assertEquals(expected, values.get("no-value"));
        }

        private Aggregation buildDeltaAggregation(String field, double value) {
            var totalAgg = new HashMap<String, Object>();
            totalAgg.put("value", value);

            var filteredAgg = new Aggregation();
            filteredAgg.setAggregation(TOTAL_PREFIX + field, totalAgg);
            return filteredAgg;
        }

        private Aggregation buildDeltaAggregationWithNoValue(String field) {
            var totalAgg = new HashMap<String, Object>();
            totalAgg.put("value", null);

            var filteredAgg = new Aggregation();
            filteredAgg.setAggregation(TOTAL_PREFIX + field, totalAgg);
            return filteredAgg;
        }
    }

    @Nested
    class TrendTests {

        @Test
        void should_adapt_trend_response() {
            var response = new SearchResponse();
            response.setTimedOut(false);
            var aggregations = new HashMap<String, Aggregation>();
            var trendAgg = new Aggregation();
            var seriesWithNull = new HashMap<String, Double>();
            seriesWithNull.put(FIELD_UPSTREAM_PUBLISH_MESSAGES_COUNT_INCREMENT, 8.0);
            seriesWithNull.put("no-value", null);
            trendAgg.setBuckets(
                List.of(
                    intervalBucket(
                        1000L,
                        Map.of(
                            FIELD_DOWNSTREAM_PUBLISH_MESSAGES_COUNT_INCREMENT,
                            10.0,
                            FIELD_UPSTREAM_PUBLISH_MESSAGES_COUNT_INCREMENT,
                            5.0
                        )
                    ),
                    intervalBucket(2000L, Map.of(FIELD_DOWNSTREAM_PUBLISH_MESSAGES_COUNT_INCREMENT, 12.0)),
                    intervalBucket(3000L, seriesWithNull)
                )
            );
            aggregations.put("trend", trendAgg);
            response.setAggregations(aggregations);

            var query = buildHistogramQuery(
                List.of(
                    new io.gravitee.repository.log.v4.model.analytics.Aggregation(
                        FIELD_DOWNSTREAM_PUBLISH_MESSAGES_COUNT_INCREMENT,
                        AggregationType.TREND
                    ),
                    new io.gravitee.repository.log.v4.model.analytics.Aggregation(
                        FIELD_UPSTREAM_PUBLISH_MESSAGES_COUNT_INCREMENT,
                        AggregationType.TREND
                    ),
                    new io.gravitee.repository.log.v4.model.analytics.Aggregation("no-value", AggregationType.TREND)
                )
            );

            Optional<EventAnalyticsAggregate> result = EventMetricsResponseAdapter.adapt(response, query);

            assertTrue(result.isPresent());
            Map<String, List<Double>> values = result.get().values();
            assertEquals(List.of(10.0, 12.0), values.get(FIELD_DOWNSTREAM_PUBLISH_MESSAGES_COUNT_INCREMENT));
            assertEquals(List.of(5.0, 8.0), values.get(FIELD_UPSTREAM_PUBLISH_MESSAGES_COUNT_INCREMENT));
            var expected = new ArrayList<Double>();
            expected.add(null);
            assertEquals(expected, values.get("no-value"));
        }

        @Test
        void should_adapt_trend_rate_response() {
            var response = new SearchResponse();
            response.setTimedOut(false);
            var aggregations = new HashMap<String, Aggregation>();
            var trendAgg = new Aggregation();
            var seriesWithNull = new HashMap<String, Double>();
            seriesWithNull.put(FIELD_UPSTREAM_PUBLISH_MESSAGES_COUNT_INCREMENT, 8.0);
            seriesWithNull.put("no-value", null);
            trendAgg.setBuckets(
                List.of(
                    intervalBucket(
                        1000L,
                        Map.of(
                            FIELD_DOWNSTREAM_PUBLISH_MESSAGES_COUNT_INCREMENT,
                            10.0,
                            FIELD_UPSTREAM_PUBLISH_MESSAGES_COUNT_INCREMENT,
                            5.0
                        )
                    ),
                    intervalBucket(2000L, Map.of(FIELD_DOWNSTREAM_PUBLISH_MESSAGES_COUNT_INCREMENT, 12.0)),
                    intervalBucket(3000L, seriesWithNull)
                )
            );
            aggregations.put("trend", trendAgg);
            response.setAggregations(aggregations);

            var query = buildHistogramQuery(
                List.of(
                    new io.gravitee.repository.log.v4.model.analytics.Aggregation(
                        FIELD_DOWNSTREAM_PUBLISH_MESSAGES_COUNT_INCREMENT,
                        AggregationType.TREND_RATE
                    ),
                    new io.gravitee.repository.log.v4.model.analytics.Aggregation(
                        FIELD_UPSTREAM_PUBLISH_MESSAGES_COUNT_INCREMENT,
                        AggregationType.TREND_RATE
                    ),
                    new io.gravitee.repository.log.v4.model.analytics.Aggregation("no-value", AggregationType.TREND_RATE)
                ),
                Duration.ofSeconds(2)
            );

            Optional<EventAnalyticsAggregate> result = EventMetricsResponseAdapter.adapt(response, query);

            assertTrue(result.isPresent());
            Map<String, List<Double>> values = result.get().values();
            assertEquals(List.of(5.0, 6.0), values.get(FIELD_DOWNSTREAM_PUBLISH_MESSAGES_COUNT_INCREMENT));
            assertEquals(List.of(2.5, 4.0), values.get(FIELD_UPSTREAM_PUBLISH_MESSAGES_COUNT_INCREMENT));
            var expected = new ArrayList<Double>();
            expected.add(null);
            assertEquals(expected, values.get("no-value"));
        }

        @Test
        void should_adapt_trend_rate_response_with_single_value() {
            var response = new SearchResponse();
            response.setTimedOut(false);
            var aggregations = new HashMap<String, Aggregation>();
            var trendAgg = new Aggregation();
            trendAgg.setBuckets(List.of(intervalBucket(1000L, Map.of(FIELD_DOWNSTREAM_PUBLISH_MESSAGES_COUNT_INCREMENT, 10.0))));
            aggregations.put("trend", trendAgg);
            response.setAggregations(aggregations);

            var query = buildHistogramQuery(
                List.of(
                    new io.gravitee.repository.log.v4.model.analytics.Aggregation(
                        FIELD_DOWNSTREAM_PUBLISH_MESSAGES_COUNT_INCREMENT,
                        AggregationType.TREND_RATE
                    )
                ),
                Duration.ofSeconds(2)
            );

            Optional<EventAnalyticsAggregate> result = EventMetricsResponseAdapter.adapt(response, query);

            assertTrue(result.isPresent());
            Map<String, List<Double>> values = result.get().values();
            assertEquals(List.of(5.0), values.get(FIELD_DOWNSTREAM_PUBLISH_MESSAGES_COUNT_INCREMENT));
        }

        private JsonNode intervalBucket(long ts, Map<String, Double> series) {
            ObjectNode bucket = MAPPER.createObjectNode();
            bucket.put(KEY, ts);
            bucket.put(DOC_COUNT, 1);

            series.forEach((name, v) -> {
                ObjectNode node = MAPPER.createObjectNode();
                if (v != null) {
                    node.put("value", v);
                } else {
                    node.putNull("value");
                }
                bucket.set(TOTAL_PREFIX + name, node);
            });

            return bucket;
        }
    }

    private static HistogramQuery buildHistogramQuery(List<io.gravitee.repository.log.v4.model.analytics.Aggregation> aggregations) {
        return buildHistogramQuery(aggregations, Duration.ofSeconds(10));
    }

    private static HistogramQuery buildHistogramQuery(
        List<io.gravitee.repository.log.v4.model.analytics.Aggregation> aggregations,
        Duration interval
    ) {
        Optional<String> query = Optional.empty();
        List<Term> terms = List.of();

        Instant to = Instant.now();
        Instant from = to.minusSeconds(10);
        return new HistogramQuery(null, new TimeRange(from, to, interval), aggregations, query, terms);
    }
}
