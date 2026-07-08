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

import io.gravitee.definition.model.v4.agent.AgentApi;
import io.gravitee.definition.model.v4.agent.StandaloneAgentDefinition;
import io.gravitee.definition.model.v4.listener.Listener;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Setter(lombok.AccessLevel.NONE)
public class NewAgentApi extends AbstractNewApi {

    private String kind;

    private boolean composable;

    private List<Listener> listeners;

    private StandaloneAgentDefinition standalone;

    /**
     * @return An {@link AgentApi.AgentApiBuilder} based on the current state of this NewAgentApi.
     */
    public AgentApi.AgentApiBuilder<?, ?> toApiDefinitionBuilder() {
        return AgentApi.builder()
            .name(name)
            .type(type)
            .apiVersion(apiVersion)
            .definitionVersion(definitionVersion)
            .tags(tags)
            .kind(kind)
            .composable(composable)
            .listeners(listeners)
            .standalone(standalone);
    }
}
