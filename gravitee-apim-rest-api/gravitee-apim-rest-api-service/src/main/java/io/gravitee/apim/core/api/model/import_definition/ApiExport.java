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
package io.gravitee.apim.core.api.model.import_definition;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.Api.ApiBuilder;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.ResponseTemplate;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.failover.Failover;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.execution.FlowExecution;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.definition.model.v4.service.ApiServices;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.WorkflowState;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.context.OriginContext;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
public class ApiExport {

    private String id;
    private String crossId;
    private String name;
    private String apiVersion;
    private DefinitionVersion definitionVersion;
    private ApiType type;
    private String description;

    @Builder.Default
    private Set<String> tags = new HashSet<>();

    private List<Listener> listeners;
    private List<EndpointGroup> endpointGroups;
    private Analytics analytics;
    private Failover failover;

    @Builder.Default
    private List<Property> properties = new ArrayList<>();

    @Builder.Default
    private List<Resource> resources = new ArrayList<>();

    private FlowExecution flowExecution;
    private List<Flow> flows;

    @Builder.Default
    private Map<String, Map<String, ResponseTemplate>> responseTemplates = new LinkedHashMap<>();

    private ApiServices services;
    private Set<String> groups;
    private Visibility visibility;

    @Builder.Default
    private Lifecycle.State state = Lifecycle.State.STOPPED;

    private String picture;
    private String pictureUrl;
    private Set<String> categories;
    private List<String> labels;

    @Builder.Default
    private OriginContext originContext = new OriginContext.Management();

    private ApiLifecycleState lifecycleState;
    private WorkflowState workflowState;
    private boolean disableMembershipNotifications;
    private String background;
    private String backgroundUrl;

    public ApiBuilder toApiBuilder() {
        return Api
            .builder()
            .id(id)
            .crossId(crossId)
            .name(name)
            .version(apiVersion)
            .originContext(originContext)
            .definitionVersion(DefinitionVersion.V4)
            .type(type)
            .description(description)
            .picture(picture)
            .background(background)
            .groups(groups == null ? null : new HashSet<>(groups))
            .categories(categories == null ? null : new HashSet<>(categories))
            .labels(labels == null ? null : new ArrayList<>(labels))
            .disableMembershipNotifications(disableMembershipNotifications);
    }

    public io.gravitee.definition.model.v4.Api.ApiBuilder toApiDefinitionBuilder() {
        return io.gravitee.definition.model.v4.Api
            .builder()
            .analytics(analytics)
            .apiVersion(apiVersion)
            .definitionVersion(DefinitionVersion.V4)
            .endpointGroups(endpointGroups)
            .failover(failover)
            .flows(flows)
            .listeners(listeners)
            .name(name)
            .properties(properties)
            .resources(resources)
            .responseTemplates(responseTemplates)
            .tags(tags)
            .type(type)
            .services(services);
    }
}
