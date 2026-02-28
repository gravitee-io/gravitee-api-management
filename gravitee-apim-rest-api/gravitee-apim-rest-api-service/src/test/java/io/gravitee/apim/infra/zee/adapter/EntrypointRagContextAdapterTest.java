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
import io.gravitee.apim.core.plugin.query_service.EntrypointPluginQueryService;
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
class EntrypointRagContextAdapterTest {

    private EntrypointPluginQueryService entrypointPluginQueryService;
    private EntrypointRagContextAdapter adapter;

    @BeforeEach
    void setUp() {
        entrypointPluginQueryService = mock(EntrypointPluginQueryService.class);
        adapter = new EntrypointRagContextAdapter(entrypointPluginQueryService);
    }

    @Test
    void resource_type_is_entrypoint() {
        assertThat(adapter.resourceType()).isEqualTo(ZeeResourceType.ENTRYPOINT);
    }

    @Nested
    class RetrieveContext {

        @Test
        void returns_available_entrypoints_when_present() {
            when(entrypointPluginQueryService.findAll()).thenReturn(
                    Set.of(
                            connectorPlugin("http-proxy", "HTTP Proxy entrypoint"),
                            connectorPlugin("websocket", "WebSocket entrypoint")));

            var context = adapter.retrieveContext("env-1", "org-1", Map.of());

            assertThat(context).contains("### Available Entrypoint Connectors");
            assertThat(context).contains("http-proxy: HTTP Proxy entrypoint");
            assertThat(context).contains("websocket: WebSocket entrypoint");
        }

        @Test
        void returns_empty_when_no_entrypoints() {
            when(entrypointPluginQueryService.findAll()).thenReturn(Set.of());

            var context = adapter.retrieveContext("env-1", "org-1", Map.of());

            assertThat(context).isEmpty();
        }

        @Test
        void limits_entrypoints_to_max_twenty() {
            var plugins = IntStream.range(0, 30)
                    .mapToObj(i -> connectorPlugin("entrypoint-" + i, "Description " + i))
                    .collect(Collectors.toSet());
            when(entrypointPluginQueryService.findAll()).thenReturn(plugins);

            var context = adapter.retrieveContext("env-1", "org-1", Map.of());

            long lines = context
                    .lines()
                    .filter(line -> line.startsWith("- entrypoint-"))
                    .count();
            assertThat(lines).isEqualTo(EntrypointRagContextAdapter.MAX_ENTRYPOINTS);
        }

        @Test
        void degrades_gracefully_when_service_throws() {
            when(entrypointPluginQueryService.findAll()).thenThrow(new RuntimeException("Plugin registry unavailable"));

            var context = adapter.retrieveContext("env-1", "org-1", Map.of());

            assertThat(context).isEmpty();
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static ConnectorPlugin connectorPlugin(String id, String description) {
        return ConnectorPlugin.builder().id(id).description(description).build();
    }
}
