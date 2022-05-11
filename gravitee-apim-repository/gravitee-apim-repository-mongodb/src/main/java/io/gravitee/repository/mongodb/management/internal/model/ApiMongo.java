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
package io.gravitee.repository.mongodb.management.internal.model;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Mongo model for Api
 *
 * @author Loic DASSONVILLE (loic.dassonville at gmail.com)
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Document(collection = "apis")
public class ApiMongo extends Auditable {

    @Id
    private String id;

    /**
     * The api crossId uniquely identifies an API across environments.
     * Apis promoted between environments will share the same crossId.
     */
    private String crossId;

    /**
     * The origin of the api (management, kubernetes, ...).
     */
    private String origin;

    /**
     * How the api is managed by the origin (fully, api_definition_only, ...)
     */
    private String mode;

    @Field("name")
    private String name;

    private String environmentId;

    private String version;

    private String description;

    private String definition;

    private String lifecycleState;

    private String visibility;

    private Date deployedAt;

    private String picture;

    private Set<String> groups;

    private Set<String> categories;

    private List<String> labels;

    private List<ApiMetadataMongo> metadatas;

    private String apiLifecycleState;

    private boolean disableMembershipNotifications;

    private String background;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(String environmentId) {
        this.environmentId = environmentId;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getLifecycleState() {
        return lifecycleState;
    }

    public void setLifecycleState(String lifecycleState) {
        this.lifecycleState = lifecycleState;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
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

    public Set<String> getGroups() {
        return groups;
    }

    public void setGroups(Set<String> groups) {
        this.groups = groups;
    }

    public Set<String> getCategories() {
        return categories;
    }

    public void setCategories(Set<String> categories) {
        this.categories = categories;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public List<ApiMetadataMongo> getMetadatas() {
        return metadatas;
    }

    public void setMetadatas(List<ApiMetadataMongo> metadatas) {
        this.metadatas = metadatas;
    }

    public String getApiLifecycleState() {
        return apiLifecycleState;
    }

    public void setApiLifecycleState(String apiLifecycleState) {
        this.apiLifecycleState = apiLifecycleState;
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
        ApiMongo api = (ApiMongo) o;
        return Objects.equals(id, api.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Api{");
        sb.append("id='").append(id).append('\'');
        sb.append(", crossId='").append(crossId).append('\'');
        sb.append(", origin='").append(crossId).append('\'');
        sb.append(", mode='").append(crossId).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", environmentId='").append(environmentId).append('\'');
        sb.append(", version='").append(version).append('\'');
        sb.append(", state='").append(lifecycleState).append('\'');
        sb.append(", visibility='").append(visibility).append('\'');
        sb.append(", groups='").append(groups).append('\'');
        sb.append(", metadatas='").append(metadatas).append('\'');
        sb.append(", apiLifecycleState='").append(apiLifecycleState).append('\'');
        sb.append(", disableMembershipNotifications='").append(disableMembershipNotifications).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }
}
