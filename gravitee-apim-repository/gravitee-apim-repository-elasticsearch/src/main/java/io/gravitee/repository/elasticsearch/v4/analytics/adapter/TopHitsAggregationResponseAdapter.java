/*
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

import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.TopHitsAggregationQueryAdapter.END_VALUE;
import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.TopHitsAggregationQueryAdapter.START_VALUE;
import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.TopHitsAggregationQueryAdapter.TOP_HITS;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchHit;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.analytics.query.events.EventAnalyticsAggregate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Converts Elasticsearch metric aggregation responses to support VALUE (top_hits) and DELTA counter-aggregations,
 * and maps them to {@link EventAnalyticsAggregate}.
 *
 * <p>Examples:
 * <ul>
 *   <li>VALUE: <pre>
 *   {"downstream-active-connections_latest": {"top_hits": {"hits": {...}}}}
 *   </pre></li>
 *   <li>DELTA counters: <pre>
 *   {"downstream-publish-messages-total_delta": {
 *         "start_value": {"hits": {"hits": [{"_source": {"downstream-publish-messages-total": 0 }}]}},
 *         "end_value": {"hits": {"hits": [{"_source": {"downstream-publish-messages-total": 121}}]}}
 *   }}</pre></li>
 * </ul>
 */
public class TopHitsAggregationResponseAdapter {

    private TopHitsAggregationResponseAdapter() {}

    public static Optional<EventAnalyticsAggregate> adapt(SearchResponse response) {
        if (response == null || response.getTimedOut()) {
            return Optional.empty();
        }

        Map<String, io.gravitee.elasticsearch.model.Aggregation> aggregations = response.getAggregations();

        if (aggregations == null || aggregations.isEmpty()) {
            return Optional.empty();
        }

        Map<String, Map<String, Long>> result = new HashMap<>();

        aggregations.forEach((key, value) -> {
            Map<String, io.gravitee.elasticsearch.model.Aggregation> subAggs = value.getAggregations();
            if (subAggs != null && subAggs.containsKey(START_VALUE) && subAggs.containsKey(END_VALUE)) {
                parseDeltaAggregations(key, subAggs, result);
            } else if (subAggs != null && subAggs.containsKey(TOP_HITS)) {
                parseValueAggregations(key, subAggs, result);
            }
        });

        if (result.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new EventAnalyticsAggregate(result));
    }

    private static void parseValueAggregations(String key, Map<String, Aggregation> subAggs, Map<String, Map<String, Long>> result) {
        // VALUE aggregation: expects top_hits subaggregation
        Aggregation topHitsAgg = subAggs.get(TOP_HITS);

        if (
            topHitsAgg != null &&
            topHitsAgg.getHits() != null &&
            topHitsAgg.getHits().getHits() != null &&
            !topHitsAgg.getHits().getHits().isEmpty()
        ) {
            // Extract field and values from the _source of the first hit
            SearchHit topHit = topHitsAgg.getHits().getHits().getFirst();

            if (topHit != null && topHit.getSource() != null) {
                JsonNode source = topHit.getSource();
                source.fieldNames().forEachRemaining(field -> result.put(key, Map.of(field, source.get(field).asLong())));
            }
        }
    }

    private static void parseDeltaAggregations(String key, Map<String, Aggregation> subAggs, Map<String, Map<String, Long>> result) {
        // DELTA aggregation: expects start_value and end_value subaggregations
        Aggregation startAgg = subAggs.get(START_VALUE);
        Aggregation endAgg = subAggs.get(END_VALUE);
        Long start = null;
        Long end = null;
        String fieldName = null;
        // Extract field and values from the _source of the first hit in each
        if (startAgg.getHits() != null && startAgg.getHits().getHits() != null && !startAgg.getHits().getHits().isEmpty()) {
            SearchHit hit = startAgg.getHits().getHits().getFirst();

            if (hit != null && hit.getSource() != null && hit.getSource().fieldNames().hasNext()) {
                fieldName = hit.getSource().fieldNames().next();
                start = hit.getSource().get(fieldName).asLong();
            }
        }

        if (endAgg.getHits() != null && endAgg.getHits().getHits() != null && !endAgg.getHits().getHits().isEmpty()) {
            SearchHit hit = endAgg.getHits().getHits().getFirst();

            if (hit != null && hit.getSource() != null && hit.getSource().fieldNames().hasNext()) {
                if (fieldName == null) fieldName = hit.getSource().fieldNames().next();
                end = hit.getSource().get(fieldName).asLong();
            }
        }

        if (fieldName != null && start != null && end != null) {
            result.put(key, Map.of(fieldName, end - start));
        }
    }
}
