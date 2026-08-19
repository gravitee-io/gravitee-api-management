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
package io.gravitee.apim.rest.api.automation.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.gravitee.apim.plugin.gamma.api.automation.GammaAutomationPort;
import io.gravitee.apim.rest.api.automation.resource.base.AbstractResourceTest;
import io.gravitee.apim.rest.api.automation.spring.GammaAutomationPorts;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
class OpenAPIResourceTest extends AbstractResourceTest {

    private static final YAMLMapper YAML = new YAMLMapper();
    private static final String OPEN_API_VERSION = "openapi";
    private static final String APIM_PATH = "/organizations/{orgId}/environments/{envId}/apis";
    private static final String MODULE_PATH = "/organizations/{orgId}/environments/{envId}/aim/catalog/mcp-servers";

    private static final String AIM_FRAGMENT = """
        openapi: 3.1.0
        info:
          title: aim automation
          version: 1.0.0
        tags:
          - name: AI Catalog
        paths:
          /organizations/{orgId}/environments/{envId}/aim/catalog/mcp-servers:
            put:
              operationId: createOrUpdateAimCatalogMcpServer
              responses:
                "200":
                  description: ok
        components:
          schemas:
            AimCatalogMcpServerSpec:
              type: object
              properties:
                hrid:
                  type: string
        """;

    private static final String COLLIDING_FRAGMENT = """
        openapi: 3.1.0
        info:
          title: broken
          version: 1.0.0
        paths: {}
        components:
          schemas:
            BaseStatus:
              type: object
        """;

    @Inject
    private GammaAutomationPorts gammaAutomationPorts;

    @Override
    protected String contextPath() {
        return "/open-api.yaml";
    }

    @AfterEach
    void tearDownPorts() {
        reset(gammaAutomationPorts);
    }

    @Test
    void should_get_spec() {
        assertThat(expectSpec().get(OPEN_API_VERSION).asText()).isNotEmpty();
    }

    @Nested
    class ModuleFragments {

        @Test
        void should_serve_apim_document_unchanged_when_no_module_publishes_a_fragment() {
            var port = portWithFragment("aim", null);
            when(gammaAutomationPorts.all()).thenReturn(List.of(port));

            var spec = expectSpec();

            assertThat(spec.get("paths").has(APIM_PATH)).isTrue();
            assertThat(spec.get("paths").has(MODULE_PATH)).isFalse();
        }

        @Test
        void should_merge_module_paths_components_and_tags() {
            var port = portWithFragment("aim", AIM_FRAGMENT);
            when(gammaAutomationPorts.all()).thenReturn(List.of(port));

            var spec = expectSpec();

            assertThat(spec.get("paths").has(APIM_PATH)).isTrue();
            assertThat(spec.get("paths").get(MODULE_PATH).get("put").get("operationId").asText()).isEqualTo(
                "createOrUpdateAimCatalogMcpServer"
            );
            assertThat(spec.get("components").get("schemas").has("AimCatalogMcpServerSpec")).isTrue();
            assertThat(spec.get("components").get("schemas").has("BaseStatus")).isTrue();
            assertThat(spec.get("tags"))
                .extracting(tag -> tag.get("name").asText())
                .contains("APIs", "AI Catalog");
            assertThat(spec.get("info").get("title").asText()).isEqualTo("Gravitee.io - Automation API");
        }

        @Test
        void should_fail_loudly_when_a_fragment_collides_with_an_existing_component() {
            var port = portWithFragment("broken", COLLIDING_FRAGMENT);
            when(gammaAutomationPorts.all()).thenReturn(List.of(port));

            try (var response = rootTarget().request().get()) {
                assertThat(response.getStatus()).isEqualTo(500);
            }
        }

        private GammaAutomationPort portWithFragment(String module, String fragment) {
            var port = mock(GammaAutomationPort.class);
            when(port.module()).thenReturn(module);
            when(port.openApiFragment()).thenReturn(Optional.ofNullable(fragment));
            return port;
        }
    }

    private JsonNode expectSpec() {
        try (var response = rootTarget().request().get()) {
            assertThat(response.getStatus()).isEqualTo(200);
            return readYAML(response.readEntity(String.class));
        }
    }

    private JsonNode readYAML(String yaml) {
        try {
            return YAML.readTree(yaml);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
