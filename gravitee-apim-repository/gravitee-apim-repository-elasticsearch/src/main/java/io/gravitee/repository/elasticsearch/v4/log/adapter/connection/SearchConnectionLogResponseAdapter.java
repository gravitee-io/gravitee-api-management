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

import static io.gravitee.repository.elasticsearch.utils.JsonNodeUtils.asBooleanOrFalse;
import static io.gravitee.repository.elasticsearch.utils.JsonNodeUtils.asIntOr;
import static io.gravitee.repository.elasticsearch.utils.JsonNodeUtils.asTextOrNull;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.log.v4.model.LogResponse;
import io.gravitee.repository.log.v4.model.connection.ConnectionLog;
import java.util.List;

public class SearchConnectionLogResponseAdapter {

    private SearchConnectionLogResponseAdapter() {}

    public static LogResponse<ConnectionLog> adapt(SearchResponse response) {
        var hits = response.getSearchHits();
        if (hits == null) {
            return new LogResponse<>(0, List.of());
        }

        return new LogResponse<>(
            (int) hits.getTotal().getValue(),
            hits
                .getHits()
                .stream()
                .map(h -> buildFromSource(h.getSource()))
                .toList()
        );
    }

    private static ConnectionLog buildFromSource(JsonNode json) {
        return ConnectionLog.builder()
            .requestId(json.get("request-id").asText())
            .timestamp(asTextOrNull(json.get("@timestamp")))
            .applicationId(asTextOrNull(json.get("application-id")))
            .apiId(asTextOrNull(json.get("api-id")))
            .planId(asTextOrNull(json.get("plan-id")))
            .clientIdentifier(asTextOrNull(json.get("client-identifier")))
            .transactionId(asTextOrNull(json.get("transaction-id")))
            .method(HttpMethod.get(asIntOr(json.get("http-method"), 0)))
            .status(asIntOr(json.get("status"), 0))
            .requestEnded(asBooleanOrFalse(json.get("request-ended")))
            .entrypointId(asTextOrNull(json.get("entrypoint-id")))
            .build();
    }
}
