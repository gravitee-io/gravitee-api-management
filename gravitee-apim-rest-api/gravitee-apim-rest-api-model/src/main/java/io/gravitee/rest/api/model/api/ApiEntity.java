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
package io.gravitee.rest.api.model.api;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.model.DefinitionContext;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.definition.model.FlowMode;
import io.gravitee.definition.model.Properties;
import io.gravitee.definition.model.Property;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.ResponseTemplate;
import io.gravitee.definition.model.Rule;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.plugins.resources.Resource;
import io.gravitee.definition.model.services.Services;
import io.gravitee.rest.api.model.DeploymentRequired;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.WorkflowState;
import io.gravitee.rest.api.model.context.OriginContext;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
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
import lombok.experimental.SuperBuilder;

/**
 * --------------------------------------------------------------------------------------------------------------
 * --------------------------------------------------------------------------------------------------------------
 * /!\ Do not forget to update {@see io.gravitee.rest.api.service.jackson.ser.api.ApiDefaultSerializer}
 * for each modification of the ApiEntity class to apply export API changes /!\
 * --------------------------------------------------------------------------------------------------------------
 * --------------------------------------------------------------------------------------------------------------
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@JsonFilter("apiMembershipTypeFilter")
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class ApiEntity implements GenericApiEntity {

    @Schema(description = "API's uuid.", example = "00f8c9e7-78fc-4907-b8c9-e778fc790750")
    @EqualsAndHashCode.Include
    private String id;

    @Schema(description = "API's crossId. Identifies API across environments.", example = "df83b2a4-cc3e-3f80-9f0d-c138c106c076")
    private String crossId;

    @Schema(description = "ID of the environment where the API is in.", example = "DEFAULT", accessMode = Schema.AccessMode.READ_ONLY)
    private String environmentId;

    @Schema(description = "API's name. Duplicate names can exists.", example = "My Api")
    private String name;

    @Schema(description = "API's version. It's a simple string only used in the portal.", example = "v1.0")
    @EqualsAndHashCode.Include
    private String version;

    @Schema(
        description = "API's description. A short description of your API.",
        example = "I can use a hundred characters to describe this API."
    )
    private String description;

    @Schema(description = "API's execution mode. Define if the execution mode should use v3 or v4-emulation-engine.", example = "v3")
    @DeploymentRequired
    @JsonProperty(value = "execution_mode")
    private ExecutionMode executionMode;

    @Schema(description = "API's groups. Used to add team in your API.", example = "['MY_GROUP1', 'MY_GROUP2']")
    private Set<String> groups;

    @JsonProperty(value = "context_path")
    @Schema(description = "API's context path.", example = "/my-awesome-api")
    private String contextPath;

    @NotNull
    @DeploymentRequired
    @JsonProperty(value = "proxy", required = true)
    @Schema(description = "API's definition.")
    private Proxy proxy;

    @DeploymentRequired
    @JsonProperty(value = "flow_mode")
    @Schema(description = "API's flow mode.", example = "BEST_MATCH")
    private FlowMode flowMode;

    @DeploymentRequired
    @JsonProperty(value = "paths")
    @Schema(description = "a map where you can associate a path to a configuration (the policies configuration)")
    private Map<String, List<Rule>> paths = new HashMap<>();

    @DeploymentRequired
    @JsonProperty(value = "flows")
    @Schema(description = "a list of flows (the policies configuration)")
    private List<Flow> flows = new ArrayList<>();

    @DeploymentRequired
    @JsonProperty(value = "plans")
    @Schema(description = "a list of plans with flows (the policies configuration)")
    private Set<PlanEntity> plans = new HashSet<>();

    @DeploymentRequired
    @JsonProperty(value = "gravitee")
    @Schema(description = "API's gravitee definition version")
    private String graviteeDefinitionVersion;

    @JsonProperty(value = "definition_context")
    @Schema(description = "the context where the api definition was created from")
    private DefinitionContext definitionContext;

    @JsonProperty("deployed_at")
    @Schema(description = "The last date (as timestamp) when the API was deployed.", example = "1581256457163")
    private Date deployedAt;

    @JsonProperty("created_at")
    @Schema(description = "The date (as a timestamp) when the API was created.", example = "1581256457163")
    private Date createdAt;

    @JsonProperty("updated_at")
    @Schema(description = "The last date (as a timestamp) when the API was updated.", example = "1581256457163")
    private Date updatedAt;

    @Schema(description = "The visibility of the API regarding the portal.", example = "PUBLIC")
    private Visibility visibility;

    @Schema(description = "The status of the API regarding the gateway.", example = "STARTED")
    @Builder.Default
    private Lifecycle.State state = Lifecycle.State.STOPPED;

    @JsonProperty("owner")
    @Schema(description = "The user with role PRIMARY_OWNER on this API.")
    private PrimaryOwnerEntity primaryOwner;

    @DeploymentRequired
    @JsonProperty(value = "properties")
    @Schema(description = "A dictionary (could be dynamic) of properties available in the API context.")
    private io.gravitee.definition.model.Properties properties;

    @DeploymentRequired
    @JsonProperty(value = "services")
    @Schema(description = "The configuration of API services like the dynamic properties, the endpoint discovery or the healthcheck.")
    private Services services;

    @DeploymentRequired
    @Schema(description = "the list of sharding tags associated with this API.", example = "public, private")
    private Set<String> tags;

    @Schema(description = "the API logo encoded in base64")
    private String picture;

    @JsonProperty(value = "picture_url")
    @Schema(
        description = "the API logo url.",
        example = "https://gravitee.mycompany.com/management/apis/6c530064-0b2c-4004-9300-640b2ce0047b/picture"
    )
    private String pictureUrl;

    @DeploymentRequired
    @JsonProperty(value = "resources")
    @Schema(description = "The list of API resources used by policies like cache resources or oauth2")
    private List<Resource> resources = new ArrayList<>();

    @Schema(description = "the list of category keys associated with this API", example = "Product, Customer, Misc")
    private Set<String> categories;

    @Schema(description = "the free list of labels associated with this API", example = "json, read_only, awesome")
    private List<String> labels;

    @DeploymentRequired
    @JsonProperty(value = "path_mappings")
    @Schema(
        description = "A list of paths used to aggregate data in analytics",
        example = "/products/:productId, /products/:productId/media"
    )
    private Set<String> pathMappings = new HashSet<>();

    @JsonIgnore
    private Map<String, Object> metadata = new HashMap<>();

    @DeploymentRequired
    @JsonProperty(value = "response_templates")
    @Schema(
        description = "A map that allows you to configure the output of a request based on the event throws by the gateway. Example : Quota exceeded, api-ky is missing, ..."
    )
    private Map<String, Map<String, ResponseTemplate>> responseTemplates;

    @JsonProperty(value = "lifecycle_state")
    private ApiLifecycleState lifecycleState;

    @JsonProperty(value = "workflow_state")
    private WorkflowState workflowState;

    @JsonProperty("disable_membership_notifications")
    private boolean disableMembershipNotifications;

    private List<ApiEntrypointEntity> entrypoints;

    @Schema(description = "the API background encoded in base64")
    private String background;

    @JsonProperty(value = "background_url")
    @Schema(
        description = "the API background url.",
        example = "https://gravitee.mycompany.com/management/apis/6c530064-0b2c-4004-9300-640b2ce0047b/background"
    )
    private String backgroundUrl;

    @JsonIgnore
    private String referenceType;

    @JsonIgnore
    private String referenceId;

    @JsonIgnore
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    @JsonGetter("properties")
    public List<Property> getPropertyList() {
        if (properties != null) {
            return properties.getProperties();
        }
        return Collections.emptyList();
    }

    @JsonSetter("properties")
    public void setPropertyList(List<Property> properties) {
        this.properties = new Properties();
        this.properties.setProperties(properties);
    }

    @Override
    @JsonIgnore
    public String getApiVersion() {
        return version;
    }

    @Override
    @JsonIgnore
    public DefinitionVersion getDefinitionVersion() {
        if (graviteeDefinitionVersion != null) {
            return DefinitionVersion.valueOfLabel(graviteeDefinitionVersion);
        }
        return null;
    }

    @Override
    public OriginContext getOriginContext() {
        if (definitionContext == null) {
            return new OriginContext.Management();
        } else if (definitionContext.isOriginKubernetes()) {
            return new OriginContext.Kubernetes(
                OriginContext.Kubernetes.Mode.valueOf(definitionContext.getMode().toUpperCase()),
                definitionContext.getSyncFrom()
            );
        }
        return new OriginContext.Management();
    }
}
