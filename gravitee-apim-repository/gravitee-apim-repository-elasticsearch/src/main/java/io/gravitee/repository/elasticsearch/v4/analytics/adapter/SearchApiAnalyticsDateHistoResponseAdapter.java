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

import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.log.v4.model.analytics.ApiAnalyticsDateHistoAggregate;
import io.gravitee.repository.log.v4.model.analytics.ApiAnalyticsMetadata;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SearchApiAnalyticsDateHistoResponseAdapter {

    public static Optional<ApiAnalyticsDateHistoAggregate> adapt(SearchResponse response) {
        if (response == null || response.getAggregations() == null || response.getAggregations().isEmpty()) {
            return Optional.empty();
        }

        final var byDate = response.getAggregations().get("by_date");
        if (byDate == null || byDate.getBuckets() == null) {
            return Optional.empty();
        }

        final List<Long> timestamps = new ArrayList<>();
        final Set<String> fields = new LinkedHashSet<>();

        byDate
            .getBuckets()
            .forEach(dateBucket -> {
                timestamps.add(dateBucket.get("key").asLong());
                final var byField = dateBucket.get("by_field");
                if (byField != null && byField.has("buckets")) {
                    byField.get("buckets").forEach(fieldBucket -> fields.add(fieldBucket.get("key").asText()));
                }
            });

        // field -> buckets aligned with timestamps
        final Map<String, List<Long>> seriesBuckets = new LinkedHashMap<>();
        fields.forEach(field -> {
            var buckets = new ArrayList<Long>(timestamps.size());
            for (int i = 0; i < timestamps.size(); i++) {
                buckets.add(0L);
            }
            seriesBuckets.put(field, buckets);
        });

        for (int i = 0; i < byDate.getBuckets().size(); i++) {
            final var dateBucket = byDate.getBuckets().get(i);
            final var byField = dateBucket.get("by_field");
            if (byField == null || !byField.has("buckets")) {
                continue;
            }
            for (var fieldBucket : byField.get("buckets")) {
                final var field = fieldBucket.get("key").asText();
                final var count = fieldBucket.get("doc_count").asLong();
                seriesBuckets.get(field).set(i, count);
            }
        }

        final List<ApiAnalyticsDateHistoAggregate.Series> values = seriesBuckets
            .entrySet()
            .stream()
            .map(e ->
                ApiAnalyticsDateHistoAggregate.Series
                    .builder()
                    .field(e.getKey())
                    .buckets(e.getValue())
                    .metadata(new ApiAnalyticsMetadata(e.getKey()))
                    .build()
            )
            .collect(java.util.stream.Collectors.toList());

        return Optional.of(ApiAnalyticsDateHistoAggregate.builder().timestamps(timestamps).values(values).build());
    }
}
