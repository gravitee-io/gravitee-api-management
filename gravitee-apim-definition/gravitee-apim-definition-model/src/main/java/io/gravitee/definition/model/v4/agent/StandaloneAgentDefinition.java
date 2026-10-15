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
import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.definition.model.Plugin;
import io.gravitee.definition.model.v4.agent.definition.AgentInput;
import io.gravitee.definition.model.v4.agent.definition.AgentModel;
import io.gravitee.definition.model.v4.agent.definition.AgentOutput;
import io.gravitee.definition.model.v4.agent.definition.AgentSkill;
import io.gravitee.definition.model.v4.agent.definition.AgentTool;
import io.gravitee.definition.model.v4.agent.definition.WorkingMemory;
import io.gravitee.definition.model.v4.agent.evaluation.Recording;
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
 * {@code inputs}/{@code outputs} contract. Capabilities are inline plugin references ({@code tools}/{@code skills})
 * except memory, whose {@code workingMemory} references an independently-deployed chat-memory store resource.
 *
 * <p>An agent that scores other agents' work is not one of these: it is {@code kind:judge}, see
 * {@link JudgeDefinition}.</p>
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

    /** What this agent produces. See {@link AgentOutput} for what the list's length means. */
    private List<AgentOutput> outputs;

    /**
     * Where {@code judge} lived before it became a kind of its own — see {@link JudgeDefinition}.
     *
     * <p>Read but never written: the definition mapper ignores unknown properties, so without this slot a stale
     * {@code standalone.judge} would load silently as an ordinary assistant. Keeping the key visible lets the gateway
     * refuse it at deploy by name, with the fix spelled out, instead of answering prose on a judge's door. Left out
     * of equality and serialization so it can never travel back to a store.</p>
     */
    @JsonProperty(value = "judge", access = JsonProperty.Access.WRITE_ONLY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Object judge;

    /**
     * Keeps a durable record of this agent's runs so they can be scored later — see {@link Recording}.
     *
     * <p>Off unless declared. It persists prompts and answers, so it is the agent's own decision rather than a
     * gateway-wide setting.</p>
     */
    private Recording recording;

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
