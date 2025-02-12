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
import io.gravitee.elasticsearch.utils.Type;
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
            hits.getHits().stream().map(h -> buildFromSource(h.getIndex(), h.getId(), h.getSource())).toList()
        );
    }

    private static ConnectionLog buildFromSource(String index, String id, JsonNode json) {
        var connectionLog = ConnectionLog
            .builder()
            .timestamp(asTextOrNull(json.get(ConnectionLogField.TIMESTAMP)))
            .status(asIntOr(json.get(ConnectionLogField.STATUS), 0))
            .gateway(asTextOrNull(json.get(ConnectionLogField.GATEWAY)))
            .uri(asTextOrNull(json.get(ConnectionLogField.URI)));

        if (index.contains(Type.REQUEST.getType())) {
            return connectionLog
                .requestId(id)
                .applicationId(asTextOrNull(json.get(ConnectionLogField.APPLICATION_ID.v2Request())))
                .apiId(asTextOrNull(json.get(ConnectionLogField.API_ID.v2Request())))
                .planId(asTextOrNull(json.get(ConnectionLogField.PLAN_ID.v2Request())))
                .clientIdentifier(asTextOrNull(json.get(ConnectionLogField.CLIENT_IDENTIFIER.v2Request())))
                .transactionId(asTextOrNull(json.get(ConnectionLogField.TRANSACTION_ID.v2Request())))
                .method(HttpMethod.get(asIntOr(json.get(ConnectionLogField.HTTP_METHOD.v2Request()), 0)))
                .requestEnded(true)
                .entrypointId(null)
                .gatewayResponseTime(json.get(ConnectionLogField.GATEWAY_RESPONSE_TIME.v2Request()).asLong(0L))
                .build();
        }
        return connectionLog
            .requestId(json.get(ConnectionLogField.REQUEST_ID.v4Metrics()).asText())
            .applicationId(asTextOrNull(json.get(ConnectionLogField.APPLICATION_ID.v4Metrics())))
            .apiId(asTextOrNull(json.get(ConnectionLogField.API_ID.v4Metrics())))
            .planId(asTextOrNull(json.get(ConnectionLogField.PLAN_ID.v4Metrics())))
            .clientIdentifier(asTextOrNull(json.get(ConnectionLogField.CLIENT_IDENTIFIER.v4Metrics())))
            .transactionId(asTextOrNull(json.get(ConnectionLogField.TRANSACTION_ID.v4Metrics())))
            .method(HttpMethod.get(asIntOr(json.get(ConnectionLogField.HTTP_METHOD.v4Metrics()), 0)))
            .requestEnded(asBooleanOrFalse(json.get(ConnectionLogField.REQUEST_ENDED.v4Metrics())))
            .entrypointId(asTextOrNull(json.get(ConnectionLogField.ENTRYPOINT_ID.v4Metrics())))
            .gatewayResponseTime(json.get(ConnectionLogField.GATEWAY_RESPONSE_TIME.v4Metrics()).asLong(0L))
            .build();
    }
}
