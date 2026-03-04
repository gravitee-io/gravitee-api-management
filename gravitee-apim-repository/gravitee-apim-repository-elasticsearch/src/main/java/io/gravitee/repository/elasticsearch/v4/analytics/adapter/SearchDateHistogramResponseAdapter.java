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
import io.gravitee.repository.log.v4.model.analytics.DateHistoAggregate;
import io.gravitee.repository.log.v4.model.analytics.DateHistoAggregate.DateHistoBucketAggregate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SearchDateHistogramResponseAdapter {

    private static final String BY_DATE_AGG = "by_date";
    private static final String BY_FIELD_AGG = "by_field";

    public static Optional<DateHistoAggregate> adapt(SearchResponse response) {
        if (response == null || response.getAggregations() == null || response.getAggregations().isEmpty()) {
            return Optional.of(emptyDateHisto());
        }

        Aggregation byDateAgg = response.getAggregations().get(BY_DATE_AGG);
        if (byDateAgg == null || byDateAgg.getBuckets() == null) {
            return Optional.of(emptyDateHisto());
        }

        var timestamps = new ArrayList<Long>();
        var timestampToFieldCounts = new TreeMap<Long, Map<String, Long>>();
        var fieldValues = new TreeSet<String>();

        for (JsonNode dateBucket : byDateAgg.getBuckets()) {
            long keyAsDate = dateBucket.get("key").asLong();
            timestamps.add(keyAsDate);
            var fieldCount = new LinkedHashMap<String, Long>();

            JsonNode byFieldNode = dateBucket.get(BY_FIELD_AGG);
            if (byFieldNode != null && byFieldNode.has("buckets")) {
                for (JsonNode fieldBucket : byFieldNode.get("buckets")) {
                    String fieldKey = fieldBucket.get("key").asText();
                    long docCount = fieldBucket.has("doc_count") ? fieldBucket.get("doc_count").asLong() : 0L;
                    fieldCount.put(fieldKey, docCount);
                    fieldValues.add(fieldKey);
                }
            }
            timestampToFieldCounts.put(keyAsDate, fieldCount);
        }

        var values = new ArrayList<DateHistoBucketAggregate>();
        for (String fieldValue : fieldValues) {
            var buckets = new ArrayList<Long>();
            for (Long ts : timestamps) {
                long count = timestampToFieldCounts.getOrDefault(ts, Map.of()).getOrDefault(fieldValue, 0L);
                buckets.add(count);
            }
            values.add(new DateHistoBucketAggregate(fieldValue, buckets, Map.of("name", fieldValue)));
        }

        return Optional.of(new DateHistoAggregate(timestamps, values));
    }

    private static DateHistoAggregate emptyDateHisto() {
        return new DateHistoAggregate(Collections.emptyList(), Collections.emptyList());
    }
}
