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
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * {@code agent} — a <b>reference</b> (by {@code refId}) to an independently-deployed agent (which must be
 * {@code composable}). The only way to include an agent in a workflow: its body is never
 * embedded, so its secrets/recipe are never copied here. Contributes no plugins (the referenced agent is
 * deployed and counted on its own). Not a {@link Workflow} — cannot be a workflow root.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentRefItem extends WorkflowItem {

    /** Id of the referenced, independently-deployed agent. */
    private String refId;

    @Override
    public List<Plugin> collectPlugins() {
        return List.of();
    }
}
