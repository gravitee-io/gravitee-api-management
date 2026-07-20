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

@SuppressWarnings("deprecation")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class EdgeProxyDefinitionTest {

    // Mirror the production GraviteeMapper's NON_NULL inclusion so serialization assertions match at-rest output.
    private final ObjectMapper objectMapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

    @Test
    void should_deserialize_legacy_payload_without_apps() throws Exception {
        // Given
        var json = """
            {
              "dnsDomains": [{ "name": "api.anthropic.com" }],
              "routes": [{ "pathPrefix": "/v1/messages", "apiPath": "/anthropic", "provider": "anthropic" }]
            }""";

        // When
        var definition = objectMapper.readValue(json, EdgeProxyDefinition.class);

        // Then
        assertThat(definition.getDnsDomains()).containsExactly(new DnsDomain("api.anthropic.com"));
        assertThat(definition.getRoutes()).containsExactly(new EdgeRoute("/v1/messages", "/anthropic", "anthropic"));
        assertThat(definition.getApps()).isNull();
    }

    @Test
    void should_deserialize_app_centric_payload_without_legacy_fields() throws Exception {
        // Given
        var json = """
            {
              "apps": [
                {
                  "name": "Claude Code",
                  "domains": [{ "name": "api.anthropic.com" }],
                  "routes": [{ "pathPrefix": "/v1/messages", "apiPath": "/anthropic" }],
                  "format": "anthropic-messages",
                  "vendor": "anthropic"
                }
              ]
            }""";

        // When
        var definition = objectMapper.readValue(json, EdgeProxyDefinition.class);

        // Then
        assertThat(definition.getDnsDomains()).isNull();
        assertThat(definition.getRoutes()).isNull();
        assertThat(definition.getApps()).hasSize(1);
        var app = definition.getApps().get(0);
        assertThat(app.name()).isEqualTo("Claude Code");
        assertThat(app.domains()).containsExactly(new DnsDomain("api.anthropic.com"));
        assertThat(app.routes()).containsExactly(new RouteMapping("/v1/messages", "/anthropic"));
        assertThat(app.format()).isEqualTo("anthropic-messages");
        assertThat(app.vendor()).isEqualTo("anthropic");
    }

    @Test
    void should_deserialize_payload_with_both_legacy_and_app_fields() throws Exception {
        // Given
        var json = """
            {
              "dnsDomains": [{ "name": "api.anthropic.com" }],
              "routes": [{ "pathPrefix": "/v1/messages", "apiPath": "/anthropic", "provider": "anthropic" }],
              "apps": [
                {
                  "name": "Claude Code",
                  "domains": [{ "name": "api.anthropic.com" }],
                  "routes": [{ "pathPrefix": "/v1/messages", "apiPath": "/anthropic" }],
                  "format": "anthropic-messages",
                  "vendor": "anthropic"
                }
              ]
            }""";

        // When
        var definition = objectMapper.readValue(json, EdgeProxyDefinition.class);

        // Then
        assertThat(definition.getDnsDomains()).containsExactly(new DnsDomain("api.anthropic.com"));
        assertThat(definition.getRoutes()).containsExactly(new EdgeRoute("/v1/messages", "/anthropic", "anthropic"));
        assertThat(definition.getApps()).hasSize(1);
        assertThat(definition.getApps().get(0).name()).isEqualTo("Claude Code");
    }

    @Test
    void should_omit_apps_from_serialized_legacy_definition() throws Exception {
        // Given
        var definition = EdgeProxyDefinition.builder()
            .dnsDomains(List.of(new DnsDomain("api.anthropic.com")))
            .routes(List.of(new EdgeRoute("/v1/messages", "/anthropic", "anthropic")))
            .build();

        // When
        var json = objectMapper.writeValueAsString(definition);

        // Then
        assertThat(json).doesNotContain("apps");
    }
}
