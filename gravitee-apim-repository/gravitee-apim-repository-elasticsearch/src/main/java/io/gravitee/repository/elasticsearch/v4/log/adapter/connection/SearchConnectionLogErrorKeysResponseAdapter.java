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
package io.gravitee.repository.elasticsearch.v4.log.adapter.connection;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Maps the Elasticsearch {@link SearchResponse} for an error-keys aggregation query
 * into a plain {@code List<String>}.
 *
 * <p>The response is expected to contain a {@code terms} aggregation named
 * {@code "error_keys"}, whose buckets each carry a {@code "key"} field with the
 * error key string. Null, blank, and missing keys are silently skipped.
 *
 * <p>Jackson {@link JsonNode} is used deliberately here because
 * {@link Aggregation#getBuckets()} returns {@code List<JsonNode>} — this is the
 * correct and unavoidable way to read aggregation buckets in this project.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SearchConnectionLogErrorKeysResponseAdapter {

    private static final String AGG_NAME = "error_keys";
    private static final String BUCKET_KEY_FIELD = "key";

    public static List<String> adapt(SearchResponse response) {
        if (response == null) {
            return List.of();
        }

        Map<String, Aggregation> aggregations = response.getAggregations();
        if (aggregations == null || aggregations.isEmpty()) {
            return List.of();
        }

        Aggregation errorKeyAgg = aggregations.get(AGG_NAME);
        if (errorKeyAgg == null || errorKeyAgg.getBuckets() == null) {
            return List.of();
        }

        List<String> results = new ArrayList<>();
        for (JsonNode bucket : errorKeyAgg.getBuckets()) {
            JsonNode keyNode = bucket.get(BUCKET_KEY_FIELD);
            if (keyNode != null && !keyNode.isNull()) {
                String key = keyNode.asText();
                if (!key.isBlank()) {
                    results.add(key);
                }
            }
        }
        return results;
    }
}
