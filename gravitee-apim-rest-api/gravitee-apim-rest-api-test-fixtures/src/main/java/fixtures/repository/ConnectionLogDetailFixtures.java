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
package fixtures.repository;

import io.gravitee.rest.api.model.v4.log.connection.ConnectionLogDetail;
import java.util.List;
import java.util.Map;

public class ConnectionLogDetailFixtures {

    public static final String X_HEADER = "X-Header";
    public static final String HEADER_VALUE = "header-value";
    private static final ConnectionLogDetail.ConnectionLogDetailBuilder<?, ?> BASE = ConnectionLogDetail.builder()
        .apiId("api-id")
        .requestId("request-id")
        .clientIdentifier("client-identifier")
        .requestEnded(true)
        .timestamp("2020-02-01T20:00:00.00Z")
        .entrypointRequest(
            ConnectionLogDetail.Request.builder().method("GET").uri("/uri").headers(Map.of(X_HEADER, List.of(HEADER_VALUE))).build()
        )
        .endpointRequest(
            ConnectionLogDetail.Request.builder().method("GET").uri("/uri").headers(Map.of(X_HEADER, List.of(HEADER_VALUE))).build()
        )
        .entrypointResponse(ConnectionLogDetail.Response.builder().status(200).headers(Map.of(X_HEADER, List.of(HEADER_VALUE))).build())
        .endpointResponse(ConnectionLogDetail.Response.builder().status(200).headers(Map.of()).build());

    public ConnectionLogDetailFixtures(String defaultApiId, String requestId) {
        BASE.apiId(defaultApiId).requestId(requestId);
    }

    public ConnectionLogDetail aConnectionLogDetail() {
        return BASE.build();
    }

    public ConnectionLogDetail aConnectionLogDetail(String requestId) {
        return BASE.requestId(requestId).build();
    }
}
