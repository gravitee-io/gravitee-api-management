/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.model.configuration.application.registration;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class AuthenticationStrategyEntity {

    private String id;

    private String name;

    @JsonProperty("display_name")
    private String displayName;

    private String description;

    private AuthenticationStrategyType type;

    @JsonProperty("client_registration_provider_id")
    private String clientRegistrationProviderId;

    private List<String> scopes;

    @JsonProperty("auth_methods")
    private List<String> authMethods;

    @JsonProperty("credential_claims")
    private String credentialClaims;

    @JsonProperty("auto_approve")
    private boolean autoApprove;

    @JsonProperty("hide_credentials")
    private boolean hideCredentials;

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

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public AuthenticationStrategyType getType() {
        return type;
    }

    public void setType(AuthenticationStrategyType type) {
        this.type = type;
    }

    public String getClientRegistrationProviderId() {
        return clientRegistrationProviderId;
    }

    public void setClientRegistrationProviderId(String clientRegistrationProviderId) {
        this.clientRegistrationProviderId = clientRegistrationProviderId;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    public List<String> getAuthMethods() {
        return authMethods;
    }

    public void setAuthMethods(List<String> authMethods) {
        this.authMethods = authMethods;
    }

    public String getCredentialClaims() {
        return credentialClaims;
    }

    public void setCredentialClaims(String credentialClaims) {
        this.credentialClaims = credentialClaims;
    }

    public boolean isAutoApprove() {
        return autoApprove;
    }

    public void setAutoApprove(boolean autoApprove) {
        this.autoApprove = autoApprove;
    }

    public boolean isHideCredentials() {
        return hideCredentials;
    }

    public void setHideCredentials(boolean hideCredentials) {
        this.hideCredentials = hideCredentials;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthenticationStrategyEntity that = (AuthenticationStrategyEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
