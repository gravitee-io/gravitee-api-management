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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.gravitee.definition.model.Plugin;
import io.gravitee.definition.model.v4.agent.definition.ScopePersistence;
import java.util.List;

/**
 * Marker type for a workflow <b>root</b>: only orchestration constructs (the controls and the human gate) may be
 * the root of a {@code kind:workflow} agent. Typing {@code AgentApi.workflow} as {@code Workflow} (rather than
 * the wider {@link WorkflowItem}) makes Jackson reject a bare {@code agent}/{@code a2a-agent} leaf at the root.
 *
 * <p>Control classes <b>implement {@code Workflow} AND extend {@link WorkflowItem}</b>; leaf items
 * ({@code agent}/{@code a2a-agent}) extend {@code WorkflowItem} only. Jackson selects the subtype set from the
 * declared field type, so the same concrete class is registered under both bases with the same {@code type}.</p>
 *
 * @author GraviteeSource Team
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes(
    {
        @JsonSubTypes.Type(value = SequenceItem.class, name = "sequence"),
        @JsonSubTypes.Type(value = ParallelItem.class, name = "parallel"),
        @JsonSubTypes.Type(value = LoopItem.class, name = "loop"),
        @JsonSubTypes.Type(value = ConditionalItem.class, name = "conditional"),
        @JsonSubTypes.Type(value = SupervisorItem.class, name = "supervisor"),
        @JsonSubTypes.Type(value = HumanItem.class, name = "human"),
    }
)
public interface Workflow {
    /** Recursively collects the capability plugins this workflow (and its descendants) reference. */
    List<Plugin> collectPlugins();

    /** The root's scope persistence (a {@code ref} to a store resource), or {@code null} for an ephemeral scope. */
    ScopePersistence getScopePersistence();
}
