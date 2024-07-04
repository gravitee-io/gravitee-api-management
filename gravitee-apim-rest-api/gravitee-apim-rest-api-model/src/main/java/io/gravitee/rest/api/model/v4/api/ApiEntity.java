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
package io.gravitee.rest.api.model.v4.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
import io.gravitee.rest.api.model.DeploymentRequired;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.WorkflowState;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.context.OriginContext;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.With;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Schema(name = "ApiEntityV4")
@Builder(toBuilder = true)
@With
public class ApiEntity implements GenericApiEntity {

    @Schema(description = "API's uuid.", example = "00f8c9e7-78fc-4907-b8c9-e778fc790750")
    private String id;

    @Schema(description = "API's crossId. Identifies API across environments.", example = "df83b2a4-cc3e-3f80-9f0d-c138c106c076")
    private String crossId;

    @Schema(description = "API's name. Duplicate names can exists.", example = "My Api")
    private String name;

    @Schema(description = "Api's version. It's a simple string only used in the portal.", example = "v1.0")
    private String apiVersion;

    @Schema(description = "API's gravitee definition version")
    private DefinitionVersion definitionVersion;

    @Schema(description = "API's type", example = "async")
    private ApiType type;

    @Schema(description = "The last date (as timestamp) when the API was deployed.", example = "1581256457163")
    private Date deployedAt;

    @Schema(description = "The date (as a timestamp) when the API was created.", example = "1581256457163")
    private Date createdAt;

    @Schema(description = "The last date (as a timestamp) when the API was updated.", example = "1581256457163")
    private Date updatedAt;

    @Schema(
        description = "API's description. A short description of your API.",
        example = "I can use a hundred characters to describe this API."
    )
    private String description;

    @Schema(description = "The list of sharding tags associated with this API.", example = "public, private")
    @DeploymentRequired
    @Builder.Default
    private Set<String> tags = new HashSet<>();

    @Schema(description = "A list of listeners used to describe our you api could be reached.")
    @DeploymentRequired
    private List<Listener> listeners;

    @Schema(description = "A list of endpoint describing the endpoints to contact.")
    @DeploymentRequired
    private List<EndpointGroup> endpointGroups;

    @Schema(description = "Analytics configuration")
    @DeploymentRequired
    private Analytics analytics;

    @Schema(description = "Failover configuration")
    @DeploymentRequired
    private Failover failover;

    @Schema(description = "A dictionary (could be dynamic) of properties available in the API context.")
    @DeploymentRequired
    @Builder.Default
    private List<Property> properties = new ArrayList<>();

    @Schema(description = "The list of API resources used by policies like cache resources or oauth2")
    @DeploymentRequired
    @Builder.Default
    private List<Resource> resources = new ArrayList<>();

    @Schema(description = "A list of plans to apply on the API")
    @DeploymentRequired
    @Builder.Default
    private Set<PlanEntity> plans = new HashSet<>();

    @Schema(description = "API's flow execution.")
    private FlowExecution flowExecution;

    @Schema(description = "A list of flows containing the policies configuration.")
    @DeploymentRequired
    private List<Flow> flows;

    @DeploymentRequired
    @Schema(
        description = "A map that allows you to configure the output of a request based on the event throws by the gateway. Example : Quota exceeded, api-key is missing, ..."
    )
    @Builder.Default
    private Map<String, Map<String, ResponseTemplate>> responseTemplates = new LinkedHashMap<>();

    @DeploymentRequired
    @Schema(description = "The configuration of API services like the dynamic properties.")
    private ApiServices services;

    @Schema(description = "API's groups. Used to add team in your API.", example = "['MY_GROUP1', 'MY_GROUP2']")
    private Set<String> groups;

    @Schema(description = "The visibility of the API regarding the portal.", example = "PUBLIC")
    private Visibility visibility;

    @Schema(description = "The status of the API regarding the gateway.", example = "STARTED")
    @Builder.Default
    private Lifecycle.State state = Lifecycle.State.STOPPED;

    @Schema(description = "The user with role PRIMARY_OWNER on this API.")
    private PrimaryOwnerEntity primaryOwner;

    @Schema(description = "the API logo encoded in base64")
    private String picture;

    @Schema(
        description = "the API logo URL.",
        example = "https://gravitee.mycompany.com/management/apis/6c530064-0b2c-4004-9300-640b2ce0047b/picture"
    )
    private String pictureUrl;

    @Schema(description = "the list of categories associated with this API", example = "Product, Customer, Misc")
    private Set<String> categories;

    @Schema(description = "the free list of labels associated with this API", example = "json, read_only, awesome")
    private List<String> labels;

    /** Context explaining where the API comes from. */
    @Builder.Default
    private OriginContext originContext = new OriginContext.Management();

    @JsonIgnore
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    private ApiLifecycleState lifecycleState;

    private WorkflowState workflowState;

    private boolean disableMembershipNotifications;

    @Schema(description = "the API background encoded in base64")
    private String background;

    @Schema(
        description = "the API background url.",
        example = "https://gravitee.mycompany.com/management/apis/6c530064-0b2c-4004-9300-640b2ce0047b/background"
    )
    private String backgroundUrl;

    @JsonIgnore
    private String referenceType;

    @JsonIgnore
    private String referenceId;
}
