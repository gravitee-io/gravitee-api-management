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
import io.gravitee.definition.model.v4.failover.Failover;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.execution.FlowExecution;
import io.gravitee.definition.model.v4.listener.Listener;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Setter(lombok.AccessLevel.NONE)
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

    private Failover failover;

    /**
     * @return An instance of {@link Api.ApiBuilder} based on the current state of this NewApi.
     */
    public Api.ApiBuilder toApiBuilder() {
        // Currently we can't use MapStruct in core. We will need to discuss as team if we want to introduce a rule to allow MapStruct in core.
        return Api
            .builder()
            .name(name)
            .version(apiVersion)
            .type(type)
            .definitionVersion(definitionVersion)
            .description(description)
            .groups(groups);
    }

    /**
     * @return An instance of {@link io.gravitee.definition.model.v4.Api.ApiBuilder} based on the current state of this NewApi.
     */
    public io.gravitee.definition.model.v4.Api.ApiBuilder<?, ?> toApiDefinitionBuilder() {
        // Currently we can't use MapStruct in core. We will need to discuss as team if we want to introduce a rule to allow MapStruct in core.
        return io.gravitee.definition.model.v4.Api
            .builder()
            .name(name)
            .type(type)
            .apiVersion(apiVersion)
            .definitionVersion(definitionVersion)
            .analytics(analytics)
            .tags(tags)
            .listeners(listeners)
            .endpointGroups(endpointGroups)
            .flows(flows)
            .flowExecution(flowExecution)
            .failover(failover);
    }
}
