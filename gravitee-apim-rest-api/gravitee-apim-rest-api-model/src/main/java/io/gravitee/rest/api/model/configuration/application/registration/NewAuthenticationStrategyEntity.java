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
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class NewAuthenticationStrategyEntity {

    @NotNull
    private String name;

    @JsonProperty("display_name")
    private String displayName;

    private String description;

    @NotNull
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

    @Override
    public String toString() {
        return "NewAuthenticationStrategyEntity{name='" + name + "', type=" + type + '}';
    }
}
