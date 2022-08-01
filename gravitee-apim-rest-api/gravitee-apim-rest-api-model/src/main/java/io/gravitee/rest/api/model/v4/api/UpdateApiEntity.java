/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.model.v4.api;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.FlowMode;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.definition.model.v4.responsetemplate.ResponseTemplate;
import io.gravitee.definition.model.v4.service.ApiServices;
import io.gravitee.rest.api.model.ApiMetadataEntity;
import io.gravitee.rest.api.model.DeploymentRequired;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.WorkflowState;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.v4.api.properties.PropertyEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@NoArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class UpdateApiEntity {

    @Schema(description = "API's uuid.", example = "00f8c9e7-78fc-4907-b8c9-e778fc790750")
    private String id;

    @Schema(description = "API's crossId. Identifies API across environments.", example = "00f8c9e7-78fc-4907-b8c9-e778fc790750")
    private String crossId;

    @NotBlank
    @NotEmpty(message = "API's name must not be empty")
    @Schema(description = "API's name. Duplicate names can exists.", example = "My Api")
    private String name;

    @NotBlank
    @Schema(description = "Api's version. It's a simple string only used in the portal.", example = "v1.0")
    private String apiVersion;

    @NotNull
    @Schema(description = "API's gravitee definition version")
    private DefinitionVersion definitionVersion;

    @NotNull
    @Schema(description = "API's type", example = "async")
    private ApiType type;

    @NotBlank
    @Schema(
        description = "API's description. A short description of your API.",
        example = "I can use a hundred characters to describe this API."
    )
    private String description;

    @Schema(description = "The list of sharding tags associated with this API.", example = "public, private")
    @DeploymentRequired
    private Set<String> tags = new HashSet<>();

    @NotNull
    @Valid
    @Schema(description = "A list of listeners used to describe our you api could be reached.")
    @DeploymentRequired
    private List<@NotNull Listener> listeners;

    @NotNull
    @Valid
    @Schema(description = "A list of endpoint describing the endpoints to contact.")
    @DeploymentRequired
    private List<EndpointGroup> endpointGroups;

    @Schema(description = "A dictionary (could be dynamic) of properties available in the API context.")
    @DeploymentRequired
    private List<PropertyEntity> properties = new ArrayList<>();

    @Schema(description = "The list of API resources used by policies like cache resources or oauth2")
    @DeploymentRequired
    private List<Resource> resources = new ArrayList<>();

    @Schema(description = "A list of plans to apply on the API")
    @DeploymentRequired
    private Set<PlanEntity> plans = new HashSet<>();

    @Schema(description = "API's flow mode.", example = "BEST_MATCH")
    @DeploymentRequired
    private FlowMode flowMode;

    @Valid
    @Schema(description = "A list of flows containing the policies configuration.")
    @DeploymentRequired
    private List<Flow> flows;

    @Valid
    @DeploymentRequired
    @Schema(
        description = "A map that allows you to configure the output of a request based on the event throws by the gateway. Example : Quota exceeded, api-key is missing, ..."
    )
    private Map<String, Map<String, ResponseTemplate>> responseTemplates = new LinkedHashMap<>();

    @Valid
    @DeploymentRequired
    @Schema(description = "The configuration of API services like the dynamic properties.")
    private ApiServices services;

    @Schema(description = "API's groups. Used to add team in your API.", example = "['MY_GROUP1', 'MY_GROUP2']")
    private Set<@NotBlank String> groups;

    @NotNull
    @Schema(description = "The visibility of the API regarding the portal.", example = "PUBLIC")
    private Visibility visibility;

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

    private List<ApiMetadataEntity> metadata;

    private ApiLifecycleState lifecycleState;

    private boolean disableMembershipNotifications;

    @Schema(description = "the API background encoded in base64")
    private String background;

    @Schema(
        description = "the API background url.",
        example = "https://gravitee.mycompany.com/management/apis/6c530064-0b2c-4004-9300-640b2ce0047b/background"
    )
    private String backgroundUrl;
}
