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

import io.gravitee.repository.log.v4.model.analytics.AverageMessagesPerRequestQuery;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SearchAverageMessagesPerRequestQueryAdapter {

    public static final String ENTRYPOINTS_AGG = "entrypoints_agg";
    public static final String FIELD = "field";
    public static final String MESSAGES_COUNT = "messages_count";
    public static final String DISTINCT_REQUESTS_COUNT = "distinct_requests_count";
    public static final String AVG_MESSAGES_PER_REQUEST = "avg_messages_per_request";

    public static String adapt(AverageMessagesPerRequestQuery query) {
        var jsonContent = new HashMap<String, Object>();
        var esQuery = buildElasticQuery(Optional.ofNullable(query).orElse(new AverageMessagesPerRequestQuery()));
        jsonContent.put("size", 0);
        jsonContent.put("query", esQuery);
        jsonContent.put("aggs", buildAverageMessagesPerRequestPerEntrypointAggregate());
        return new JsonObject(jsonContent).encode();
    }

    private static JsonObject buildAverageMessagesPerRequestPerEntrypointAggregate() {
        return JsonObject.of(
            ENTRYPOINTS_AGG,
            JsonObject.of(
                // Group by entrypoint id (already filtered by <connector-type: entrypoint>
                "terms",
                JsonObject.of(FIELD, "connector-id"),
                "aggs",
                JsonObject.of(
                    // Compute count of messages for an entrypoint
                    MESSAGES_COUNT,
                    JsonObject.of("sum", JsonObject.of(FIELD, "count-increment")),
                    // Compute count of distinct requests
                    DISTINCT_REQUESTS_COUNT,
                    JsonObject.of("cardinality", JsonObject.of(FIELD, "request-id")),
                    // Compute average messages per request thanks to previously computed messages_count and distinct_requests_count
                    AVG_MESSAGES_PER_REQUEST,
                    JsonObject.of(
                        "bucket_script",
                        JsonObject.of(
                            "buckets_path",
                            JsonObject.of("req_count", DISTINCT_REQUESTS_COUNT, "msg_count", MESSAGES_COUNT),
                            "script",
                            "params.msg_count / params.req_count"
                        )
                    )
                )
            )
        );
    }

    private static JsonObject buildElasticQuery(AverageMessagesPerRequestQuery query) {
        var terms = new ArrayList<JsonObject>();
        terms.add(JsonObject.of("term", JsonObject.of("connector-type", "entrypoint")));

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
