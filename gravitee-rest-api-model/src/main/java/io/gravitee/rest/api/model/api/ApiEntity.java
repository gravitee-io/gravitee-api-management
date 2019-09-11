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

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.model.Path;
import io.gravitee.definition.model.Properties;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.ResponseTemplates;
import io.gravitee.definition.model.plugins.resources.Resource;
import io.gravitee.definition.model.services.Services;
import io.gravitee.rest.api.model.DeploymentRequired;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.WorkflowState;
import io.gravitee.rest.api.model.search.Indexable;

import javax.validation.constraints.NotNull;
import java.util.*;

/**
 * --------------------------------------------------------------------------------------------------------------
 * --------------------------------------------------------------------------------------------------------------
 * /!\ Do not forget to update {@see io.gravitee.management.service.jackson.ser.api.ApiDefaultSerializer}
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

    private String id;
    private String name;
    private String version;
    private String description;
    private Set<String> groups;

    @NotNull
    @DeploymentRequired
    @JsonProperty(value = "proxy", required = true)
    private Proxy proxy;

    @DeploymentRequired
    @JsonProperty(value = "paths", required = true)
    private Map<String, Path> paths = new HashMap<>();

    @JsonProperty("deployed_at")
    private Date deployedAt;

    @JsonProperty("created_at")
    private Date createdAt;

    @JsonProperty("updated_at")
    private Date updatedAt;

    private Visibility visibility;

    private Lifecycle.State state;

    @JsonProperty("owner")
    private PrimaryOwnerEntity primaryOwner;

    @DeploymentRequired
    @JsonProperty(value = "properties")
    private io.gravitee.definition.model.Properties properties;

    @DeploymentRequired
    @JsonProperty(value = "services")
    private Services services;

    @DeploymentRequired
    private Set<String> tags;

    private String picture;

    @JsonProperty(value = "picture_url")
    private String pictureUrl;

    @DeploymentRequired
    @JsonProperty(value = "resources")
    private List<Resource> resources = new ArrayList<>();

    private Set<String> views;

    private List<String> labels;

    @DeploymentRequired
    @JsonProperty(value = "path_mappings")
    private Set<String> pathMappings = new HashSet<>();

    @JsonIgnore
    private Map<String, Object> metadata = new HashMap<>();

    @DeploymentRequired
    @JsonProperty(value = "response_templates")
    private Map<String, ResponseTemplates> responseTemplates;

    @JsonProperty(value = "lifecycle_state")
    private ApiLifecycleState lifecycleState;

    @JsonProperty(value = "workflow_state")
    private WorkflowState workflowState;

    private List<ApiEntrypointEntity> entrypoints;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public Map<String, Path> getPaths() {
        return paths;
    }

    public void setPaths(Map<String, Path> paths) {
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

    public void setProperties(Properties properties) {
        this.properties = properties;
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

    public Set<String> getViews() {
        return views;
    }

    public void setViews(Set<String> views) {
        this.views = views;
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

    public Map<String, ResponseTemplates> getResponseTemplates() {
        return responseTemplates;
    }

    public void setResponseTemplates(Map<String, ResponseTemplates> responseTemplates) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApiEntity api = (ApiEntity) o;
        return Objects.equals(id, api.id) &&
                Objects.equals(version, api.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, version);
    }

    public String toString() {
        return "ApiEntity{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", description='" + description + '\'' +
                ", proxy=" + proxy +
                ", paths=" + paths +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", visibility=" + visibility +
                ", state=" + state +
                ", primaryOwner=" + primaryOwner +
                ", tags=" + tags +
                ", view=" + views +
                ", groups=" + groups +
                ", pathMappings=" + pathMappings +
                ", lifecycleState=" + lifecycleState +
                ", workflowState=" + workflowState +
                '}';
    }
}
