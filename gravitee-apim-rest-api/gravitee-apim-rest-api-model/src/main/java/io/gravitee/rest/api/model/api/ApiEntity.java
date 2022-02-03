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
package io.gravitee.rest.api.model.api;

import com.fasterxml.jackson.annotation.*;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.model.*;
import io.gravitee.definition.model.Properties;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.plugins.resources.Resource;
import io.gravitee.definition.model.services.Services;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.search.Indexable;
import io.swagger.annotations.ApiModelProperty;
import java.util.*;
import javax.validation.constraints.NotNull;

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
public class ApiEntity implements Indexable {

    @ApiModelProperty(value = "API's uuid.", example = "00f8c9e7-78fc-4907-b8c9-e778fc790750")
    private String id;

    @ApiModelProperty(value = "API's crossId. Identifies API across environments.", example = "00f8c9e7-78fc-4907-b8c9-e778fc790750")
    private String crossId;

    @ApiModelProperty(value = "API's name. Duplicate names can exists.", example = "My Api")
    private String name;

    @ApiModelProperty(value = "Api's version. It's a simple string only used in the portal.", example = "v1.0")
    private String version;

    @ApiModelProperty(
        value = "API's description. A short description of your API.",
        example = "I can use a hundred characters to describe this API."
    )
    private String description;

    @ApiModelProperty(value = "API's groups. Used to add team in your API.", example = "['MY_GROUP1', 'MY_GROUP2']")
    private Set<String> groups;

    @JsonProperty(value = "context_path")
    @ApiModelProperty(value = "API's context path.", example = "/my-awesome-api")
    private String contextPath;

    @NotNull
    @DeploymentRequired
    @JsonProperty(value = "proxy", required = true)
    @ApiModelProperty(value = "API's definition.")
    private Proxy proxy;

    @DeploymentRequired
    @JsonProperty(value = "flow_mode")
    @ApiModelProperty(value = "API's flow mode.", example = "BEST_MATCH")
    private FlowMode flowMode;

    @DeploymentRequired
    @JsonProperty(value = "paths", required = true)
    @ApiModelProperty(value = "a map where you can associate a path to a configuration (the policies configuration)")
    private Map<String, List<Rule>> paths = new HashMap<>();

    @DeploymentRequired
    @JsonProperty(value = "flows", required = true)
    @ApiModelProperty(value = "a list of flows (the policies configuration)")
    private List<Flow> flows = new ArrayList<>();

    @DeploymentRequired
    @JsonProperty(value = "plans", required = true)
    @ApiModelProperty(value = "a list of plans with flows (the policies configuration)")
    private List<Plan> plans = new ArrayList<>();

    @DeploymentRequired
    @JsonProperty(value = "gravitee", required = false)
    @ApiModelProperty(value = "API's gravitee definition version")
    private String graviteeDefinitionVersion;

    @JsonProperty("deployed_at")
    @ApiModelProperty(value = "The last date (as timestamp) when the API was deployed.", example = "1581256457163")
    private Date deployedAt;

    @JsonProperty("created_at")
    @ApiModelProperty(value = "The date (as a timestamp) when the API was created.", example = "1581256457163")
    private Date createdAt;

    @JsonProperty("updated_at")
    @ApiModelProperty(value = "The last date (as a timestamp) when the API was updated.", example = "1581256457163")
    private Date updatedAt;

    @ApiModelProperty(value = "The visibility of the API regarding the portal.", example = "PUBLIC", allowableValues = "PUBLIC, PRIVATE")
    private Visibility visibility;

    @ApiModelProperty(
        value = "The status of the API regarding the gateway.",
        example = "STARTED",
        allowableValues = "INITIALIZED, STOPPED, STARTED, CLOSED"
    )
    private Lifecycle.State state;

    @JsonProperty("owner")
    @ApiModelProperty(value = "The user with role PRIMARY_OWNER on this API.")
    private PrimaryOwnerEntity primaryOwner;

    @DeploymentRequired
    @JsonProperty(value = "properties")
    @ApiModelProperty(value = "A dictionary (could be dynamic) of properties available in the API context.")
    private io.gravitee.definition.model.Properties properties;

