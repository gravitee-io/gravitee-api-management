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

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class SocialIdentityProviderEntity {

    private static final String DEFAULT_AUTHORIZATION_HEADER = "Bearer %s";
    private static final String DEFAULT_SCOPE_DELIMITER = " ";

    private String id;

    private String name;

    private String description;

    private String clientId;

    private boolean emailRequired;

    @JsonIgnore
    private String clientSecret;

    @JsonIgnore
    private List<GroupMappingEntity> groupMappings;

    @JsonIgnore
    private List<RoleMappingEntity> roleMappings;

    public static class UserProfile {
        public static final String ID = "id";
        public static final String SUB = "sub";
        public static final String FIRSTNAME = "firstname";
        public static final String LASTNAME = "lastname";
        public static final String PICTURE = "picture";
        public static final String EMAIL = "email";
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @JsonIgnore
    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getScopeDelimiter() {
        return DEFAULT_SCOPE_DELIMITER;
    }

    @JsonIgnore
    public String getAuthorizationHeader() {
        return DEFAULT_AUTHORIZATION_HEADER;
    }

    public abstract IdentityProviderType getType();

    public abstract String getAuthorizationEndpoint();

    public String getTokenIntrospectionEndpoint() {
        return null;
    }

    @JsonIgnore
    public abstract String getTokenEndpoint();

    @JsonIgnore
    public abstract String getUserInfoEndpoint();

    public String getUserLogoutEndpoint() {
        return null;
    }

    public abstract List<String> getRequiredUrlParams();

    public abstract List<String> getOptionalUrlParams();

    public abstract List<String> getScopes();

    public abstract String getDisplay();

    public abstract String getColor();

    public abstract String getIcon();

    @JsonIgnore
    public abstract Map<String, String> getUserProfileMapping();

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

    public boolean isEmailRequired() {
        return emailRequired;
    }

    public void setEmailRequired(boolean emailRequired) {
        this.emailRequired = emailRequired;
    }
}
