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

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.execution.FlowExecution;
import io.gravitee.definition.model.v4.listener.Listener;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class NewApi {

    private String name;

    private String apiVersion;

    @Builder.Default
    private DefinitionVersion definitionVersion = DefinitionVersion.V4;

    private ApiType type;

    private String description;

    @Builder.Default
    private Set<String> tags = Set.of();

    private List<Listener> listeners;

    private List<EndpointGroup> endpointGroups;

    private Analytics analytics;

    private FlowExecution flowExecution;

    @Builder.Default
    private List<Flow> flows = List.of();

    @Builder.Default
    private Set<String> groups = Set.of();

    public Api toApi() {
        return Api
            .builder()
            .name(name)
            .version(apiVersion)
            .definitionVersion(definitionVersion)
            .description(description)
            .groups(groups)
            .apiDefinitionV4(toApiDefinition())
            .build();
    }

    public io.gravitee.definition.model.v4.Api toApiDefinition() {
        return io.gravitee.definition.model.v4.Api
            .builder()
            .name(name)
            .apiVersion(apiVersion)
            .definitionVersion(definitionVersion)
            .analytics(analytics)
            .tags(tags)
            .listeners(listeners)
            .endpointGroups(endpointGroups)
            .flows(flows)
            .flowExecution(flowExecution)
            .build();
    }
}
