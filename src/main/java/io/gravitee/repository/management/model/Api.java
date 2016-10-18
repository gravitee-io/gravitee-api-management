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
package io.gravitee.repository.management.model;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class Api {

    /**
     * The api ID.
     */
    private String id;

    /**
     * The api name.
     */
    private String name;

    /**
     * the api description.
     */
    private String description;

    /**
     * The api version.
     */
    private String version;

    /**
     * The api JSON definition
     */
    private String definition;
    
    /**
     * The api deployment date
     */
    private Date deployedAt;

    /**
     * The Api creation date
     */
    private Date createdAt;

    /**
     * The Api last updated date
     */
    private Date updatedAt;

    /**
     * The api visibility
     */
    private Visibility visibility;

    /**
     * The current api life cycle state.
     */
    private LifecycleState lifecycleState = LifecycleState.STOPPED;

    /**
     * The api picture
     */
    private String picture;

    /**
     * the api group, may be null
     */
    private String group;

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public LifecycleState getLifecycleState() {
        return lifecycleState;
    }

    public void setLifecycleState(LifecycleState lifecycleState) {
        this.lifecycleState = lifecycleState;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public Date getDeployedAt() {
        return deployedAt;
    }

    public void setDeployedAt(Date deployedAt) {
        this.deployedAt = deployedAt;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Api api = (Api) o;
        return Objects.equals(name, api.name) &&
                Objects.equals(version, api.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, version);
    }

    public String toString() {
        return "Api{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            ", description='" + description + '\'' +
            ", version='" + version + '\'' +
            ", definition='" + definition + '\'' +
            ", createdAt=" + createdAt +
            ", updatedAt=" + updatedAt +
            ", visibility=" + visibility +
            ", lifecycleState=" + lifecycleState +
            '}';
    }
}