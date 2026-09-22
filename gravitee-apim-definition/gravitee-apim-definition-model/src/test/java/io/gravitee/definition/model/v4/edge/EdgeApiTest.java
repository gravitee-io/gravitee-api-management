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
package io.gravitee.definition.model.v4.edge;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class EdgeApiTest {

    private final ObjectMapper objectMapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

    @Test
    void should_round_trip_a_route_carrying_its_api_plan_and_subscription() throws Exception {
        // Given
        var route = new RouteMapping("/v1/messages", "/anthropic", "api-id", "plan-id", "subscription-id");
        var edgeApi = edgeApiWith(route);

        // When
        var json = objectMapper.writeValueAsString(edgeApi);
        var read = objectMapper.readValue(json, EdgeApi.class);

        // Then
        assertThat(json).contains("\"apiId\":\"api-id\"", "\"planId\":\"plan-id\"", "\"subscriptionId\":\"subscription-id\"");
        assertThat(read.getProxy().getApps().get(0).routes()).containsExactly(route);
    }

    @Test
    void should_round_trip_a_route_without_api_plan_nor_subscription() throws Exception {
        // Given
        var edgeApi = edgeApiWith(new RouteMapping("/v1/messages", "/anthropic"));

        // When
        var json = objectMapper.writeValueAsString(edgeApi);
        var read = objectMapper.readValue(json, EdgeApi.class);

        // Then
        assertThat(json).doesNotContain("apiId", "planId", "subscriptionId");
        assertThat(read.getProxy().getApps().get(0).routes()).containsExactly(
            new RouteMapping("/v1/messages", "/anthropic", null, null, null)
        );
    }

    @Test
    void should_deserialize_a_definition_stored_before_routes_carried_api_plan_and_subscription() throws Exception {
        // Given
        var json = """
            {
              "id": "edge-api",
              "name": "Edge",
              "type": "edge",
              "proxy": {
                "apps": [
                  {
                    "name": "Claude Code",
                    "routes": [{ "path": "/v1/messages", "apiPath": "/anthropic" }]
                  }
                ]
              }
            }""";

        // When
        var edgeApi = objectMapper.readValue(json, EdgeApi.class);

        // Then
        var route = edgeApi.getProxy().getApps().get(0).routes().get(0);
        assertThat(route.apiId()).isNull();
        assertThat(route.planId()).isNull();
        assertThat(route.subscriptionId()).isNull();
    }

    private static EdgeApi edgeApiWith(RouteMapping route) {
        return EdgeApi.builder()
            .id("edge-api")
            .name("Edge")
            .proxy(
                EdgeProxyDefinition.builder()
                    .apps(List.of(new EdgeApp("Claude Code", null, List.of(route), "anthropic-messages", "anthropic")))
                    .build()
            )
            .build();
    }
}
