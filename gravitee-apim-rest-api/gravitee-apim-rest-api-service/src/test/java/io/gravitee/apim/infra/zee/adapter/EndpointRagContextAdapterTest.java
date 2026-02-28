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
package io.gravitee.apim.infra.zee.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.plugin.model.ConnectorPlugin;
import io.gravitee.apim.core.plugin.query_service.EndpointPluginQueryService;
import io.gravitee.apim.core.zee.model.ZeeResourceType;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class EndpointRagContextAdapterTest {

    private EndpointPluginQueryService endpointPluginQueryService;
    private EndpointRagContextAdapter adapter;

    @BeforeEach
    void setUp() {
        endpointPluginQueryService = mock(EndpointPluginQueryService.class);
        adapter = new EndpointRagContextAdapter(endpointPluginQueryService);
    }

    @Test
    void resource_type_is_endpoint() {
        assertThat(adapter.resourceType()).isEqualTo(ZeeResourceType.ENDPOINT);
    }

    @Nested
    class RetrieveContext {

        @Test
        void returns_available_endpoints_when_present() {
            when(endpointPluginQueryService.findAll()).thenReturn(
                    Set.of(
                            connectorPlugin("http-proxy", "HTTP Proxy connector"),
                            connectorPlugin("kafka", "Apache Kafka connector")));

            var context = adapter.retrieveContext("env-1", "org-1", Map.of());

            assertThat(context).contains("### Available Endpoint Connectors");
            assertThat(context).contains("http-proxy: HTTP Proxy connector");
            assertThat(context).contains("kafka: Apache Kafka connector");
        }

        @Test
        void returns_empty_when_no_endpoints() {
            when(endpointPluginQueryService.findAll()).thenReturn(Set.of());

            var context = adapter.retrieveContext("env-1", "org-1", Map.of());

            assertThat(context).isEmpty();
        }

        @Test
        void limits_endpoints_to_max_twenty() {
            var plugins = IntStream.range(0, 30)
                    .mapToObj(i -> connectorPlugin("endpoint-" + i, "Description " + i))
                    .collect(Collectors.toSet());
            when(endpointPluginQueryService.findAll()).thenReturn(plugins);

            var context = adapter.retrieveContext("env-1", "org-1", Map.of());

            long lines = context
                    .lines()
                    .filter(line -> line.startsWith("- endpoint-"))
                    .count();
            assertThat(lines).isEqualTo(EndpointRagContextAdapter.MAX_ENDPOINTS);
        }

        @Test
        void degrades_gracefully_when_service_throws() {
            when(endpointPluginQueryService.findAll()).thenThrow(new RuntimeException("Plugin registry unavailable"));

            var context = adapter.retrieveContext("env-1", "org-1", Map.of());

            assertThat(context).isEmpty();
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static ConnectorPlugin connectorPlugin(String id, String description) {
        return ConnectorPlugin.builder().id(id).description(description).build();
    }
}
