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

import io.gravitee.repository.log.v4.model.analytics.AverageConnectionDurationQuery;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SearchAverageConnectionDurationQueryAdapter {

    public static final String ENTRYPOINTS_AGG = "entrypoints_agg";
    public static final String FIELD = "field";
    public static final String AVG_ENDED_REQUEST_DURATION_MS = "avg_ended_request_duration_ms";

    public static String adapt(AverageConnectionDurationQuery query, boolean isEntrypointIdKeyword) {
        var jsonContent = new HashMap<String, Object>();
        var esQuery = buildElasticQuery(Optional.ofNullable(query).orElse(new AverageConnectionDurationQuery()));
        jsonContent.put("size", 0);
        jsonContent.put("query", esQuery);
        jsonContent.put("aggs", buildAverageMessagesPerRequestPerEntrypointAggregate(isEntrypointIdKeyword));
        return new JsonObject(jsonContent).encode();
    }

    private static JsonObject buildAverageMessagesPerRequestPerEntrypointAggregate(boolean isEntrypointIdKeyword) {
        return JsonObject.of(
            ENTRYPOINTS_AGG,
            JsonObject.of(
                "terms",
                JsonObject.of(FIELD, isEntrypointIdKeyword ? "entrypoint-id" : "entrypoint-id.keyword"),
                "aggs",
                JsonObject.of(AVG_ENDED_REQUEST_DURATION_MS, JsonObject.of("avg", JsonObject.of(FIELD, "gateway-response-time-ms")))
            )
        );
    }

    private static JsonObject buildElasticQuery(AverageConnectionDurationQuery query) {
        var terms = new ArrayList<JsonObject>();

        // Compute ended requests only
        terms.add(JsonObject.of("term", JsonObject.of("request-ended", "true")));
        query.apiId().ifPresent(apiId -> terms.add(JsonObject.of("term", JsonObject.of("api-id", apiId))));

        var timestamp = new JsonObject();
        query.from().ifPresent(from -> timestamp.put("from", from.toEpochMilli()).put("include_lower", true));
        query.to().ifPresent(to -> timestamp.put("to", to.toEpochMilli()).put("include_upper", true));

        if (!timestamp.isEmpty()) {
            terms.add(JsonObject.of("range", JsonObject.of("@timestamp", timestamp)));
        }
        return JsonObject.of("bool", JsonObject.of("must", JsonArray.of(terms.toArray())));
    }
}
