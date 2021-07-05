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
import java.util.Map;
import java.util.Objects;

public class IdentityProvider {

    public enum AuditEvent implements Audit.AuditEvent {
        IDENTITY_PROVIDER_CREATED, IDENTITY_PROVIDER_UPDATED, IDENTITY_PROVIDER_DELETED, IDENTITY_PROVIDER_ACTIVATED, IDENTITY_PROVIDER_DEACTIVATED
    }

    /**
     * Identity provider ID
     */
    private String id;

    /**
     * Identity provider organization Id
     */
    private String organizationId;

    /**
     * Identity provider name
     */
    private String name;

    /**
     * Identity provider description
     */
    private String description;

    /**
     * Is the identity provider enabled
     */
    private boolean enabled;

    /**
     * Identity provider type
     */
    private IdentityProviderType type;

    /**
     * Identity provider configuration
     */
    private Map<String, Object> configuration;

    /**
     * Identity provider group mapping
     */
    private Map<String, String[]> groupMappings;

    /**
     * Identity provider role mapping
     */
    private Map<String, String[]> roleMappings;

    /**
     * Identity provider user profile mapping
     */
    private Map<String, String> userProfileMapping;

    private Boolean emailRequired;

    private Boolean syncMappings;

    /**
     * Identity provider creation date
     */
    private Date createdAt;

    /**
     * Identity provider last updated date
     */
    private Date updatedAt;

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
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

    public Boolean getSyncMappings() {
        return syncMappings;
    }

    public void setSyncMappings(Boolean syncMappings) {
        this.syncMappings = syncMappings;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IdentityProvider that = (IdentityProvider) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(organizationId, that.organizationId);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, organizationId);
    }
}
