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

import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Mongo object model for team
 * 
 * @author Loic DASSONVILLE (loic.dassonville at gmail.com)
 */
@Document(collection="teams")
public class TeamMongo extends AbstractUserMongo{

    private String description;
    private String email;
    
    /**
     * The team can only be visible for members.
     */
    private boolean privateTeam;
    
    private Date createdAt;
    private Date updatedAt;
    
    private List<TeamMemberMongo> members;
   
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

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public boolean isPrivateTeam() {
		return privateTeam;
	}

	public void setPrivateTeam(boolean privateTeam) {
		this.privateTeam = privateTeam;
	}

	public List<TeamMemberMongo> getMembers() {
		return members;
	}

	public void setMembers(List<TeamMemberMongo> members) {
		this.members = members;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public Date getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}
	
}
