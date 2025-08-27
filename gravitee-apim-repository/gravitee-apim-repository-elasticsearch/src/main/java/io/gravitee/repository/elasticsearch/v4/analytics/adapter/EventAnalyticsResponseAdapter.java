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

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.elasticsearch.model.SearchHit;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.analytics.query.stats.EventAnalyticsAggregate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Converts Elasticsearch metric aggregation responses (e.g. top_hits) to support `VALUE`, `DELTA` aggregation types.
 * to {@link io.gravitee.repository.analytics.query.stats.EventAnalyticsAggregate}.
 *
 * <p>Example: Converts a response like
 *   {"downstream-active-connections_latest": {"top_hits": {"hits": {...}}}}
 * to {@link io.gravitee.repository.analytics.query.stats.EventAnalyticsAggregate}.
 */
public class EventAnalyticsResponseAdapter {

    private EventAnalyticsResponseAdapter() {}

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

            if (subAggs != null) {
                io.gravitee.elasticsearch.model.Aggregation topHitsAgg = subAggs.get("top_hits");

                if (
                    topHitsAgg != null &&
                    topHitsAgg.getHits() != null &&
                    topHitsAgg.getHits().getHits() != null &&
                    !topHitsAgg.getHits().getHits().isEmpty()
                ) {
                    SearchHit topHit = topHitsAgg.getHits().getHits().getFirst();

                    if (topHit != null && topHit.getSource() != null) {
                        JsonNode source = topHit.getSource();
                        source.fieldNames().forEachRemaining(field -> result.put(key, Map.of(field, source.get(field).asLong())));
                    }
                }
            }
        });

        if (result.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new EventAnalyticsAggregate(result));
    }
}
