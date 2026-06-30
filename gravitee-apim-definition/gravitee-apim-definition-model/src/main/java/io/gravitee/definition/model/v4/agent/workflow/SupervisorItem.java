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
import io.gravitee.definition.model.Plugin;
import io.gravitee.definition.model.v4.agent.definition.AgentModel;
import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * {@code supervisor} — an LLM-driven orchestrator: its own {@code model} plans which of its {@code items}
 * (candidates) to invoke to reach the {@code goal}, guided by {@code role}/{@code instructions}.
 */
@NoArgsConstructor
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SupervisorItem extends ControlItem {

    /** The planning model (a supervisor calls an LLM, so it carries its own model). */
    private AgentModel model;

    private String role;
    private String goal;
    private String instructions;

    @Override
    public List<Plugin> collectPlugins() {
        List<Plugin> plugins = new ArrayList<>(super.collectPlugins());
        if (model != null && model.getType() != null) {
            plugins.add(new Plugin("model", model.getType()));
        }
        return plugins;
    }
}
