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
package io.gravitee.rest.api.model.configuration.identity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class IdentityProviderEntity {

    private String id;

    private String name;

    private String description;

    private IdentityProviderType type;

    private boolean enabled;

    private Map<String, Object> configuration;

    private List<GroupMappingEntity> groupMappings;

    private List<RoleMappingEntity> roleMappings;

    private Map<String, String> userProfileMapping;

    private boolean emailRequired;

    private boolean syncMappings;

    @JsonProperty("created_at")
    private Date createdAt;

    @JsonProperty("updated_at")
    private Date updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public IdentityProviderType getType() {
        return type;
    }

    public void setType(IdentityProviderType type) {
        this.type = type;
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

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<GroupMappingEntity> getGroupMappings() {
        return groupMappings;
    }

    public void setGroupMappings(List<GroupMappingEntity> groupMappings) {
        this.groupMappings = groupMappings;
    }

    public List<RoleMappingEntity> getRoleMappings() {
        return roleMappings;
    }

    public void setRoleMappings(List<RoleMappingEntity> roleMappings) {
        this.roleMappings = roleMappings;
    }

    public Map<String, String> getUserProfileMapping() {
        return userProfileMapping;
    }

    public void setUserProfileMapping(Map<String, String> userProfileMapping) {
        this.userProfileMapping = userProfileMapping;
    }

    public boolean isEmailRequired() {
        return emailRequired;
    }

    public void setEmailRequired(boolean emailRequired) {
        this.emailRequired = emailRequired;
    }

    public boolean isSyncMappings() {
        return syncMappings;
    }

    public void setSyncMappings(boolean syncMappings) {
        this.syncMappings = syncMappings;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IdentityProviderEntity that = (IdentityProviderEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
