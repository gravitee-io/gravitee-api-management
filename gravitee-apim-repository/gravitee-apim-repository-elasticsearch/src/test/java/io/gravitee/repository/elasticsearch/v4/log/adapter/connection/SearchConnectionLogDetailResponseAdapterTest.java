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

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.elasticsearch.model.SearchHit;
import io.gravitee.elasticsearch.model.SearchHits;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.log.v4.model.connection.ConnectionLogDetail;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchConnectionLogDetailResponseAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void should_return_empty_when_no_hit() {
        assertThat(SearchConnectionLogDetailResponseAdapter.adapt(new SearchResponse())).isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("generate")
    void should_build_connection_log_detail(String hit, ConnectionLogDetail expected) {
        final SearchResponse searchResponse = buildSearchHit(hit);

        final ConnectionLogDetail connectionLogDetail = SearchConnectionLogDetailResponseAdapter.adapt(searchResponse).get();

        assertThat(connectionLogDetail).isEqualTo(expected);
    }

    private static Stream<Arguments> generate() {
        ConnectionLogDetail.Response entrypointResponse = ConnectionLogDetail.Response
            .builder()
            .status(200)
            .headers(
                Map.of(
                    "Content-Type",
                    List.of("text/plain"),
                    "X-Gravitee-Client-Identifier",
                    List.of("8eec8b53-edae-4954-ac8b-53edae1954e4"),
                    "X-Gravitee-Request-Id",
                    List.of("e220afa7-4c77-4280-a0af-a74c7782801c"),
                    "X-Gravitee-Transaction-Id",
                    List.of("e220afa7-4c77-4280-a0af-a74c7782801c")
                )
            )
            .build();

        ConnectionLogDetail.Request endpointRequest = ConnectionLogDetail.Request
            .builder()
            .method("GET")
            .uri("")
            .headers(
                Map.of(
                    "Accept-Encoding",
                    List.of("gzip, deflate, br"),
                    "Host",
                    List.of("localhost:8082"),
                    "X-Gravitee-Request-Id",
                    List.of("e220afa7-4c77-4280-a0af-a74c7782801c"),
                    "X-Gravitee-Transaction-Id",
                    List.of("e220afa7-4c77-4280-a0af-a74c7782801c")
                )
            )
            .build();

        ConnectionLogDetail.Request entrypointRequest = ConnectionLogDetail.Request
            .builder()
            .method("GET")
            .uri("/test?param=paramValue")
            .headers(
                Map.of(
                    "Accept-Encoding",
                    List.of("gzip, deflate, br"),
                    "Host",
                    List.of("localhost:8082"),
                    "X-Gravitee-Transaction-Id",
                    List.of("e220afa7-4c77-4280-a0af-a74c7782801c"),
                    "X-Gravitee-Request-Id",
                    List.of("e220afa7-4c77-4280-a0af-a74c7782801c")
                )
            )
            .build();

        ConnectionLogDetail.Response endpointResponse = ConnectionLogDetail.Response.builder().status(200).headers(Map.of()).build();

        return Stream.of(
            Arguments.of(
                "connection-log-detail.json",
                ConnectionLogDetail
                    .builder()
                    .timestamp("2023-10-27T07:41:39.317+02:00")
                    .apiId("4c3e775d-eeb5-4d6c-be77-5deeb5ed6c41")
                    .requestId("e220afa7-4c77-4280-a0af-a74c7782801c")
                    .clientIdentifier("8eec8b53-edae-4954-ac8b-53edae1954e4")
                    .requestEnded(true)
                    .entrypointRequest(entrypointRequest)
                    .entrypointResponse(entrypointResponse)
                    .endpointRequest(endpointRequest)
                    .endpointResponse(endpointResponse)
                    .build()
            ),
            Arguments.of(
                "connection-log-detail-endpoint-only.json",
                ConnectionLogDetail
                    .builder()
                    .timestamp("2023-10-27T07:41:39.317+02:00")
                    .apiId("4c3e775d-eeb5-4d6c-be77-5deeb5ed6c41")
                    .requestId("e220afa7-4c77-4280-a0af-a74c7782801c")
                    .clientIdentifier("8eec8b53-edae-4954-ac8b-53edae1954e4")
                    .requestEnded(true)
                    .endpointRequest(endpointRequest)
                    .endpointResponse(endpointResponse)
                    .build()
            ),
            Arguments.of(
                "connection-log-detail-entrypoint-only.json",
                ConnectionLogDetail
                    .builder()
                    .timestamp("2023-10-27T07:41:39.317+02:00")
                    .apiId("4c3e775d-eeb5-4d6c-be77-5deeb5ed6c41")
                    .requestId("e220afa7-4c77-4280-a0af-a74c7782801c")
                    .clientIdentifier("8eec8b53-edae-4954-ac8b-53edae1954e4")
                    .requestEnded(true)
                    .entrypointRequest(entrypointRequest)
                    .entrypointResponse(entrypointResponse)
                    .build()
            ),
            Arguments.of(
                "connection-log-detail-response-only.json",
                ConnectionLogDetail
                    .builder()
                    .timestamp("2023-10-27T07:41:39.317+02:00")
                    .apiId("4c3e775d-eeb5-4d6c-be77-5deeb5ed6c41")
                    .requestId("e220afa7-4c77-4280-a0af-a74c7782801c")
                    .clientIdentifier("8eec8b53-edae-4954-ac8b-53edae1954e4")
                    .requestEnded(true)
                    .entrypointResponse(entrypointResponse)
                    .endpointResponse(endpointResponse)
                    .build()
            ),
            Arguments.of(
                "connection-log-detail-request-only.json",
                ConnectionLogDetail
                    .builder()
                    .timestamp("2023-10-27T07:41:39.317+02:00")
                    .apiId("4c3e775d-eeb5-4d6c-be77-5deeb5ed6c41")
                    .requestId("e220afa7-4c77-4280-a0af-a74c7782801c")
                    .clientIdentifier("8eec8b53-edae-4954-ac8b-53edae1954e4")
                    .requestEnded(true)
                    .entrypointRequest(entrypointRequest)
                    .endpointRequest(endpointRequest)
                    .build()
            )
        );
    }

    @SneakyThrows
    @NotNull
    private SearchResponse buildSearchHit(String fileName) {
        final SearchResponse searchResponse = new SearchResponse();
        final SearchHits searchHits = new SearchHits();
        final SearchHit searchHit = new SearchHit();

        final ObjectMapper objectMapper = new ObjectMapper();
        final JsonNode jsonNode = objectMapper.readTree(loadFile("/hits/" + fileName));

        searchHit.setSource(jsonNode);
        searchHits.setHits(List.of(searchHit));
        searchResponse.setSearchHits(searchHits);
        return searchResponse;
    }

    private String loadFile(String resource) throws IOException {
        InputStream stream = this.getClass().getResourceAsStream(resource);
        JsonNode json = objectMapper.readValue(stream, JsonNode.class);
        return objectMapper.writeValueAsString(json);
    }
}
