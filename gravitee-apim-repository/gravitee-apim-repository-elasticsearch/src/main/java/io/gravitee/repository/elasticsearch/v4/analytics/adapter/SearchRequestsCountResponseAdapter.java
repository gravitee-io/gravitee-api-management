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

import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.log.v4.model.analytics.CountAggregate;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.NoArgsConstructor;

@CustomLog
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SearchRequestsCountResponseAdapter {

    public static Optional<CountAggregate> adapt(SearchResponse response) {
        final Map<String, Aggregation> aggregations = response.getAggregations();
        if (aggregations == null || aggregations.isEmpty()) {
            return Optional.empty();
        }
        final var entrypointsAggregation = aggregations.get("entrypoints");
        if (entrypointsAggregation == null) {
            return Optional.empty();
        }

        final var requestsCountByEntrypoint = entrypointsAggregation
            .getBuckets()
            .stream()
            .collect(
                Collectors.toMap(
                    jsonNode -> jsonNode.get("key").asText(),
                    jsonNode -> {
                        final long docCount = jsonNode.get("doc_count").asLong();
                        return docCount;
                    }
                )
            );

        return Optional.of(buildFromSource(requestsCountByEntrypoint, totalCount(response, requestsCountByEntrypoint)));
    }

    /**
     * The total is the number of documents matching the query, whatever their entrypoint or their
     * response status. Deriving it from a subset of the documents would let a single entrypoint
     * count exceed the total.
     */
    private static long totalCount(SearchResponse response, Map<String, Long> requestsCountByEntrypoint) {
        final var searchHits = response.getSearchHits();
        if (searchHits != null && searchHits.getTotal() != null) {
            return searchHits.getTotal().getValue();
        }
        // Degenerate response without hits: the entrypoint breakdown is the best lower bound we
        // have, and it keeps the total consistent with what the breakdown displays.
        log.warn("Requests count response carries no total hits, falling back to the entrypoint breakdown");
        return requestsCountByEntrypoint.values().stream().mapToLong(Long::longValue).sum();
    }

    private static CountAggregate buildFromSource(Map<String, Long> requestsCountByEntrypoint, long totalCount) {
        return CountAggregate.builder().total(totalCount).countBy(requestsCountByEntrypoint).build();
    }
}
