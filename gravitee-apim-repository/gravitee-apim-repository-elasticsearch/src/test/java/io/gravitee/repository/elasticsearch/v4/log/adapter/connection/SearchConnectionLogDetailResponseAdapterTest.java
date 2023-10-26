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
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchConnectionLogDetailResponseAdapterTest {

    @Test
    void should_return_empty_when_no_hit() {
        assertThat(SearchConnectionLogDetailResponseAdapter.adapt(new SearchResponse())).isEmpty();
    }

    @Test
    void should_build_full_connection_log_detail() {
        final SearchResponse searchResponse = buildSearchHit();

        final ConnectionLogDetail connectionLogDetail = SearchConnectionLogDetailResponseAdapter.adapt(searchResponse).get();

        final ConnectionLogDetail build = ConnectionLogDetail
            .builder()
            .timestamp("2023-10-27T07:41:39.317+02:00")
            .apiId("4c3e775d-eeb5-4d6c-be77-5deeb5ed6c41")
            .requestId("e220afa7-4c77-4280-a0af-a74c7782801c")
            .clientIdentifier("8eec8b53-edae-4954-ac8b-53edae1954e4")
            .requestEnded(true)
            .entrypointRequest(
                ConnectionLogDetail.Request
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
                    .build()
            )
            .endpointRequest(
                ConnectionLogDetail.Request
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
                    .build()
            )
            .entrypointResponse(
                ConnectionLogDetail.Response
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
                    .build()
            )
            .endpointResponse(ConnectionLogDetail.Response.builder().status(200).headers(Map.of()).build())
            .build();

        connectionLogDetail.equals(build);
        assertThat(connectionLogDetail).isEqualTo(build);

        assertThat(SearchConnectionLogDetailResponseAdapter.adapt(searchResponse)).contains(build);
    }

    @SneakyThrows
    @NotNull
    private static SearchResponse buildSearchHit() {
        final SearchResponse searchResponse = new SearchResponse();
        final SearchHits searchHits = new SearchHits();
        final SearchHit searchHit = new SearchHit();

        final ObjectMapper objectMapper = new ObjectMapper();
        final JsonNode jsonNode = objectMapper.readTree(searchHitString());

        searchHit.setSource(jsonNode);
        searchHits.setHits(List.of(searchHit));
        searchResponse.setSearchHits(searchHits);
        return searchResponse;
    }

    private static String searchHitString() {
        return """
               {
               		"@timestamp": "2023-10-27T07:41:39.317+02:00",
               		"api-id": "4c3e775d-eeb5-4d6c-be77-5deeb5ed6c41",
               		"request-id": "e220afa7-4c77-4280-a0af-a74c7782801c",
               		"client-identifier": "8eec8b53-edae-4954-ac8b-53edae1954e4",
               		"request-ended": "true",
               		"entrypoint-request": {
               			"method": "GET",
               			"uri": "/test?param=paramValue",
               			"headers": {
               				"Accept-Encoding": [
               					"gzip, deflate, br"
               				],
               				"Host": [
               					"localhost:8082"
               				],
               				"X-Gravitee-Request-Id": [
               					"e220afa7-4c77-4280-a0af-a74c7782801c"
               				],
               				"X-Gravitee-Transaction-Id": [
               					"e220afa7-4c77-4280-a0af-a74c7782801c"
               				]
               			}
               		},
               		"entrypoint-response": {
               			"status": 200,
               			"headers": {
               				"Content-Type": [
               					"text/plain"
               				],
               				"X-Gravitee-Client-Identifier": [
               					"8eec8b53-edae-4954-ac8b-53edae1954e4"
               				],
               				"X-Gravitee-Request-Id": [
               					"e220afa7-4c77-4280-a0af-a74c7782801c"
               				],
               				"X-Gravitee-Transaction-Id": [
               					"e220afa7-4c77-4280-a0af-a74c7782801c"
               				]
               			}
               		},
               		"endpoint-request": {
               			"method": "GET",
               			"uri": "",
               			"headers": {
               				"Accept-Encoding": [
               					"gzip, deflate, br"
               				],
               				"Host": [
               					"localhost:8082"
               				],
               				"X-Gravitee-Request-Id": [
               					"e220afa7-4c77-4280-a0af-a74c7782801c"
               				],
               				"X-Gravitee-Transaction-Id": [
               					"e220afa7-4c77-4280-a0af-a74c7782801c"
               				]
               			}
               		},
               		"endpoint-response": {
               			"status": 200,
               			"headers": {}
               		}
               	}
               """;
    }
}
