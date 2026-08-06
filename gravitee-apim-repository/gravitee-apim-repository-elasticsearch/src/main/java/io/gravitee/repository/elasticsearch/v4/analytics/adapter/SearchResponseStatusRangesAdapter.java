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

import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys.TRACK_TOTAL_HITS;
import static io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Limits.ENTRYPOINT_BUCKETS;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchHits;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.elasticsearch.model.TotalHits;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusQueryCriteria;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusRangesAggregate;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.CustomLog;

@CustomLog
public class SearchResponseStatusRangesAdapter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static final String BY_ENTRYPOINT_ID_AGG = "entrypoint_id_agg";
    public static final String FIELD = "field";
    public static final String STATUS_RANGES = "status_ranges";
    static final String ALL_APIS_STATUS_RANGES = "all_apis_status_ranges";

    /**
     * Key for the requests the status ranges cannot classify. Kept out of the {@code <n>xx} shape on
     * purpose: the console renders any unrecognised key as its own neutral slice.
     */
    public static final String UNKNOWN_RANGE = "unknown";

    public String adaptQuery(ResponseStatusQueryCriteria query, boolean isEntrypointIdKeyword) {
        return json()
            .put("size", 0)
            // Needed to derive the UNKNOWN_RANGE bucket: the status ranges only span [100, 600),
            // so the requests they leave out are the ones the query matched minus the ones they hold.
            .put(TRACK_TOTAL_HITS, true)
            .<ObjectNode>set("query", buildElasticQuery(query))
            .set("aggs", buildResponseCountPerStatusCodeRangePerEntrypointAggregation(isEntrypointIdKeyword))
            .toString();
    }

    private ObjectNode buildElasticQuery(ResponseStatusQueryCriteria queryParams) {
        var filterQuery = array();

        if (queryParams == null || queryParams.apiIds() == null) {
            log.warn("Null query params or queried API IDs. Empty ranges will be returned");
            filterQuery.add(apiIdsFilterForQuery(List.of()));
        } else {
            filterQuery.add(apiIdsFilterForQuery(queryParams.apiIds()));
        }

        if (queryParams != null && queryParams.from() != null && queryParams.to() != null) {
            filterQuery.add(dateRangeFilterForQuery(queryParams.from(), queryParams.to()));
        }

        return json().set("bool", json().set("filter", filterQuery));
    }

    private static ObjectNode apiIdsFilterForQuery(List<String> apiIds) {
        var terms = array();
        terms.add(json().set("terms", json().set("api-id", toArray(apiIds))));
        terms.add(json().set("terms", json().set("api", toArray(apiIds))));
        return buildShould(terms);
    }

    private static ObjectNode buildShould(ArrayNode terms) {
        return json().set("bool", json().set("should", terms));
    }

    private static ObjectNode dateRangeFilterForQuery(Long from, Long to) {
        log.info("Query filtering date range from {} to {}", new Date(from), new Date(to));
        return json().set("range", json().set("@timestamp", json().put("gte", from).put("lte", to)));
    }

    private static ObjectNode buildResponseCountPerStatusCodeRangePerEntrypointAggregation(boolean isEntrypointIdKeyword) {
        return json()
            .<ObjectNode>set(
                BY_ENTRYPOINT_ID_AGG,
                json()
                    .<ObjectNode>set(
                        "terms",
                        json().put(FIELD, isEntrypointIdKeyword ? "entrypoint-id" : "entrypoint-id.keyword").put("size", ENTRYPOINT_BUCKETS)
                    )
                    .<ObjectNode>set(
                        "aggs",
                        json().set(
                            STATUS_RANGES,
                            json().set(
                                "range",
                                json()
                                    .put(FIELD, "status")
                                    .set(
                                        "ranges",
                                        array()
                                            .add(json().put("from", 100.0).put("to", 200.0))
                                            .add(json().put("from", 200.0).put("to", 300.0))
                                            .add(json().put("from", 300.0).put("to", 400.0))
                                            .add(json().put("from", 400.0).put("to", 500.0))
                                            .add(json().put("from", 500.0).put("to", 600.0))
                                    )
                            )
                        )
                    )
            )
            .set(
                ALL_APIS_STATUS_RANGES,
                json().set(
                    "range",
                    json()
                        .put(FIELD, "status")
                        .set(
                            "ranges",
                            array()
                                .add(json().put("from", 100.0).put("to", 200.0))
                                .add(json().put("from", 200.0).put("to", 300.0))
                                .add(json().put("from", 300.0).put("to", 400.0))
                                .add(json().put("from", 400.0).put("to", 500.0))
                                .add(json().put("from", 500.0).put("to", 600.0))
                        )
                )
            );
    }

    public Optional<ResponseStatusRangesAggregate> adaptResponse(SearchResponse response) {
        final Map<String, Aggregation> aggregations = response.getAggregations();
        if (aggregations == null || aggregations.isEmpty()) {
            return Optional.empty();
        }
        final var byEntrypointsAggregation = aggregations.get(BY_ENTRYPOINT_ID_AGG);
        if (byEntrypointsAggregation == null) {
            return Optional.empty();
        }

        final Map<String, Map<String, Long>> statusRangesByEntrypoints = byEntrypointsAggregation
            .getBuckets()
            .stream()
            .collect(
                Collectors.toMap(
                    jsonNode -> jsonNode.get("key").asText(),
                    jsonNode -> withUnknownRange(processStatusRanges(jsonNode.get(STATUS_RANGES)), jsonNode.path("doc_count").asLong())
                )
            );

        final var allApisStatusRangesAggregation = aggregations.get(ALL_APIS_STATUS_RANGES);
        if (allApisStatusRangesAggregation == null) {
            return Optional.empty();
        }

        final Map<String, Long> allApisStatusRanges = allApisStatusRangesAggregation
            .getBuckets()
            .stream()
            .collect(Collectors.toMap(jsonNode -> jsonNode.get("key").asText(), jsonNode -> jsonNode.get("doc_count").asLong()));

        final var ranges = totalHits(response)
            .map(total -> withUnknownRange(allApisStatusRanges, total))
            .orElse(allApisStatusRanges);

        return Optional.of(
            ResponseStatusRangesAggregate.builder().statusRangesCountByEntrypoint(statusRangesByEntrypoints).ranges(ranges).build()
        );
    }

    /**
     * Bucket for everything the ranges do not classify, so that the breakdown always adds up to the
     * request total displayed next to it.
     * <p>
     * The ranges span [100, 600), so this holds requests whose status falls outside that window as
     * well as those carrying no status at all. In practice it is nearly always the latter: the
     * gateway leaves {@code status} at its {@code 0} default when a response status is never
     * committed — an aborted connection, a client that hung up — which is why the console labels the
     * bucket "No status". A status outside [100, 600) would land here too, but that takes an
     * upstream returning a value that is not a valid HTTP status.
     *
     * @param matchedRequests every request the query matched, whatever its status
     */
    private static Map<String, Long> withUnknownRange(Map<String, Long> statusRanges, long matchedRequests) {
        final long classified = statusRanges.values().stream().mapToLong(Long::longValue).sum();
        final var result = new LinkedHashMap<>(statusRanges);
        result.put(UNKNOWN_RANGE, Math.max(0, matchedRequests - classified));
        return result;
    }

    private static Optional<Long> totalHits(SearchResponse response) {
        return Optional.ofNullable(response.getSearchHits()).map(SearchHits::getTotal).map(TotalHits::getValue);
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
            final var count = bucket.get("doc_count").asLong();
            result.put(bucket.get("key").asText(), count);
        }

        return result;
    }

    private static ObjectNode json() {
        return MAPPER.createObjectNode();
    }

    private static ArrayNode array() {
        return MAPPER.createArrayNode();
    }

    private static ArrayNode toArray(List<String> list) {
        var arrayNode = array();
        list.forEach(arrayNode::add);
        return arrayNode;
    }
}
