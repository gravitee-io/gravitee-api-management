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
package io.gravitee.repositories.mongodb.internal.model;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import io.gravitee.repository.model.Api;

/**
 * Mongo object model for application.
 * 
 * @author Loic DASSONVILLE (loic.dassonville at gmail.com)
 */
@Document(collection="applications")
public class ApplicationMongo {

	@Id
    private String name;
    private String description;
    private String type;
    private Date createdAt;
    private Date updatedAt;
    private ApiKeyMongo key;
    
    @DBRef
    private AbstractUserMongo owner;
    
    @DBRef
    private UserMongo creator;
    
    @DBRef
    private List<ApiMongo> apis;

    
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
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

	public ApiKeyMongo getKey() {
		return key;
	}

	public void setKey(ApiKeyMongo key) {
		this.key = key;
	}

	public List<ApiMongo> getApis() {
		return apis;
	}

	public void setApis(List<ApiMongo> apis) {
		this.apis = apis;
	}

	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApplicationMongo that = (ApplicationMongo) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Application{");
        sb.append("name='").append(name).append('\'');
        sb.append(", type='").append(type).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
