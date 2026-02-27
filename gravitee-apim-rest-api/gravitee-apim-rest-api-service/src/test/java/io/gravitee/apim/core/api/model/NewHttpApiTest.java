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
package io.gravitee.apim.core.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.NewApiFixtures;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NewHttpApiTest {

    @Test
    void should_not_set_allowedInApiProducts_by_default_for_v4_proxy_http_api() {
        NewHttpApi newHttpApi = NewApiFixtures.aProxyApiV4();

        var definition = newHttpApi.toApiDefinitionBuilder().build();

        assertThat(definition.getDefinitionVersion()).isEqualTo(DefinitionVersion.V4);
        assertThat(definition.getType()).isEqualTo(ApiType.PROXY);
        assertThat(definition.getAllowedInApiProducts()).isNull();
    }

    @Test
    void should_not_set_allowedInApiProducts_for_non_proxy_http_api_types() {
        // MCP proxy
        NewHttpApi mcpProxy = NewApiFixtures.aMcpProxyApiV4();
        var mcpDefinition = mcpProxy.toApiDefinitionBuilder().build();
        assertThat(mcpDefinition.getDefinitionVersion()).isEqualTo(DefinitionVersion.V4);
        assertThat(mcpDefinition.getType()).isEqualTo(ApiType.MCP_PROXY);
        assertThat(mcpDefinition.getAllowedInApiProducts()).isNull();

        // LLM proxy
        NewHttpApi llmProxy = NewApiFixtures.aLlmProxyApiV4();
        var llmDefinition = llmProxy.toApiDefinitionBuilder().build();
        assertThat(llmDefinition.getDefinitionVersion()).isEqualTo(DefinitionVersion.V4);
        assertThat(llmDefinition.getType()).isEqualTo(ApiType.LLM_PROXY);
        assertThat(llmDefinition.getAllowedInApiProducts()).isNull();
    }

    @Test
    void should_not_set_allowedInApiProducts_when_definition_version_is_not_v4() {
        // Given a PROXY HTTP API but not V4
        NewHttpApi httpApiV2 = NewApiFixtures.aProxyApiV4().toBuilder().definitionVersion(DefinitionVersion.V2).build();

        var definition = httpApiV2.toApiDefinitionBuilder().build();
        assertThat(definition.getDefinitionVersion()).isEqualTo(DefinitionVersion.V2);
        assertThat(definition.getType()).isEqualTo(ApiType.PROXY);
        assertThat(definition.getAllowedInApiProducts()).isNull();
    }
}
