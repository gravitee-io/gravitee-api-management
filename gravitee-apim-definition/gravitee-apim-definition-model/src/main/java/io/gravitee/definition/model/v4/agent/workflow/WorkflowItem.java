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
package io.gravitee.definition.model.v4.agent.workflow;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.gravitee.definition.model.Plugin;
import io.gravitee.definition.model.v4.agent.definition.AgentInput;
import io.gravitee.definition.model.v4.agent.definition.ScopePersistence;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * A workflow node, discriminated by {@code type}. Two families share this base:
 * <ul>
 *   <li><b>leaf items</b> — {@link AgentRefItem} ({@code agent}, a reference to a deployed agent) and
 *       {@link A2aAgentItem} ({@code a2a-agent}, an external agent); these are <i>not</i> {@link Workflow};</li>
 *   <li><b>controls + human</b> — {@link SequenceItem}/{@link ParallelItem}/{@link LoopItem}/
 *       {@link ConditionalItem}/{@link SupervisorItem}/{@link HumanItem}; these also implement {@link Workflow}
 *       and so may be a workflow root.</li>
 * </ul>
 * No agent body is ever embedded — an agent is always referenced by id ({@code AgentRefItem.refId}).
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes(
    {
        @JsonSubTypes.Type(value = AgentRefItem.class, name = "agent"),
        @JsonSubTypes.Type(value = A2aAgentItem.class, name = "a2a-agent"),
        @JsonSubTypes.Type(value = HumanItem.class, name = "human"),
        @JsonSubTypes.Type(value = SequenceItem.class, name = "sequence"),
        @JsonSubTypes.Type(value = ParallelItem.class, name = "parallel"),
        @JsonSubTypes.Type(value = LoopItem.class, name = "loop"),
        @JsonSubTypes.Type(value = ConditionalItem.class, name = "conditional"),
        @JsonSubTypes.Type(value = SupervisorItem.class, name = "supervisor"),
    }
)
public abstract class WorkflowItem {

    /** {@code agent | a2a-agent | human | sequence | parallel | loop | conditional | supervisor}. */
    protected String type;

    protected String name;

    /** Optional guard — honoured when this item is a child of a {@code conditional}. */
    protected Condition when;

    /** Scope keys this item consumes. On the workflow root these form the agent's external contract. */
    protected List<AgentInput> inputs;

    /** Scope key this item produces. On the workflow root this is the agent's result. */
    protected String output;

    /**
     * How the workflow persists its agentic scope across turns — a {@code ref} to a store resource. Like {@link #inputs}
     * / {@link #output}, this is <b>honored on the workflow root only</b> (ignored on sub-items); it is the workflow
     * counterpart of a standalone agent's {@code workingMemory}. Absent ⇒ the scope stays ephemeral.
     */
    protected ScopePersistence scopePersistence;

    /** Recursively collects the capability plugins this item (and its descendants) reference. */
    public abstract List<Plugin> collectPlugins();
}
