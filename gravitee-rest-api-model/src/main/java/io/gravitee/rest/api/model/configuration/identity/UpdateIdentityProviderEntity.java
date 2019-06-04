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

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class UpdateIdentityProviderEntity {

    @NotNull
    private String name;

    private String description;

    @NotNull
    private Map<String, Object> configuration;

    @NotNull
    private boolean enabled;

    private List<GroupMappingEntity> groupMappings;

    private List<RoleMappingEntity> roleMappings;

    private Map<String, String> userProfileMapping;

    private boolean emailRequired;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UpdateIdentityProviderEntity that = (UpdateIdentityProviderEntity) o;

        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
