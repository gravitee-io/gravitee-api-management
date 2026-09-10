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

import io.gravitee.repository.elasticsearch.v4.analytics.engine.LlmProxyFields;
import io.vertx.core.json.JsonObject;
import java.util.Map;

/**
 * Builder for LLM total cost aggregations.
 * Computes the sum of sent and received token costs using Elasticsearch scripted aggregations.
 *
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LLMTotalCostBuilder {

    private static final String SCRIPT_SOURCE = """
        (doc.containsKey('%s') && doc['%s'].size() > 0 ? doc['%s'].value : 0) + \
        (doc.containsKey('%s') && doc['%s'].size() > 0 ? doc['%s'].value : 0)
        """.formatted(
            LlmProxyFields.SENT_COST,
            LlmProxyFields.SENT_COST,
            LlmProxyFields.SENT_COST,
            LlmProxyFields.RECEIVED_COST,
            LlmProxyFields.RECEIVED_COST,
            LlmProxyFields.RECEIVED_COST
        );

    public Map<String, JsonObject> buildSum(String aggName) {
        return Map.of(aggName, sum());
    }

    /**
     * The summed cost on its own, for a caller that nests it inside another aggregation — dividing it per
     * conversation, say — rather than reading it back under a name of its own.
     */
    public JsonObject sum() {
        return json().put("sum", json().put("script", script()));
    }

    public Map<String, JsonObject> buildAvg(String aggName) {
        return Map.of(aggName, json().put("avg", json().put("script", script())));
    }

    private JsonObject script() {
        return json().put("source", SCRIPT_SOURCE);
    }

    private JsonObject json() {
        return new JsonObject();
    }
}
