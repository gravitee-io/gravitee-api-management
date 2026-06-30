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
 * {@code a2a-agent} — an <b>external</b> A2A agent reached over the network (not deployed in this gateway);
 * {@code configuration} carries its address/credentials. Not a {@link Workflow} — cannot be a workflow root.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class A2aAgentItem extends WorkflowItem {

    /** Address/credentials of the external A2A agent (e.g. {@code { "url": "https://…" }}). */
    private Object configuration;

    @Override
    public List<Plugin> collectPlugins() {
        return List.of();
    }
}
