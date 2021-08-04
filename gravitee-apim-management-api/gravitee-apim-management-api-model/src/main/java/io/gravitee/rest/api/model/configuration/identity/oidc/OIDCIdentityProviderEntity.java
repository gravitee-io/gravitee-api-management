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
package io.gravitee.rest.api.model.configuration.identity.oidc;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.gravitee.rest.api.model.configuration.identity.IdentityProviderType;
import io.gravitee.rest.api.model.configuration.identity.SocialIdentityProviderEntity;
import java.util.List;
import java.util.Map;

public class OIDCIdentityProviderEntity extends SocialIdentityProviderEntity {

    @JsonIgnore
    private String discoveryEndpoint;

    @JsonIgnore
    private String tokenEndpoint;

    private String authorizationEndpoint;

    @JsonIgnore
    private String userInfoEndpoint;

    private String userLogoutEndpoint;

    private String tokenIntrospectionEndpoint;

    private List<String> scopes;

    private String color;

    @JsonIgnore
    private Map<String, String> userProfileMapping;

    @Override
    public IdentityProviderType getType() {
        return IdentityProviderType.OIDC;
    }

    public String getDiscoveryEndpoint() {
        return discoveryEndpoint;
    }

    public void setDiscoveryEndpoint(String discoveryEndpoint) {
        this.discoveryEndpoint = discoveryEndpoint;
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public void setTokenEndpoint(String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    @Override
    public String getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    public void setAuthorizationEndpoint(String authorizationEndpoint) {
        this.authorizationEndpoint = authorizationEndpoint;
    }

    @Override
    public String getUserLogoutEndpoint() {
        return userLogoutEndpoint;
    }

    public void setUserLogoutEndpoint(String userLogoutEndpoint) {
        this.userLogoutEndpoint = userLogoutEndpoint;
    }

    public String getUserInfoEndpoint() {
        return userInfoEndpoint;
    }

    @Override
    public String getTokenIntrospectionEndpoint() {
        return tokenIntrospectionEndpoint;
    }

    public void setTokenIntrospectionEndpoint(String tokenIntrospectionEndpoint) {
        this.tokenIntrospectionEndpoint = tokenIntrospectionEndpoint;
    }

    @Override
    public List<String> getRequiredUrlParams() {
        return null;
    }

    @Override
    public List<String> getOptionalUrlParams() {
        return null;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    @Override
    public List<String> getScopes() {
        return this.scopes;
    }

    @Override
    public String getDisplay() {
        return null;
    }

    public void setColor(String color) {
        this.color = color;
    }

    @Override
    public String getColor() {
        return this.color;
    }

    public void setUserInfoEndpoint(String userInfoEndpoint) {
        this.userInfoEndpoint = userInfoEndpoint;
    }

    public Map<String, String> getUserProfileMapping() {
        return userProfileMapping;
    }

    public void setUserProfileMapping(Map<String, String> userProfileMapping) {
        this.userProfileMapping = userProfileMapping;
    }
}