    @DeploymentRequired
    @JsonProperty(value = "services")
    @ApiModelProperty(value = "The configuration of API services like the dynamic properties, the endpoint discovery or the healthcheck.")
    private Services services;

    @DeploymentRequired
    @ApiModelProperty(value = "the list of sharding tags associated with this API.", example = "public, private")
    private Set<String> tags;

    @ApiModelProperty(value = "the API logo encoded in base64")
    private String picture;

    @JsonProperty(value = "picture_url")
    @ApiModelProperty(
        value = "the API logo url.",
        example = "https://gravitee.mycompany.com/management/apis/6c530064-0b2c-4004-9300-640b2ce0047b/picture"
    )
    private String pictureUrl;

    @DeploymentRequired
    @JsonProperty(value = "resources")
    @ApiModelProperty(value = "The list of API resources used by policies like cache resources or oauth2")
    private List<Resource> resources = new ArrayList<>();

    @ApiModelProperty(value = "the list of categories associated with this API", example = "Product, Customer, Misc")
    private Set<String> categories;

    @ApiModelProperty(value = "the free list of labels associated with this API", example = "json, read_only, awesome")
    private List<String> labels;

    @DeploymentRequired
    @JsonProperty(value = "path_mappings")
    @ApiModelProperty(
        value = "A list of paths used to aggregate data in analytics",
        example = "/products/:productId, /products/:productId/media"
    )
    private Set<String> pathMappings = new HashSet<>();

    @JsonIgnore
    private Map<String, Object> metadata = new HashMap<>();

    @DeploymentRequired
    @JsonProperty(value = "response_templates")
    @ApiModelProperty(
        value = "A map that allows you to configure the output of a request based on the event throws by the gateway. Example : Quota exceeded, api-ky is missing, ..."
    )
    private Map<String, Map<String, ResponseTemplate>> responseTemplates;

    @JsonProperty(value = "lifecycle_state")
    private ApiLifecycleState lifecycleState;

    @JsonProperty(value = "workflow_state")
    private WorkflowState workflowState;

    @JsonProperty("disable_membership_notifications")
    private boolean disableMembershipNotifications;

    private List<ApiEntrypointEntity> entrypoints;

    @ApiModelProperty(value = "the API background encoded in base64")
    private String background;

    @JsonProperty(value = "background_url")
    @ApiModelProperty(
        value = "the API background url.",
        example = "https://gravitee.mycompany.com/management/apis/6c530064-0b2c-4004-9300-640b2ce0047b/background"
    )
    private String backgroundUrl;

    @JsonIgnore
    private String referenceType;

    @JsonIgnore
    private String referenceId;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getReferenceType() {
        return this.referenceType;
    }

    @Override
    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    @Override
    public String getReferenceId() {
        return referenceId;
    }

    @Override
    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Lifecycle.State getState() {
        return state;
    }

    public void setState(Lifecycle.State state) {
        this.state = state;
    }

