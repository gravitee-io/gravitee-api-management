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
import java.util.Objects;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class Team {

    /**
     * The shorthand name of the team.
     */
    private String name;

    /**
     * The description of the team.
     */
    private String description;

    /**
     * The team can only be visible for members.
     */
    private boolean privateTeam;

    /**
     * The publicly visible email address.
     */
    private String email;

    /**
     * The team creation date
     */
    private Date createdAt;
    
    /**
     * The team last updated date
     */
    private Date updatedAt;
    
    
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

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Team)) {
            return false;
        }
        Team team = (Team) o;
        return Objects.equals(name, team.name);
    }

    public int hashCode() {
        return Objects.hash(name);
    }

    public String toString() {
        return "Team{" +
            "name='" + name + '\'' +
            ", description='" + description + '\'' +
            ", privateTeam=" + privateTeam +
            ", email='" + email + '\'' +
            ", createdAt=" + createdAt +
            ", updatedAt=" + updatedAt +
            '}';
    }
}
