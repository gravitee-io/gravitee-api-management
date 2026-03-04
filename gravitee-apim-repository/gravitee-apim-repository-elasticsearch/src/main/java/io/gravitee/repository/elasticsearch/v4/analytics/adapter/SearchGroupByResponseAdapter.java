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
import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.log.v4.model.analytics.GroupByAggregate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SearchGroupByResponseAdapter {

    private static final String GROUP_BY_AGG = "group_by_field";

    public static Optional<GroupByAggregate> adapt(SearchResponse response) {
        if (response == null || response.getAggregations() == null || response.getAggregations().isEmpty()) {
            return Optional.of(emptyGroupBy());
        }

        Aggregation agg = response.getAggregations().get(GROUP_BY_AGG);
        if (agg == null || agg.getBuckets() == null) {
            return Optional.of(emptyGroupBy());
        }

        var values = new LinkedHashMap<String, Long>();
        var metadata = new LinkedHashMap<String, Map<String, String>>();

        for (JsonNode bucket : agg.getBuckets()) {
            String key = bucket.get("key").asText();
            long docCount = bucket.has("doc_count") ? bucket.get("doc_count").asLong() : 0L;
            values.put(key, docCount);
            metadata.put(key, Map.of("name", key));
        }

        return Optional.of(new GroupByAggregate(values, metadata));
    }

    private static GroupByAggregate emptyGroupBy() {
        return new GroupByAggregate(Collections.emptyMap(), Collections.emptyMap());
    }
}
