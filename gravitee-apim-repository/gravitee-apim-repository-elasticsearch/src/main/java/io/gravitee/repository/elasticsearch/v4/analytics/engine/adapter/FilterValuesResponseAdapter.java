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
package io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.log.v4.model.analytics.FilterValuesResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FilterValuesResponseAdapter {

    static final String AGG_NAME = "filter_values";
    static final String TOTAL_COUNT_AGG_NAME = "total_count";

    public FilterValuesResult adapt(SearchResponse response) {
        if (response == null || response.getAggregations() == null || response.getAggregations().isEmpty()) {
            return new FilterValuesResult(Collections.emptyList(), null, 0);
        }

        var agg = response.getAggregations().get(AGG_NAME);
        if (agg == null) {
            return new FilterValuesResult(Collections.emptyList(), null, 0);
        }

        var values = extractValues(agg);
        var afterKey = extractAfterKey(agg);
        var totalCount = extractTotalCount(response);

        return new FilterValuesResult(values, afterKey, totalCount);
    }

    private long extractTotalCount(SearchResponse response) {
        var totalCountAgg = response.getAggregations().get(TOTAL_COUNT_AGG_NAME);
        if (totalCountAgg == null || totalCountAgg.getValue() == null) {
            return 0;
        }
        return totalCountAgg.getValue().longValue();
    }

    private List<String> extractValues(Aggregation agg) {
        var buckets = agg.getBuckets();
        if (buckets == null || buckets.isEmpty()) {
            return Collections.emptyList();
        }

        var values = new ArrayList<String>(buckets.size());
        for (var bucket : buckets) {
            var keyNode = bucket.get("key");
            if (keyNode != null && keyNode.has("value")) {
                values.add(keyNode.get("value").asText());
            }
        }
        return values;
    }

    private Map<String, Object> extractAfterKey(Aggregation agg) {
        var afterKeyNode = agg.getAfterKey();
        if (afterKeyNode == null || afterKeyNode.isNull() || !afterKeyNode.isObject()) {
            return null;
        }

        var afterKey = new HashMap<String, Object>();
        afterKeyNode.fields().forEachRemaining(entry -> afterKey.put(entry.getKey(), extractJsonValue(entry.getValue())));
        return afterKey;
    }

    private Object extractJsonValue(JsonNode node) {
        if (node.isTextual()) return node.asText();
        if (node.isLong()) return node.asLong();
        if (node.isInt()) return node.asInt();
        if (node.isDouble()) return node.asDouble();
        if (node.isBoolean()) return node.asBoolean();
        return node.asText();
    }
}
