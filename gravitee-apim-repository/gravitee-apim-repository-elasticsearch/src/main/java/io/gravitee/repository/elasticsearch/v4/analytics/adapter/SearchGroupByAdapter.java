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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.log.v4.model.analytics.GroupByAggregate;
import io.gravitee.repository.log.v4.model.analytics.GroupByQueryCriteria;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Builds an ES {@code terms} aggregation query and parses its response (US-03 GROUP_BY).
 *
 * <p>ES query shape:</p>
 * <pre>{@code
 * {
 *   "size": 0,
 *   "query": { "bool": { "must": [...] } },
 *   "aggs": { "group_by": { "terms": { "field": "<field>", "size": <n>,
 *                                       "order": { "_count": "asc|desc" } } } }
 * }
 * }</pre>
 *
 * <p>Tie-breaking (AC 5): within equal counts, buckets are sorted by key ascending so
 * ordering is deterministic across identical requests. This is achieved by adding a
 * secondary sort on {@code _key} ascending.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SearchGroupByAdapter {

    static final String GROUP_BY_AGG = "group_by";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static String adaptQuery(GroupByQueryCriteria criteria) {
        String esOrder = criteria.order().equalsIgnoreCase("ASC") ? "asc" : "desc";

        // Primary sort on count, secondary on key ascending for stable tie-breaking (AC 5)
        var orderArray = MAPPER
            .createArrayNode()
            .add(json().put("_count", esOrder))
            .add(json().put("_key", "asc"));

        var termsAgg = json()
            .put("field", criteria.field())
            .put("size", criteria.size())
            .set("order", orderArray);

        return json()
            .put("size", 0)
            .<ObjectNode>set("query", buildQuery(criteria))
            .set("aggs", json().set(GROUP_BY_AGG, json().set("terms", termsAgg)))
            .toString();
    }

    public static Optional<GroupByAggregate> adaptResponse(SearchResponse response) {
        final Map<String, Aggregation> aggregations = response.getAggregations();
        if (aggregations == null || aggregations.isEmpty()) {
            return Optional.of(GroupByAggregate.builder().values(Collections.emptyMap()).build());
        }
        final Aggregation groupByAgg = aggregations.get(GROUP_BY_AGG);
        if (groupByAgg == null || groupByAgg.getBuckets() == null || groupByAgg.getBuckets().isEmpty()) {
            return Optional.of(GroupByAggregate.builder().values(Collections.emptyMap()).build());
        }

        // Use LinkedHashMap to preserve the order returned by ES
        final Map<String, Long> values = new LinkedHashMap<>();
        for (var bucket : groupByAgg.getBuckets()) {
            values.put(bucket.get("key").asText(), bucket.get("doc_count").asLong());
        }

        return Optional.of(GroupByAggregate.builder().values(values).build());
    }

    private static ObjectNode buildQuery(GroupByQueryCriteria criteria) {
        var must = MAPPER.createArrayNode();
        must.add(json().set("term", json().put("api-id", criteria.apiId())));
        if (criteria.from() != null && criteria.to() != null) {
            must.add(
                json()
                    .set(
                        "range",
                        json().set("@timestamp", json().put("gte", criteria.from().toEpochMilli()).put("lte", criteria.to().toEpochMilli()))
                    )
            );
        }
        return json().set("bool", json().set("must", must));
    }

    private static ObjectNode json() {
        return MAPPER.createObjectNode();
    }
}
