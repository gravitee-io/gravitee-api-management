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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Mongo model for Api
 * 
 * @author Loic DASSONVILLE (loic.dassonville at gmail.com)
 */
@Document(collection="apis")
public class ApiMongo {
    
	@Id
    private String name;
    private String version;
    private String description;

    private String definition;

    private String lifecycleState;
    
	private boolean privateApi;
    
    private Date createdAt;
    private Date updatedAt;
    
    @DBRef 
    private AbstractUserMongo owner;
    
    @DBRef 
    private UserMongo creator;

    private List<PolicyConfigurationMongo> policies = new ArrayList<>();
    
    
    public boolean isPrivateApi() {
		return privateApi;
	}

	public void setPrivateApi(boolean privateApi) {
		this.privateApi = privateApi;
	}
    
    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
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

    public AbstractUserMongo getOwner() {
		return owner;
	}

	public void setOwner(AbstractUserMongo owner) {
		this.owner = owner;
	}

	public UserMongo getCreator() {
		return creator;
	}

	public void setCreator(UserMongo creator) {
		this.creator = creator;
	}
	
	public List<PolicyConfigurationMongo> getPolicies() {
		return policies;
	}

	public void setPolicies(List<PolicyConfigurationMongo> policies) {
		this.policies = policies;
	}

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApiMongo api = (ApiMongo) o;
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
        sb.append('}');
        return sb.toString();
    }
}
