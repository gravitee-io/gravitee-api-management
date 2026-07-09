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
package io.gravitee.rest.api.model.v4.agent;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.agent.AgentAnalytics;
import io.gravitee.definition.model.v4.agent.StandaloneAgentDefinition;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.WorkflowState;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.context.OriginContext;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A first-class agent read model: sibling of {@link io.gravitee.rest.api.model.v4.api.ApiEntity} and
 * {@link io.gravitee.rest.api.model.federation.FederatedApiAgentEntity}, discriminated by {@link DefinitionVersion#AGENT}.
 * Phase 1 covers {@code kind = standalone}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class AgentApiEntity implements GenericApiEntity {

    private String id;
    private String crossId;
    private String hrid;
    private String name;
    private String apiVersion;

    @Builder.Default
    private DefinitionVersion definitionVersion = DefinitionVersion.AGENT;

    @Builder.Default
    private ApiType type = ApiType.AGENT;

    private Date deployedAt;
    private Date createdAt;
    private Date updatedAt;
    private String description;
    private Set<String> groups;
    private Visibility visibility;

    @Builder.Default
    private Lifecycle.State state = Lifecycle.State.STOPPED;

    private PrimaryOwnerEntity primaryOwner;
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

    private Set<String> tags;

    // Agent-specific
    private String kind;
    private boolean composable;
    private List<Listener> listeners;
    private StandaloneAgentDefinition standalone;
    private AgentAnalytics analytics;
    private List<Resource> resources;
    private List<Plan> plans;

    @JsonIgnore
    private String referenceType;

    @JsonIgnore
    private String referenceId;

    @JsonIgnore
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}
