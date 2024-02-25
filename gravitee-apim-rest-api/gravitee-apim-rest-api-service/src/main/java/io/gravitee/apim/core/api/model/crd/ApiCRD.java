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
package io.gravitee.apim.core.api.model.crd;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiMetadata;
import io.gravitee.definition.model.DefinitionContext;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.ResponseTemplate;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.definition.model.v4.resource.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
public class ApiCRD {

    private String id;

    private String crossId;

    private String name;

    private String description;

    private String version;

    private String type;

    private String state;

    private String lifecycleState;

    private String visibility;

    private DefinitionContext definitionContext;

    private Map<String, Map<String, ResponseTemplate>> responseTemplates;

    private Set<String> tags;

    private Set<String> labels;

    private List<Resource> resources;

    private Map<String, PlanCRD> plans;

    private List<Flow> flows;

    private List<Property> properties;

    private List<ApiMetadata> metadata;

    private List<Listener> listeners;

    private List<EndpointGroup> endpointGroups;

    private Analytics analytics;

    public String getDefinitionVersion() {
        return "V4";
    }

    public Api toApi() {
        return Api
            .builder()
            .id(id)
            .crossId(crossId)
            .name(name)
            .version(version)
            .definitionVersion(DefinitionVersion.V4)
            .description(description)
            .labels(labels == null ? null : new ArrayList<>(labels))
            .type(ApiType.valueOf(type))
            .apiLifecycleState(Api.ApiLifecycleState.valueOf(lifecycleState))
            .lifecycleState(Api.LifecycleState.valueOf(state))
            .apiDefinitionV4(toApiDefinition())
            .build();
    }

    public io.gravitee.definition.model.v4.Api toApiDefinition() {
        return io.gravitee.definition.model.v4.Api
            .builder()
            .analytics(analytics)
            .apiVersion(version)
            .definitionVersion(DefinitionVersion.V4)
            .endpointGroups(endpointGroups)
            .flows(flows)
            .id(id)
            .listeners(listeners)
            .name(name)
            .properties(properties)
            .resources(resources)
            .responseTemplates(responseTemplates)
            .tags(tags)
            .type(ApiType.valueOf(type))
            .build();
    }
}