    public Proxy getProxy() {
        return proxy;
    }

    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }

    public Map<String, List<Rule>> getPaths() {
        return paths;
    }

    public void setPaths(Map<String, List<Rule>> paths) {
        this.paths = paths;
    }

    public PrimaryOwnerEntity getPrimaryOwner() {
        return primaryOwner;
    }

    public void setPrimaryOwner(PrimaryOwnerEntity primaryOwner) {
        this.primaryOwner = primaryOwner;
    }

    public Services getServices() {
        return services;
    }

    public void setServices(Services services) {
        this.services = services;
    }

    public Properties getProperties() {
        return properties;
    }

    @JsonIgnore
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    @JsonSetter("properties")
    public void setPropertyList(List<Property> properties) {
        this.properties = new Properties();
        this.properties.setProperties(properties);
    }

    @JsonGetter("properties")
    public List<Property> getPropertyList() {
        if (properties != null) {
            return properties.getProperties();
        }
        return Collections.emptyList();
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public Date getDeployedAt() {
        return deployedAt;
    }

    public void setDeployedAt(Date deployedAt) {
        this.deployedAt = deployedAt;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public void setPictureUrl(String pictureUrl) {
        this.pictureUrl = pictureUrl;
    }

    public List<Resource> getResources() {
        return resources;
    }

    public void setResources(List<Resource> resources) {
        this.resources = resources;
    }

    public Set<String> getCategories() {
        return categories;
    }

    public void setCategories(Set<String> categories) {
        this.categories = categories;
    }

    public Set<String> getGroups() {
        return groups;
    }

    public void setGroups(Set<String> groups) {
        this.groups = groups;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public Set<String> getPathMappings() {
        return pathMappings;
    }

    public void setPathMappings(Set<String> pathMappings) {
        this.pathMappings = pathMappings;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public Map<String, Map<String, ResponseTemplate>> getResponseTemplates() {
        return responseTemplates;
    }

    public void setResponseTemplates(Map<String, Map<String, ResponseTemplate>> responseTemplates) {
        this.responseTemplates = responseTemplates;
    }

    public ApiLifecycleState getLifecycleState() {
        return lifecycleState;
    }

    public void setLifecycleState(ApiLifecycleState lifecycleState) {
        this.lifecycleState = lifecycleState;
    }

    public WorkflowState getWorkflowState() {
        return workflowState;
    }

    public void setWorkflowState(WorkflowState workflowState) {
        this.workflowState = workflowState;
    }

    public List<ApiEntrypointEntity> getEntrypoints() {
        return entrypoints;
    }

    public void setEntrypoints(List<ApiEntrypointEntity> entrypoints) {
        this.entrypoints = entrypoints;
    }

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public boolean isDisableMembershipNotifications() {
        return disableMembershipNotifications;
    }

    public void setDisableMembershipNotifications(boolean disableMembershipNotifications) {
        this.disableMembershipNotifications = disableMembershipNotifications;
    }

    public String getBackground() {
        return background;
    }

    public void setBackground(String background) {
        this.background = background;
    }

    public String getBackgroundUrl() {
        return backgroundUrl;
    }

    public void setBackgroundUrl(String backgroundUrl) {
        this.backgroundUrl = backgroundUrl;
    }

    public List<Flow> getFlows() {
        return flows;
    }

    public void setFlows(List<Flow> flows) {
        this.flows = flows;
    }

    public List<Plan> getPlans() {
        return plans;
    }

    public void setPlans(List<Plan> plans) {
        this.plans = plans;
    }

    public String getGraviteeDefinitionVersion() {
        return graviteeDefinitionVersion;
    }

    public void setGraviteeDefinitionVersion(String graviteeDefinitionVersion) {
        this.graviteeDefinitionVersion = graviteeDefinitionVersion;
    }

    public FlowMode getFlowMode() {
        return flowMode;
    }

    public void setFlowMode(FlowMode flowMode) {
        this.flowMode = flowMode;
    }

    public String getCrossId() {
        return crossId;
    }

    public void setCrossId(String crossId) {
        this.crossId = crossId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApiEntity api = (ApiEntity) o;
        return Objects.equals(id, api.id) && Objects.equals(version, api.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, version);
    }

    public String toString() {
        return (
            "ApiEntity{" +
            "id='" +
            id +
            '\'' +
            ", crossId='" +
            crossId +
            '\'' +
            ", name='" +
            name +
            '\'' +
            ", version='" +
            version +
            '\'' +
            ", description='" +
            description +
            '\'' +
            ", proxy=" +
            proxy +
            ", paths=" +
            paths +
            ", createdAt=" +
            createdAt +
            ", updatedAt=" +
            updatedAt +
            ", visibility=" +
            visibility +
            ", state=" +
            state +
            ", primaryOwner=" +
            primaryOwner +
            ", tags=" +
            tags +
            ", category=" +
            categories +
            ", groups=" +
            groups +
            ", pathMappings=" +
            pathMappings +
            ", lifecycleState=" +
            lifecycleState +
            ", workflowState=" +
            workflowState +
            ", disableMembershipNotifications=" +
            disableMembershipNotifications +
            ", graviteeDefinitionVersion=" +
            graviteeDefinitionVersion +
            ", flowMode=" +
            flowMode +
            '}'
        );
    }
}
