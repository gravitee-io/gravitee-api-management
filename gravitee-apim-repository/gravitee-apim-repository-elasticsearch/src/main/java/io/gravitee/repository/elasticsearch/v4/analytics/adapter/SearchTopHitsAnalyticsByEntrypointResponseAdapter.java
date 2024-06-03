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

import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.SearchTopHitsAnalyticsByEntrypointQueryAdapter.ENTRYPOINT_ID_AGG;
import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.SearchTopHitsAnalyticsByEntrypointQueryAdapter.STATUS_RANGES;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SearchTopHitsAnalyticsByEntrypointResponseAdapter {

    public static Optional<Map<String, Map<String, Long>>> adapt(SearchResponse response) {
        final Map<String, Aggregation> aggregations = response.getAggregations();
        if (aggregations == null || aggregations.isEmpty()) {
            return Optional.empty();
        }
        final var entrypointsAggregation = aggregations.get(ENTRYPOINT_ID_AGG);
        if (entrypointsAggregation == null) {
            return Optional.empty();
        }

        final var result = entrypointsAggregation.getBuckets().stream()
                .collect(
                        Collectors.toMap(
                                jsonNode -> jsonNode.get("key").asText(),
                                jsonNode -> processStatusRanges(jsonNode.get(STATUS_RANGES))
                        )
                );
        return Optional.of(result);
    }

    private static Map<String, Long> processStatusRanges(JsonNode jsonNode) {
        if (jsonNode == null) {
            return Collections.emptyMap();
        }

        final var buckets = jsonNode.get("buckets");
        if (buckets == null || buckets.isEmpty()) {
            return Collections.emptyMap();
        }

        final var result = new HashMap<String, Long>();
        for (final var bucket : buckets) {
            final var count = bucket.get("count").get("value").asLong();
            result.put(bucket.get("key").asText(), count);
        }

        return result;
    }
}
