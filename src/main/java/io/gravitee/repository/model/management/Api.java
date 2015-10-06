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
package io.gravitee.repository.model.management;

import java.net.URI;
import java.util.Date;
import java.util.Objects;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class Api {

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
     * The uri used to expose api.
     */
    private URI publicURI;
    
    /**
     * The uri of consumed api.
     */
    private URI targetURI;

    /**
     * The Api creation date
     */
    private Date createdAt;
    
    /**
     * The Api last updated date
     */
    private Date updatedAt;
    
    /**
     * The api owner entity type (user or team)
     */
    private OwnerType ownerType;
    
    /**
     * The api owner entity name (user name or team name)
     */
    private String owner;

    /**
     * The api user name creator
     */
    private String creator;

    /**
     * The api visibility (private of for all users)
     */
    private boolean privateApi;

    /**
     * The current api life cycle state.
     */
    private LifecycleState lifecycleState = LifecycleState.STOPPED;

    /**
     * The api JSON descriptor
     */
    private boolean jsonDescriptor;

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

    public URI getPublicURI() {
        return publicURI;
    }

    public void setPublicURI(URI publicURI) {
        this.publicURI = publicURI;
    }

    public URI getTargetURI() {
        return targetURI;
    }

    public void setTargetURI(URI targetURI) {
        this.targetURI = targetURI;
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

    public boolean isPrivateApi() {
        return privateApi;
    }

    public void setPrivateApi(boolean privateApi) {
        this.privateApi = privateApi;
    }

    public OwnerType getOwnerType() {
		return ownerType;
	}

	public void setOwnerType(OwnerType ownerType) {
		this.ownerType = ownerType;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}
	
    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isJsonDescriptor() {
        return jsonDescriptor;
    }

    public void setJsonDescriptor(boolean jsonDescriptor) {
        this.jsonDescriptor = jsonDescriptor;
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

	@Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Api{");
        sb.append("name='").append(name).append('\'');
        sb.append(", version='").append(version).append('\'');
        sb.append(", lifecycleState=").append(lifecycleState);
        sb.append(", jsonDescriptor=").append(jsonDescriptor);
        sb.append('}');
        return sb.toString();
    }
}
