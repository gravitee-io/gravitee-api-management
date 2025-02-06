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

import com.fasterxml.jackson.annotation.JsonProperty;
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
import io.gravitee.definition.model.v4.nativeapi.NativeEndpointGroup;
import io.gravitee.definition.model.v4.nativeapi.NativeFlow;
import io.gravitee.definition.model.v4.nativeapi.NativeListener;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.definition.model.v4.service.ApiServices;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.WorkflowState;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.context.OriginContext;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Builder;

public sealed interface ApiDescriptor {
    String id();
    String crossId();
    String name();
    String apiVersion();
    DefinitionVersion definitionVersion();
    ApiType type();
    String description();
    Instant deployedAt();
    Instant createdAt();
    Instant updatedAt();
    boolean disableMembershipNotifications();
    Map<String, Object> metadata();
    Set<String> groups();
    Visibility visibility();
    List<String> labels();
    ApiLifecycleState lifecycleState();
    Set<String> tags();
    PrimaryOwnerEntity primaryOwner();
    Set<String> categories();
    OriginContext originContext();
    WorkflowState workflowState();
    String picture();
    String background();

    @Builder
    record ApiDescriptorV4(
        String id,
        String crossId,
        String name,
        String apiVersion,
        ApiType type,
        String description,
        Instant deployedAt,
        Instant createdAt,
        Instant updatedAt,
        boolean disableMembershipNotifications,
        Map<String, Object> metadata,
        Set<String> groups,
        Lifecycle.State state,
        Visibility visibility,
        List<String> labels,
        ApiLifecycleState lifecycleState,
        Set<String> tags,
        PrimaryOwnerEntity primaryOwner,
        Set<String> categories,
        OriginContext originContext,
        WorkflowState workflowState,
        String picture,
        String background,
        List<Listener> listeners,
        List<EndpointGroup> endpointGroups,
        Analytics analytics,
        FlowExecution flowExecution,
        List<Flow> flows,
        Map<String, Map<String, ResponseTemplate>> responseTemplates,
        List<Property> properties,
        List<Resource> resources,
        ApiServices services,
        Failover failover
    )
        implements ApiDescriptor {
        @JsonProperty("definitionVersion")
        @Override
        public DefinitionVersion definitionVersion() {
            return DefinitionVersion.V4;
        }
    }

    @Builder
    record ApiDescriptorNative(
        String id,
        String crossId,
        String name,
        String apiVersion,
        String description,
        Instant deployedAt,
        Instant createdAt,
        Instant updatedAt,
        boolean disableMembershipNotifications,
        Map<String, Object> metadata,
        Set<String> groups,
        Lifecycle.State state,
        Visibility visibility,
        List<String> labels,
        ApiLifecycleState lifecycleState,
        Set<String> tags,
        PrimaryOwnerEntity primaryOwner,
        Set<String> categories,
        OriginContext originContext,
        WorkflowState workflowState,
        String picture,
        String background,
        List<NativeListener> listeners,
        List<NativeEndpointGroup> endpointGroups,
        List<NativeFlow> flows,
        List<Property> properties,
        List<Resource> resources
    )
        implements ApiDescriptor {
        @JsonProperty("definitionVersion")
        @Override
        public DefinitionVersion definitionVersion() {
            return DefinitionVersion.V4;
        }

        @JsonProperty("type")
        @Override
        public ApiType type() {
            return ApiType.NATIVE;
        }
    }

    @Builder
    record ApiDescriptorFederated(
        String id,
        String crossId,
        String name,
        String apiVersion,
        ApiType type,
        String description,
        Instant deployedAt,
        Instant createdAt,
        Instant updatedAt,
        boolean disableMembershipNotifications,
        Map<String, Object> metadata,
        Set<String> groups,
        Visibility visibility,
        List<String> labels,
        ApiLifecycleState lifecycleState,
        Set<String> tags,
        PrimaryOwnerEntity primaryOwner,
        Set<String> categories,
        OriginContext originContext,
        WorkflowState workflowState,
        String picture,
        String background,
        String providerId
    )
        implements ApiDescriptor {
        @JsonProperty("definitionVersion")
        @Override
        public DefinitionVersion definitionVersion() {
            return DefinitionVersion.FEDERATED;
        }
    }
}
