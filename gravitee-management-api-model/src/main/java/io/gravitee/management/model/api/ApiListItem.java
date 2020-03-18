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
package io.gravitee.management.model.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.management.model.PrimaryOwnerEntity;
import io.gravitee.management.model.Visibility;
import io.gravitee.management.model.WorkflowState;
import io.swagger.annotations.ApiModelProperty;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiListItem {
    @ApiModelProperty(
            value = "Api's uuid.",
            example = "00f8c9e7-78fc-4907-b8c9-e778fc790750")
    private String id;

    @ApiModelProperty(
            value = "Api's name. Duplicate names can exists.",
            example = "My Api")
    private String name;

    @ApiModelProperty(
            value = "Api's version. It's a simple string only used in the portal.",
            example = "v1.0")
    private String version;

    @ApiModelProperty(
            value = "API's description. A short description of your API.",
            example = "I can use a hundred characters to describe this API.")
    private String description;

    @JsonProperty("created_at")
    @ApiModelProperty(
            value = "The date (as a timestamp) when the API was created.",
            example = "1581256457163")
    private Date createdAt;

    @JsonProperty("updated_at")
    @ApiModelProperty(
            value = "The last date (as a timestamp) when the API was updated.",
            example = "1581256457163")
    private Date updatedAt;

    @ApiModelProperty(
            value = "The visibility of the API regarding the portal.",
            example = "PUBLIC",
            allowableValues = "PUBLIC, PRIVATE")
    private Visibility visibility;

    @ApiModelProperty(
            value = "The status of the API regarding the gateway.",
            example = "STARTED",
            allowableValues = "INITIALIZED, STOPPED, STARTED, CLOSED")
    private Lifecycle.State state;

    @JsonProperty("owner")
    @ApiModelProperty(
            value = "The user with role PRIMARY_OWNER on this API.")
    private PrimaryOwnerEntity primaryOwner;

    private String role;

    @JsonProperty(value = "picture_url")
    @ApiModelProperty(
            value = "the API logo url.",
            example = "https://gravitee.mycompany.com/management/apis/6c530064-0b2c-4004-9300-640b2ce0047b/picture")
    private String pictureUrl;

    @JsonProperty(value = "virtual_hosts")
    private List<VirtualHost> virtualHosts;

    @ApiModelProperty(
            value = "the list of views associated with this API",
            dataType = "java.util.List",
            example = "Product, Customer, Misc")
    private Set<String> views;

    @ApiModelProperty(
            value = "the free list of labels associated with this API",
            dataType = "java.util.List",
            example = "json, read_only, awesome")
    private List<String> labels;

    @ApiModelProperty(
            value = "true if the user can administrate the API, false otherwise",
            example = "true")
    private boolean manageable;

    @ApiModelProperty(
            value = "How consumers have evaluated the API (between 0 to 5)",
            example = "4")
    private Double rate;

    @ApiModelProperty(
            value = "How many consumers have evaluated the API",
            example = "4")
    private int numberOfRatings;

    @ApiModelProperty(
            value = "the list of sharding tags associated with this API.",
            dataType = "java.util.List",
            example = "public, private")
    private Set<String> tags;

    @JsonProperty(value = "lifecycle_state")
    private ApiLifecycleState lifecycleState;

    @JsonProperty(value = "workflow_state")
    private WorkflowState workflowState;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Lifecycle.State getState() {
        return state;
    }

    public void setState(Lifecycle.State state) {
        this.state = state;
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

    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    public PrimaryOwnerEntity getPrimaryOwner() {
        return primaryOwner;
    }

    public void setPrimaryOwner(PrimaryOwnerEntity primaryOwner) {
        this.primaryOwner = primaryOwner;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public void setPictureUrl(String pictureUrl) {
        this.pictureUrl = pictureUrl;
    }

    public List<VirtualHost> getVirtualHosts() {
        return virtualHosts;
    }

    public void setVirtualHosts(List<VirtualHost> virtualHosts) {
        this.virtualHosts = virtualHosts;
    }

    public Set<String> getViews() {
        return views;
    }

    public void setViews(Set<String> views) {
        this.views = views;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public boolean isManageable() {
        return manageable;
    }

    public void setManageable(boolean manageable) {
        this.manageable = manageable;
    }

    public Double getRate() {
        return rate;
    }

    public void setRate(Double rate) {
        this.rate = rate;
    }

    public int getNumberOfRatings() {
        return numberOfRatings;
    }

    public void setNumberOfRatings(int numberOfRatings) {
        this.numberOfRatings = numberOfRatings;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApiListItem that = (ApiListItem) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, version);
    }

    public String toString() {
        return "ApiListItem{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            ", version='" + version + '\'' +
            ", description='" + description + '\'' +
            ", createdAt=" + createdAt +
            ", updatedAt=" + updatedAt +
            ", visibility=" + visibility +
            ", state=" + state +
            ", primaryOwner=" + primaryOwner +
            ", role=" + role +
            ", manageable=" + manageable +
            ", rate=" + rate +
            ", numberOfRatings=" + numberOfRatings +
            ", tags=" + tags +
            ", lifecycleState=" + lifecycleState +
            ", workflowState=" + workflowState +
            '}';
    }
}
