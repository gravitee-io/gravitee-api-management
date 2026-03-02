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

import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.SearchGroupByQueryAdapter.BY_FIELD_AGG;

import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.log.v4.model.analytics.GroupByAggregate;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SearchGroupByResponseAdapter {

    public static Optional<GroupByAggregate> adapt(SearchResponse response) {
        final Map<String, Aggregation> aggregations = response.getAggregations();
        if (aggregations == null || aggregations.isEmpty()) {
            return Optional.empty();
        }
        final var byFieldAggregation = aggregations.get(BY_FIELD_AGG);
        if (byFieldAggregation == null || byFieldAggregation.getBuckets() == null || byFieldAggregation.getBuckets().isEmpty()) {
            return Optional.empty();
        }

        final var values = byFieldAggregation
            .getBuckets()
            .stream()
            .collect(Collectors.toMap(jsonNode -> jsonNode.get("key").asText(), jsonNode -> jsonNode.get("doc_count").asLong()));

        return Optional.of(GroupByAggregate.builder().values(values).build());
    }
}
