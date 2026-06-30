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

import com.fasterxml.jackson.annotation.JsonInclude;
import io.gravitee.definition.model.Plugin;
import io.gravitee.definition.model.v4.agent.definition.AgentInput;
import io.gravitee.definition.model.v4.agent.definition.AgentModel;
import io.gravitee.definition.model.v4.agent.definition.AgentSkill;
import io.gravitee.definition.model.v4.agent.definition.AgentTool;
import io.gravitee.definition.model.v4.agent.definition.WorkingMemory;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * The body of a {@code kind:standalone} agent — a single task agent: a {@code model}, a {@code role}/{@code goal}/
 * {@code instructions} prompt, capabilities ({@code tools}/{@code skills}/{@code workingMemory}) and the
 * {@code inputs}/{@code output} contract. Capabilities are inline plugin references ({@code tools}/{@code skills})
 * except memory, whose {@code workingMemory} references an independently-deployed chat-memory store resource.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StandaloneAgentDefinition {

    private AgentModel model;
    private String role;
    private String goal;
    private String instructions;
    private List<AgentTool> tools;
    private List<AgentSkill> skills;
    private WorkingMemory workingMemory;
    private List<AgentInput> inputs;
    private String output;

    /** The capability plugins this agent references — its model, tools and skills. */
    public List<Plugin> collectPlugins() {
        List<Plugin> plugins = new ArrayList<>();
        if (model != null && model.getType() != null) {
            plugins.add(new Plugin("model", model.getType()));
        }
        if (tools != null) {
            tools
                .stream()
                .filter(t -> t.getType() != null)
                .forEach(t -> plugins.add(new Plugin("tool", t.getType())));
        }
        if (skills != null) {
            skills
                .stream()
                .filter(s -> s.getType() != null)
                .forEach(s -> plugins.add(new Plugin("skill", s.getType())));
        }
        // workingMemory references an independently-deployed store resource → contributes no plugin here.
        return plugins;
    }
}
