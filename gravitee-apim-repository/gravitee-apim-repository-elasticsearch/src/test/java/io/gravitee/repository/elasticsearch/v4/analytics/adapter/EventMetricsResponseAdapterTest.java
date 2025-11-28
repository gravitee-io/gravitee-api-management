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

import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.BUCKETS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs.METRICS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.DOC_COUNT;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.KEY;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.BEFORE_START;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.BY_DIMENSIONS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.END_BUCKET_PREFIX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.END_IN_RANGE;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.LATEST_VALUE_PREFIX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.MAX_PREFIX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.PER_INTERVAL;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.START_BUCKET_PREFIX;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Names.TOP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
    private static final String FIELD_DOWNSTREAM_PUBLISH_MESSAGES_TOTAL = "downstream-publish-messages-total";
    private static final String FIELD_UPSTREAM_PUBLISH_MESSAGES_TOTAL = "upstream-publish-messages-total";

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
        void should_sum_latest_values_across_buckets() {
            // Bucket #1: downstream=2, upstream=3
            ObjectNode bucket1 = MAPPER.createObjectNode();
            bucket1.put(KEY, 1);
            bucket1.put(DOC_COUNT, 1);
            bucket1.set(
                LATEST_VALUE_PREFIX + FIELD_DOWNSTREAM_ACTIVE_CONNECTIONS,
                topMetricsHit(metric(FIELD_DOWNSTREAM_ACTIVE_CONNECTIONS, 2))
            );
            bucket1.set(
                LATEST_VALUE_PREFIX + FIELD_UPSTREAM_ACTIVE_CONNECTIONS,
                topMetricsHit(metric(FIELD_UPSTREAM_ACTIVE_CONNECTIONS, 3))
            );

            // Bucket #2: downstream=5, upstream non-numeric -> ignored
            ObjectNode bucket2 = MAPPER.createObjectNode();
            bucket2.put(KEY, 2);
            bucket2.put(DOC_COUNT, 1);
            bucket2.set(
                LATEST_VALUE_PREFIX + FIELD_DOWNSTREAM_ACTIVE_CONNECTIONS,
                topMetricsHit(metric(FIELD_DOWNSTREAM_ACTIVE_CONNECTIONS, 5))
            );
            bucket2.set(LATEST_VALUE_PREFIX + FIELD_UPSTREAM_ACTIVE_CONNECTIONS, emptyTop());

            // Bucket #3: missing top hit for downstream -> ignored for downstream
            ObjectNode bucket3 = MAPPER.createObjectNode();
            bucket3.put(KEY, 3);
            bucket3.put(DOC_COUNT, 1);
            bucket3.set(LATEST_VALUE_PREFIX + FIELD_DOWNSTREAM_ACTIVE_CONNECTIONS, emptyTop());

            SearchResponse response = buildResponse(List.of(bucket1, bucket2, bucket3));
            HistogramQuery query = buildHistogramQuery(
                List.of(
                    new io.gravitee.repository.log.v4.model.analytics.Aggregation(
                        FIELD_DOWNSTREAM_ACTIVE_CONNECTIONS,
                        AggregationType.VALUE
                    ),
                    new io.gravitee.repository.log.v4.model.analytics.Aggregation(FIELD_UPSTREAM_ACTIVE_CONNECTIONS, AggregationType.VALUE)
                )
            );

            Optional<EventAnalyticsAggregate> result = EventMetricsResponseAdapter.adapt(response, query);

            assertTrue(result.isPresent());
            Map<String, List<Long>> values = result.get().values();
            assertEquals(List.of(7L), values.get(FIELD_DOWNSTREAM_ACTIVE_CONNECTIONS));
            assertEquals(List.of(3L), values.get(FIELD_UPSTREAM_ACTIVE_CONNECTIONS));
        }
    }

    @Nested
    class DeltaTests {

        @Test
        void should_compute_delta_sum_across_buckets() {
            // Bucket #1: start downstream=10, end downstream=25 -> delta 15
            ObjectNode bucket1 = MAPPER.createObjectNode();
            bucket1.put(KEY, 1);
            bucket1.put(DOC_COUNT, 1);
            bucket1.set(
                BEFORE_START,
                MAPPER.createObjectNode().setAll(
                    Map.of(
                        START_BUCKET_PREFIX + FIELD_DOWNSTREAM_PUBLISH_MESSAGES_TOTAL,
                        topMetricsHit(metric(FIELD_DOWNSTREAM_PUBLISH_MESSAGES_TOTAL, 10))
                    )
                )
            );
            bucket1.set(
                END_IN_RANGE,
                MAPPER.createObjectNode().setAll(
                    Map.of(
                        END_BUCKET_PREFIX + FIELD_DOWNSTREAM_PUBLISH_MESSAGES_TOTAL,
                        topMetricsHit(metric(FIELD_DOWNSTREAM_PUBLISH_MESSAGES_TOTAL, 25))
                    )
                )
            );

            // Bucket #2: start downstream missing, end downstream=5 -> baseline 0 -> delta 5
            ObjectNode bucket2 = MAPPER.createObjectNode();
            bucket2.put(KEY, 2);
            bucket2.put(DOC_COUNT, 1);
            bucket2.set(BEFORE_START, MAPPER.createObjectNode()); // no start
            bucket2.set(
                END_IN_RANGE,
                MAPPER.createObjectNode().setAll(
                    Map.of(
                        END_BUCKET_PREFIX + FIELD_DOWNSTREAM_PUBLISH_MESSAGES_TOTAL,
                        topMetricsHit(metric(FIELD_DOWNSTREAM_PUBLISH_MESSAGES_TOTAL, 5))
                    )
                )
            );

            // Bucket #3: start upstream=40, end upstream=35 -> negative -> clamp to 0
            ObjectNode bucket3 = MAPPER.createObjectNode();
            bucket3.put(KEY, 3);
            bucket3.put(DOC_COUNT, 1);
            bucket3.set(
                BEFORE_START,
                MAPPER.createObjectNode().setAll(
                    Map.of(
                        START_BUCKET_PREFIX + FIELD_UPSTREAM_PUBLISH_MESSAGES_TOTAL,
                        topMetricsHit(metric(FIELD_UPSTREAM_PUBLISH_MESSAGES_TOTAL, 40))
                    )
                )
            );
            bucket3.set(
                END_IN_RANGE,
                MAPPER.createObjectNode().setAll(
                    Map.of(
                        END_BUCKET_PREFIX + FIELD_UPSTREAM_PUBLISH_MESSAGES_TOTAL,
                        topMetricsHit(metric(FIELD_UPSTREAM_PUBLISH_MESSAGES_TOTAL, 35))
                    )
                )
            );

            SearchResponse response = buildResponse(List.of(bucket1, bucket2, bucket3));
            HistogramQuery query = buildHistogramQuery(
                List.of(
                    new io.gravitee.repository.log.v4.model.analytics.Aggregation(
                        FIELD_DOWNSTREAM_PUBLISH_MESSAGES_TOTAL,
                        AggregationType.DELTA
                    ),
                    new io.gravitee.repository.log.v4.model.analytics.Aggregation(
                        FIELD_UPSTREAM_PUBLISH_MESSAGES_TOTAL,
                        AggregationType.DELTA
                    )
                )
            );

            Optional<EventAnalyticsAggregate> result = EventMetricsResponseAdapter.adapt(response, query);

            assertTrue(result.isPresent());
            Map<String, List<Long>> values = result.get().values();
            assertEquals(List.of(20L), values.get(FIELD_DOWNSTREAM_PUBLISH_MESSAGES_TOTAL));
            assertEquals(List.of(0L), values.get(FIELD_UPSTREAM_PUBLISH_MESSAGES_TOTAL));
        }
    }

    @Nested
    class TrendTests {

        @Test
        void should_sum_trend_values_reset_aware_per_timestamp() {
            long timestamp1 = 1000L;
            long timestamp2 = 2000L;
            // Dimension A intervals:
            ArrayNode downstreamIntervalsA = MAPPER.createArrayNode();
            downstreamIntervalsA.add(intervalBucket(timestamp1, Map.of(MAX_PREFIX + FIELD_DOWNSTREAM_PUBLISH_MESSAGES_TOTAL, 10L)));
            downstreamIntervalsA.add(intervalBucket(timestamp2, Map.of(MAX_PREFIX + FIELD_DOWNSTREAM_PUBLISH_MESSAGES_TOTAL, 3L)));
            ObjectNode dimensionABucket = MAPPER.createObjectNode();
            dimensionABucket.put(DOC_COUNT, 1);
            dimensionABucket.set(PER_INTERVAL, MAPPER.createObjectNode().set(BUCKETS, downstreamIntervalsA));
            // Dimension B intervals:
            ArrayNode downstreamIntervalsB = MAPPER.createArrayNode();
            downstreamIntervalsB.add(intervalBucket(timestamp1, Map.of(MAX_PREFIX + FIELD_DOWNSTREAM_PUBLISH_MESSAGES_TOTAL, 7L)));
            ObjectNode dimensionBBucket = MAPPER.createObjectNode();
            dimensionBBucket.put(DOC_COUNT, 1);
            dimensionBBucket.set(PER_INTERVAL, MAPPER.createObjectNode().set(BUCKETS, downstreamIntervalsB));
            SearchResponse response = buildResponse(List.of(dimensionABucket, dimensionBBucket));
            HistogramQuery query = buildHistogramQuery(
                List.of(
                    new io.gravitee.repository.log.v4.model.analytics.Aggregation(
                        FIELD_DOWNSTREAM_PUBLISH_MESSAGES_TOTAL,
                        AggregationType.TREND
                    )
                )
            );

            Optional<EventAnalyticsAggregate> aggregate = EventMetricsResponseAdapter.adapt(response, query);

            assertTrue(aggregate.isPresent());
            Map<String, List<Long>> valuesByField = aggregate.get().values();
            assertEquals(List.of(0L, 0L), valuesByField.get(FIELD_DOWNSTREAM_PUBLISH_MESSAGES_TOTAL));
        }
    }

    // ---------------- Builders / Helpers ----------------

    private static SearchResponse buildResponse(List<JsonNode> buckets) {
        Aggregation agg = new Aggregation();
        agg.setBuckets(buckets);

        Map<String, Aggregation> aggs = new HashMap<>();
        aggs.put(BY_DIMENSIONS, agg);

        SearchResponse response = new SearchResponse();
        response.setTimedOut(false);
        response.setAggregations(aggs);

        return response;
    }

    private static ObjectNode topMetricsHit(ObjectNode metrics) {
        // { "top": [ { "metrics": { ... } } ] }
        ObjectNode root = MAPPER.createObjectNode();
        ArrayNode top = MAPPER.createArrayNode();
        ObjectNode first = MAPPER.createObjectNode();
        first.set(METRICS, metrics);
        top.add(first);
        root.set(TOP, top);

        return root;
    }

    private static ObjectNode emptyTop() {
        ObjectNode root = MAPPER.createObjectNode();
        root.set(TOP, MAPPER.createArrayNode());
        // empty => adapter should ignore
        return root;
    }

    private static ObjectNode metric(String field, long value) {
        ObjectNode metrics = MAPPER.createObjectNode();
        metrics.put(field, value);

        return metrics;
    }

    private static ObjectNode intervalBucket(long ts, Map<String, Long> series) {
        ObjectNode bucket = MAPPER.createObjectNode();
        bucket.put(KEY, ts);
        bucket.put(DOC_COUNT, (long) 1);

        series.forEach((name, v) -> {
            ObjectNode node = MAPPER.createObjectNode();
            node.put("value", v);
            bucket.set(name, node);
        });

        return bucket;
    }

    private static HistogramQuery buildHistogramQuery(List<io.gravitee.repository.log.v4.model.analytics.Aggregation> aggregations) {
        Optional<String> query = Optional.empty();
        List<Term> terms = List.of();

        Instant to = Instant.now();
        Instant from = to.minusSeconds(10);
        return new HistogramQuery(null, new TimeRange(from, to, Duration.ofSeconds(10)), aggregations, query, terms);
    }
}
