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
package io.gravitee.gateway.reactive.handlers.api.v4;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.AbstractApi;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.definition.model.v4.resource.Resource;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AgentApiSynthesizerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    // language=JSON
    private static final String AGENT = """
        {
          "definitionVersion": "V4", "type": "agent", "id": "researcher", "name": "Researcher",
          "apiVersion": "1.0.0", "kind": "standalone", "composable": true,
          "listeners": [
            { "type": "http", "paths": [ { "path": "/r/" } ],
              "entrypoints": [ { "type": "agent-a2a", "configuration": {} } ] }
          ],
          "standalone": {
            "model": { "type": "openai", "configuration": {} },
            "instructions": "Hi {{q}}", "inputs": [ { "name": "q" } ], "output": "a"
          },
          "plans": [ { "id": "keyless", "name": "Keyless", "security": { "type": "KEY_LESS", "configuration": {} },
                      "status": "published", "mode": "standard" } ]
        }
        """;

    // language=JSON — a composable agent that ALSO declares an HTTP listener but has NO plan (composition-only).
    private static final String COMPOSITION_ONLY_AGENT = """
        {
          "definitionVersion": "V4", "type": "agent", "id": "subagent", "name": "Sub Agent",
          "apiVersion": "1.0.0", "kind": "standalone", "composable": true,
          "listeners": [
            { "type": "http", "paths": [ { "path": "/s/" } ],
              "entrypoints": [ { "type": "agent-a2a", "configuration": {} } ] }
          ],
          "standalone": {
            "model": { "type": "openai", "configuration": {} },
            "instructions": "Hi {{q}}", "inputs": [ { "name": "q" } ], "output": "a"
          }
        }
        """;

    // language=JSON — a standalone agent declaring tools (2 of type tool-http, 1 of type tool-mcp), an inline skill,
    // and a referenced memory store (api-level resource).
    private static final String CAPABILITIES_AGENT = """
        {
          "definitionVersion": "V4", "type": "agent", "id": "cap", "name": "Cap",
          "apiVersion": "1.0.0", "kind": "standalone",
          "listeners": [
            { "type": "http", "paths": [ { "path": "/c/" } ],
              "entrypoints": [ { "type": "agent-http", "configuration": {} } ] }
          ],
          "resources": [ { "name": "mem", "type": "chat-memory-store-inmemory", "configuration": {} } ],
          "standalone": {
            "model": { "type": "openai", "configuration": {} }, "instructions": "do", "output": "a",
            "tools": [
              { "name": "search", "type": "tool-http", "configuration": { "url": "https://x" } },
              { "name": "fetch", "type": "tool-http", "configuration": { "url": "https://y" } },
              { "name": "mcp", "type": "tool-mcp", "configuration": {} }
            ],
            "skills": [ { "name": "weather", "type": "skill-inline", "configuration": { "content": "c" } } ],
            "workingMemory": { "ref": "mem", "configuration": {} }
          },
          "plans": [ { "id": "keyless", "name": "Keyless", "security": { "type": "key-less", "configuration": {} },
                      "status": "published", "mode": "standard" } ]
        }
        """;

    private io.gravitee.definition.model.v4.agent.AgentApi agent() throws Exception {
        return (io.gravitee.definition.model.v4.agent.AgentApi) mapper.readValue(AGENT, AbstractApi.class);
    }

    @Test
    void synthesize_yields_a_proxy_shell_the_pipeline_accepts() throws Exception {
        io.gravitee.definition.model.v4.Api proxy = AgentApiSynthesizer.synthesize(agent());

        assertThat(proxy.getId()).isEqualTo("researcher");
        assertThat(proxy.getType()).isEqualTo(ApiType.AGENT);
        assertThat(proxy.getDefinitionVersion()).isEqualTo(DefinitionVersion.V4);
        // only the HTTP listener feeds the pipeline
        assertThat(proxy.getListeners()).singleElement().isInstanceOf(HttpListener.class);
        // non-empty endpoint groups so DefaultEndpointManager does not leave a null default group
        assertThat(proxy.getEndpointGroups())
            .singleElement()
            .satisfies(g -> {
                assertThat(g.getName()).isEqualTo(AgentApiSynthesizer.SYNTHETIC_ENDPOINT_GROUP);
                assertThat(g.getEndpoints()).isEmpty();
            });
        // plans carried through (agent reuses Gravitee Plan)
        assertThat(proxy.getPlans())
            .singleElement()
            .satisfies(p -> assertThat(p.getSecurity().getType()).isEqualTo("KEY_LESS"));
    }

    @Test
    void standalone_capabilities_translate_to_endpoint_groups_and_resources() throws Exception {
        io.gravitee.definition.model.v4.agent.AgentApi clean = (io.gravitee.definition.model.v4.agent.AgentApi) mapper.readValue(
            CAPABILITIES_AGENT,
            AbstractApi.class
        );

        io.gravitee.definition.model.v4.Api proxy = AgentApiSynthesizer.synthesize(clean);

        // tools → one endpoint group per distinct type (group type = the tool plugin id)
        assertThat(proxy.getEndpointGroups()).hasSize(2);
        EndpointGroup httpGroup = group(proxy.getEndpointGroups(), "tool-http");
        assertThat(httpGroup.getEndpoints())
            .extracting(e -> e.getName())
            .containsExactlyInAnyOrder("search", "fetch");
        assertThat(httpGroup.getEndpoints()).allSatisfy(e -> {
            assertThat(e.getType()).isEqualTo("tool-http");
            assertThat(e.getConfiguration()).isNotBlank();
        });
        EndpointGroup mcpGroup = group(proxy.getEndpointGroups(), "tool-mcp");
        assertThat(mcpGroup.getEndpoints())
            .singleElement()
            .satisfies(e -> assertThat(e.getName()).isEqualTo("mcp"));

        // skills → synthetic resources appended next to the api-level memory resource
        assertThat(proxy.getResources())
            .extracting(Resource::getType)
            .containsExactlyInAnyOrder("chat-memory-store-inmemory", "skill-inline");
    }

    private static EndpointGroup group(List<EndpointGroup> groups, String type) {
        return groups
            .stream()
            .filter(g -> type.equals(g.getType()))
            .findFirst()
            .orElseThrow();
    }

    @Test
    void composition_only_agent_is_deployable_but_never_http_exposed() throws Exception {
        io.gravitee.definition.model.v4.agent.AgentApi clean = (io.gravitee.definition.model.v4.agent.AgentApi) mapper.readValue(
            COMPOSITION_ONLY_AGENT,
            AbstractApi.class
        );

        io.gravitee.definition.model.v4.Api proxy = AgentApiSynthesizer.synthesize(clean);

        // No published plan + composable → HTTP listeners stripped, so canCreate is false → never served.
        assertThat(proxy.getListeners()).isEmpty();
        // A synthetic published keyless plan is attached so the agent clears the deploy gate (registers for composition).
        assertThat(proxy.getPlans())
            .singleElement()
            .satisfies(p -> {
                assertThat(p.getSecurity().getType()).isEqualTo("key-less");
                assertThat(p.getStatus()).isEqualTo(PlanStatus.PUBLISHED);
            });
    }

    @Test
    void carrying_reactable_exposes_synthetic_definition_and_clean_agent() throws Exception {
        io.gravitee.definition.model.v4.agent.AgentApi clean = agent();
        AgentApi reactable = new AgentApi(clean);

        // getDefinition() is the synthetic proxy Api (what the reused pipeline reads)
        assertThat(reactable.getDefinition()).isInstanceOf(io.gravitee.definition.model.v4.Api.class);
        assertThat(reactable.getDefinition().getType()).isEqualTo(ApiType.AGENT);
        assertThat(reactable.getId()).isEqualTo("researcher");
        // the clean agent definition is carried for the agent runtime (H2)
        assertThat(reactable.getAgentDefinition()).isSameAs(clean);
    }
}
