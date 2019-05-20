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

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Document(collection = "identity_providers")
public class IdentityProviderMongo extends Auditable {

	@Id
	private String id;

    private String referenceId;
    
    private String referenceType;

	private String name;

	private String description;

	private boolean enabled;

	private String type;

	private Map<String, Object> configuration;

	private Map<String, String []> groupMappings;

	private Map<String, String []> roleMappings;

	private Map<String, String> userProfileMapping;

	private Boolean emailRequired;

	public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Map<String, String[]> getGroupMappings() {
		return groupMappings;
	}

	public void setGroupMappings(Map<String, String[]> groupMappings) {
		this.groupMappings = groupMappings;
	}

	public Map<String, String[]> getRoleMappings() {
		return roleMappings;
	}

	public void setRoleMappings(Map<String, String[]> roleMappings) {
		this.roleMappings = roleMappings;
	}

	public Map<String, Object> getConfiguration() {
		return configuration;
	}

	public void setConfiguration(Map<String, Object> configuration) {
		this.configuration = configuration;
	}

	public Map<String, String> getUserProfileMapping() {
		return userProfileMapping;
	}

	public void setUserProfileMapping(Map<String, String> userProfileMapping) {
		this.userProfileMapping = userProfileMapping;
	}

	public Boolean getEmailRequired() {
		return emailRequired;
	}

	public void setEmailRequired(Boolean emailRequired) {
		this.emailRequired = emailRequired;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		IdentityProviderMongo that = (IdentityProviderMongo) o;

		return id.equals(that.id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}
}
