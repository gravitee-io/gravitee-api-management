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
import io.gravitee.repository.log.v4.model.analytics.AgentActivityQuery;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Builds the ES query for listing agent activity hops from the {@code v4-metrics} index.
 *
 * <p>Hops are A2A inbound calls (filtered by api-id) and LLM/MCP outbound calls
 * (filtered by application-id). Result is sorted by {@code @timestamp} descending
 * and paginated.
 */
public class AgentActivityQueryAdapter {

    private static final String TIMESTAMP_FIELD = "@timestamp";
    private static final String API_ID_FIELD = "api-id";
    private static final String APPLICATION_ID_FIELD = "application-id";
    private static final String REQUEST_ID_V4 = "request-id";
    private static final String REQUEST_ID_V2 = "id";

    public static String adaptHops(AgentActivityQuery query) {
        var must = new JsonArray();

        // Time range
        if (query.from() != 0 || query.to() != 0) {
            var range = new JsonObject();
            if (query.from() > 0) {
                range.put("gte", query.from());
            }
            if (query.to() > 0) {
                range.put("lte", query.to());
            }
            must.add(JsonObject.of("range", JsonObject.of(TIMESTAMP_FIELD, range)));
        }

        // A2A inbound: api-id matches this agent's proxy
        // OR LLM/MCP outbound: application-id is the agent
        var shouldClauses = new JsonArray();

        if (query.a2aApiId() != null && !query.a2aApiId().isEmpty()) {
            shouldClauses.add(JsonObject.of("term", JsonObject.of(API_ID_FIELD, query.a2aApiId())));
        }

        if (query.applicationIds() != null && !query.applicationIds().isEmpty()) {
            for (var appId : query.applicationIds()) {
                shouldClauses.add(JsonObject.of("term", JsonObject.of(APPLICATION_ID_FIELD, appId)));
            }
        }

        if (shouldClauses.isEmpty()) {
            must.add(JsonObject.of("match_none", JsonObject.of()));
        } else {
            must.add(JsonObject.of("bool", JsonObject.of("should", shouldClauses, "minimum_should_match", 1)));
        }

        var result = new JsonObject();
        result.put("size", query.size());
        result.put("from", query.page() * query.size());

        if (!must.isEmpty()) {
            result.put("query", JsonObject.of("bool", JsonObject.of("must", must)));
        }

        result.put("sort", JsonArray.of(JsonObject.of(TIMESTAMP_FIELD, JsonObject.of("order", "desc"))));

        return result.encode();
    }

    /**
     * Builds a query to fetch decisions for a set of request ids.
     */
    public static String adaptDecisions(AgentActivityQuery query, java.util.Set<String> requestIds) {
        var must = new JsonArray();

        must.add(JsonObject.of("terms", JsonObject.of("requestId", new JsonArray(new ArrayList<>(requestIds)))));

        // Scope to this agent's actor-id on decisions
        if (query.actorId() != null && !query.actorId().isEmpty()) {
            must.add(JsonObject.of("term", JsonObject.of("actorId", query.actorId())));
        }

        var result = new JsonObject();
        result.put("size", requestIds.size() * 5); // up to 5 decisions per hop
        result.put("query", JsonObject.of("bool", JsonObject.of("must", must)));

        return result.encode();
    }

    /**
     * Extracts all request-ids from a v4-metrics search response so the decisions query
     * can join by request-id.
     */
    public static Set<String> extractRequestIds(SearchResponse hopsResponse) {
        var ids = new HashSet<String>();
        if (hopsResponse == null) {
            return ids;
        }
        var hits = hopsResponse.getSearchHits();
        if (hits == null) {
            return ids;
        }
        for (var hit : hits.getHits()) {
            var source = hit.getSource();
            if (source == null) {
                continue;
            }
            var id = source.has(REQUEST_ID_V4)
                ? source.get(REQUEST_ID_V4).asText(null)
                : source.has(REQUEST_ID_V2)
                    ? source.get(REQUEST_ID_V2).asText(null)
                    : null;
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }
}
