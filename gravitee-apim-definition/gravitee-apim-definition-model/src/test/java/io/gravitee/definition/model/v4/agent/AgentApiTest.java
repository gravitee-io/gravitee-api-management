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
package io.gravitee.definition.model.v4.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.Plugin;
import io.gravitee.definition.model.v4.AbstractApi;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.agent.workflow.A2aAgentItem;
import io.gravitee.definition.model.v4.agent.workflow.AgentRefItem;
import io.gravitee.definition.model.v4.agent.workflow.ConditionalItem;
import io.gravitee.definition.model.v4.agent.workflow.HumanItem;
import io.gravitee.definition.model.v4.agent.workflow.LoopItem;
import io.gravitee.definition.model.v4.agent.workflow.ParallelItem;
import io.gravitee.definition.model.v4.agent.workflow.SequenceItem;
import io.gravitee.definition.model.v4.agent.workflow.SupervisorItem;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AgentApiTest {

    private final ObjectMapper mapper = new ObjectMapper();

    // A standalone agent: exposed via A2A (HTTP) and composable (composable flag); references a memory resource.
    // language=JSON
    private static final String RESEARCHER = """
        {
          "definitionVersion": "V4", "type": "agent", "id": "researcher", "name": "Researcher",
          "apiVersion": "1.0.0", "kind": "standalone", "composable": true,
          "listeners": [
            { "type": "http",
              "paths": [ { "path": "/agm/researcher/", "overrideAccess": false } ],
              "requestValidation": { "rejectNullByte": true },
              "entrypoints": [ { "type": "agent-a2a", "configuration": {} } ] }
          ],
          "standalone": {
            "role": "Analyst.", "goal": "Notes.", "instructions": "Research {{topic}}.",
            "inputs": [ { "name": "topic" } ], "output": "notes",
            "model": { "type": "openai", "configuration": { "model": "gpt-5-mini" } },
            "tools":  [ { "name": "web-search", "type": "mcp", "configuration": { "url": "x" } } ],
            "skills": [ { "name": "cite", "type": "inline", "configuration": { "content": "Cite." } } ],
            "workingMemory": { "ref": "redis-memory-store", "configuration": { "maxMessages": 50 } }
          },
          "plans": [ { "id": "keyless", "name": "Keyless", "security": { "type": "KEY_LESS", "configuration": {} }, "status": "published", "mode": "standard" } ]
        }
        """;

    // A workflow/supervisor agent, composition-only (composable, no published plan); items reference deployed agents.
    // language=JSON
    private static final String WRITING_TEAM = """
        {
          "definitionVersion": "V4", "type": "agent", "id": "writing-team", "name": "Writing Team",
          "apiVersion": "1.0.0", "kind": "workflow", "composable": true,
          "listeners": [],
          "workflow": {
            "type": "supervisor",
            "model": { "type": "openai", "configuration": { "model": "gpt-5-mini" } },
            "goal": "Turn the notes into a reviewed report.",
            "instructions": "Draft a report from {{notes}} in a {{tone}} tone.",
            "inputs": [ { "name": "notes" }, { "name": "tone" } ], "output": "report",
            "items": [ { "type": "agent", "refId": "drafter" }, { "type": "agent", "refId": "reviewer" } ]
          },
          "plans": []
        }
        """;

    // A workflow/sequence agent, externally exposed; composes a ref, an external a2a-agent, a ref and a human gate.
    // language=JSON
    private static final String RESEARCH_WRITER = """
        {
          "definitionVersion": "V4", "type": "agent", "id": "rw", "name": "research-writer",
          "apiVersion": "1.0.0", "kind": "workflow",
          "listeners": [
            { "type": "http",
              "paths": [ { "path": "/agm/research-writer/", "overrideAccess": false } ],
              "entrypoints": [
                { "type": "agent-a2a", "configuration": {} },
                { "type": "agent-http", "qos": "none", "configuration": {} }
              ] }
          ],
          "workflow": {
            "type": "sequence",
            "inputs": [ { "name": "topic", "type": "string", "required": true }, { "name": "tone", "default": "neutral" } ],
            "output": "report",
            "items": [
              { "type": "agent", "refId": "researcher" },
              { "type": "a2a-agent", "name": "Web searcher", "configuration": { "url": "https://x" } },
              { "type": "agent", "refId": "writing-team" },
              { "type": "human", "name": "Review",
                "when": { "combine": "all", "clauses": [ { "variable": "report", "op": "isPresent" } ] },
                "ask": "Approve:\\n{{report}}", "inputs": [ { "name": "report" } ], "output": "report" }
            ]
          }
        }
        """;

    // Exercises every control (loop/parallel/conditional) + recursion (a sequence nested inside a conditional).
    // language=JSON
    private static final String CONTROLS = """
        {
          "definitionVersion": "V4", "type": "agent", "id": "controls", "name": "Controls",
          "apiVersion": "1.0.0", "kind": "workflow", "composable": true,
          "listeners": [],
          "workflow": {
            "type": "sequence", "output": "out",
            "items": [
              { "type": "loop", "max": 3, "testExitAtEnd": true,
                "until": { "combine": "all", "clauses": [ { "variable": "x", "op": "isPresent" } ] },
                "items": [ { "type": "agent", "refId": "a" } ] },
              { "type": "parallel", "items": [ { "type": "agent", "refId": "b" }, { "type": "agent", "refId": "c" } ] },
              { "type": "conditional", "items": [
                  { "type": "agent", "refId": "d", "when": { "combine": "any", "clauses": [ { "variable": "y", "op": "isTrue" } ] } },
                  { "type": "sequence", "items": [ { "type": "agent", "refId": "e" } ] }
              ] }
            ]
          }
        }
        """;

    @Test
    void should_deserialize_all_controls_and_nested_recursion() throws Exception {
        AgentApi api = (AgentApi) mapper.readValue(CONTROLS, AbstractApi.class);

        SequenceItem root = (SequenceItem) api.getWorkflow();
        assertThat(root.getItems()).hasSize(3);

        LoopItem loop = (LoopItem) root.getItems().get(0);
        assertThat(loop.getMax()).isEqualTo(3);
        assertThat(loop.getTestExitAtEnd()).isTrue();
        assertThat(loop.getUntil().getClauses().get(0).getOp()).isEqualTo("isPresent");
        assertThat(loop.getItems().get(0)).isInstanceOf(AgentRefItem.class);

        ParallelItem parallel = (ParallelItem) root.getItems().get(1);
        assertThat(parallel.getItems())
            .hasSize(2)
            .allSatisfy(it -> assertThat(it).isInstanceOf(AgentRefItem.class));

        ConditionalItem conditional = (ConditionalItem) root.getItems().get(2);
        assertThat(conditional.getItems().get(0)).isInstanceOf(AgentRefItem.class);
        assertThat(conditional.getItems().get(0).getWhen().getCombine()).isEqualTo("any");
        // recursion depth 3: sequence → conditional → sequence → agent
        assertThat(conditional.getItems().get(1)).isInstanceOf(SequenceItem.class);
        SequenceItem nested = (SequenceItem) conditional.getItems().get(1);
        assertThat(((AgentRefItem) nested.getItems().get(0)).getRefId()).isEqualTo("e");

        // round-trips cleanly
        AgentApi again = (AgentApi) mapper.readValue(mapper.writeValueAsString(api), AbstractApi.class);
        assertThat(again).isEqualTo(api);
    }

    @Test
    void should_deserialize_standalone_agent() throws Exception {
        AbstractApi api = mapper.readValue(RESEARCHER, AbstractApi.class);

        assertThat(api).isInstanceOf(AgentApi.class);
        assertThat(api.getType()).isEqualTo(ApiType.AGENT);
        AgentApi agent = (AgentApi) api;
        assertThat(agent.getKind()).isEqualTo("standalone");
        assertThat(agent.getWorkflow()).isNull();
        assertThat(agent.getStandalone()).isNotNull();
        assertThat(agent.getStandalone().getModel().getType()).isEqualTo("openai");
        assertThat(agent.getStandalone().getWorkingMemory().getRef()).isEqualTo("redis-memory-store");
        assertThat(agent.getStandalone().getInputs())
            .singleElement()
            .satisfies(i -> assertThat(i.getName()).isEqualTo("topic"));
        // composable (referenceable as a sub-agent) and HTTP-exposed
        assertThat(agent.isComposable()).isTrue();
        assertThat(agent.getListeners().get(0)).isInstanceOf(HttpListener.class);
        // a keyless plan (reusing the Gravitee Plan)
        assertThat(agent.getPlans())
            .singleElement()
            .satisfies(p -> assertThat(p.getSecurity().getType()).isEqualTo("KEY_LESS"));
    }

    @Test
    void should_deserialize_workflow_supervisor_with_referenced_items() throws Exception {
        AgentApi api = (AgentApi) mapper.readValue(WRITING_TEAM, AbstractApi.class);

        assertThat(api.getKind()).isEqualTo("workflow");
        assertThat(api.getStandalone()).isNull();
        assertThat(api.getWorkflow()).isInstanceOf(SupervisorItem.class);
        SupervisorItem supervisor = (SupervisorItem) api.getWorkflow();
        assertThat(supervisor.getModel().getType()).isEqualTo("openai");
        assertThat(supervisor.getItems())
            .hasSize(2)
            .allSatisfy(it -> assertThat(it).isInstanceOf(AgentRefItem.class));
        assertThat(((AgentRefItem) supervisor.getItems().get(0)).getRefId()).isEqualTo("drafter");
        assertThat(api.isComposable()).isTrue();
    }

    @Test
    void should_deserialize_workflow_sequence_with_ref_external_and_human_items() throws Exception {
        AgentApi api = (AgentApi) mapper.readValue(RESEARCH_WRITER, AbstractApi.class);

        assertThat(api.getWorkflow()).isInstanceOf(SequenceItem.class);
        SequenceItem root = (SequenceItem) api.getWorkflow();
        assertThat(root.getItems()).hasSize(4);
        assertThat(root.getItems().get(0)).isInstanceOf(AgentRefItem.class);
        assertThat(((AgentRefItem) root.getItems().get(0)).getRefId()).isEqualTo("researcher");
        assertThat(root.getItems().get(1)).isInstanceOf(A2aAgentItem.class);
        assertThat(root.getItems().get(2)).isInstanceOf(AgentRefItem.class);
        assertThat(root.getItems().get(3)).isInstanceOf(HumanItem.class);

        HumanItem human = (HumanItem) root.getItems().get(3);
        assertThat(human.getWhen()).isNotNull();
        assertThat(human.getWhen().getCombine()).isEqualTo("all");
        assertThat(human.getWhen().getClauses().get(0).getOp()).isEqualTo("isPresent");

        // root contract — a defaulted input
        assertThat(root.getOutput()).isEqualTo("report");
        assertThat(root.getInputs()).anySatisfy(i -> {
            assertThat(i.getName()).isEqualTo("tone");
            assertThat(i.getDefaultValue()).isEqualTo("neutral");
        });
    }

    @Test
    void should_reject_a_bare_agent_reference_at_the_workflow_root() {
        // The workflow root is type-locked to Workflow (controls + human); a leaf "agent" is not a Workflow.
        // language=JSON
        String invalid = """
            { "definitionVersion": "V4", "type": "agent", "id": "x", "name": "x", "apiVersion": "1.0.0", "kind": "workflow",
              "workflow": { "type": "agent", "refId": "researcher" } }
            """;
        assertThatThrownBy(() -> mapper.readValue(invalid, AbstractApi.class)).isInstanceOf(Exception.class);
    }

    @Test
    void should_round_trip_each_example() throws Exception {
        for (String json : List.of(RESEARCHER, WRITING_TEAM, RESEARCH_WRITER)) {
            AgentApi api = (AgentApi) mapper.readValue(json, AbstractApi.class);
            String s1 = mapper.writeValueAsString(api);
            AgentApi again = (AgentApi) mapper.readValue(s1, AbstractApi.class);
            // serialization is stable, and the object survives a deserialize→serialize→deserialize cycle
            assertThat(mapper.writeValueAsString(again)).isEqualTo(s1);
            assertThat(again).isEqualTo(api);
        }
    }

    @Test
    void should_collect_only_own_capability_plugins_not_referenced_agents() throws Exception {
        // Standalone: its own model/tools/skills (memory is a referenced resource → no plugin).
        AgentApi researcher = (AgentApi) mapper.readValue(RESEARCHER, AbstractApi.class);
        assertThat(researcher.getPlugins())
            .contains(new Plugin("model", "openai"), new Plugin("tool", "mcp"), new Plugin("skill", "inline"))
            .doesNotContain(new Plugin("memory", "redis-memory-store"));

        // Workflow/sequence: its agent items are references → contribute no plugins (only entrypoint plugins).
        AgentApi rw = (AgentApi) mapper.readValue(RESEARCH_WRITER, AbstractApi.class);
        assertThat(rw.getPlugins()).doesNotContain(new Plugin("model", "openai"), new Plugin("tool", "mcp"), new Plugin("skill", "inline"));

        // Workflow/supervisor: the inline supervisor model IS contributed; the referenced items are not.
        AgentApi team = (AgentApi) mapper.readValue(WRITING_TEAM, AbstractApi.class);
        assertThat(team.getPlugins()).contains(new Plugin("model", "openai"));
    }
}
