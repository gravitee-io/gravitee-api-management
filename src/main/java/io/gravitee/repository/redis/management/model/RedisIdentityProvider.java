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
package io.gravitee.repository.redis.management.model;

import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RedisIdentityProvider {

    /**
     * Identity provider ID
     */
    private String id;

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
    private String type;

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

    /**
     * Identity provider creation date
     */
    private long createdAt;

    /**
     * Identity provider last updated date
     */
    private long updatedAt;

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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
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

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RedisIdentityProvider redisPlan = (RedisIdentityProvider) o;

        return id.equals(redisPlan.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
