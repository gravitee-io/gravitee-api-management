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
package io.gravitee.management.model;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.model.Path;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.plugins.resources.Resource;
import io.gravitee.definition.model.services.Services;

import javax.validation.constraints.NotNull;
import java.util.*;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@JsonFilter("apiMembershipTypeFilter")
public class ApiEntity {

    private String id;
    private String name;
    private String version;
    private String description;

    @NotNull
    @DeploymentRequired
    @MembershipTypesAllowed({"PRIMARY_OWNER", "OWNER"})
    @JsonProperty(value = "proxy", required = true)
    private Proxy proxy;

    @DeploymentRequired
    @MembershipTypesAllowed({"PRIMARY_OWNER", "OWNER"})
    @JsonProperty(value = "paths", required = true)
    private Map<String, Path> paths = new HashMap<>();
    
    @JsonProperty("deployed_at")
    private Date deployedAt;

    @JsonProperty("created_at")
    private Date createdAt;

    @JsonProperty("updated_at")
    private Date updatedAt;

    @DeploymentRequired
    private Visibility visibility;

    private Lifecycle.State state;

    @JsonProperty("owner")
    private PrimaryOwnerEntity primaryOwner;

    @DeploymentRequired
    @MembershipTypesAllowed({"PRIMARY_OWNER", "OWNER"})
    @JsonProperty(value = "properties")
    private Map<String, String> properties;

    private MembershipType permission;

    @DeploymentRequired
    @MembershipTypesAllowed({"PRIMARY_OWNER", "OWNER"})
    @JsonProperty(value = "services")
    private Services services;

    @DeploymentRequired
    @MembershipTypesAllowed({"PRIMARY_OWNER", "OWNER"})
    private Set<String> tags;
    
    private String picture;

    @JsonProperty(value = "picture_url")
    private String pictureUrl;

    @DeploymentRequired
    @MembershipTypesAllowed({"PRIMARY_OWNER", "OWNER"})
    @JsonProperty(value = "resources")
    private List<Resource> resources = new ArrayList<>();

    private Set<String> views;

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

    public MembershipType getPermission() {
        return permission;
    }

    public void setPermission(MembershipType permission) {
        this.permission = permission;
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

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
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
            ", permission=" + permission +
            ", tags=" + tags +
            ", view=" + views +
            '}';
    }
}
