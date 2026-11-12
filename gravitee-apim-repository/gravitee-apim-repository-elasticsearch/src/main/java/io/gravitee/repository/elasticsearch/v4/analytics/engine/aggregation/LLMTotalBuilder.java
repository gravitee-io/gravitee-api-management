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
package io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation;

import io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs;
import io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys;
import io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Query;
import io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Tokens;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Map;

/**
 * An LLM total: the sent and the received value of a request added together, a side the request does not carry
 * counting for nothing. A request carrying neither has no total, so it is left out of the average, as a request without
 * the value is on every other metric.
 */
public record LLMTotalBuilder(String sentField, String receivedField) {
    private static final String SCRIPT = """
        double total = 0;
        boolean carried = false;
        if (doc.containsKey('%1$s') && doc['%1$s'].size() > 0) { total += doc['%1$s'].value; carried = true; }
        if (doc.containsKey('%2$s') && doc['%2$s'].size() > 0) { total += doc['%2$s'].value; carried = true; }
        if (!carried) { return null; }
        return total;
        """;

    public Map<String, JsonObject> buildSum(String aggName) {
        return scripted(aggName, Aggs.SUM);
    }

    public Map<String, JsonObject> buildAvg(String aggName) {
        return scripted(aggName, Aggs.AVG);
    }

    /** The requests carrying at least one of the two values. */
    public JsonObject carrying() {
        return JsonObject.of(
            Keys.BOOL,
            JsonObject.of(Query.SHOULD, JsonArray.of(exists(sentField), exists(receivedField)), Query.MINIMUM_SHOULD_MATCH, 1)
        );
    }

    private Map<String, JsonObject> scripted(String aggName, String type) {
        var source = SCRIPT.formatted(sentField, receivedField);
        return Map.of(aggName, JsonObject.of(type, JsonObject.of(Aggs.SCRIPT, JsonObject.of(Aggs.SCRIPT_SOURCE, source))));
    }

    private static JsonObject exists(String field) {
        return JsonObject.of(Query.EXISTS, JsonObject.of(Tokens.FIELD, field));
    }
}
