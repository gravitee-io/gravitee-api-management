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
package io.gravitee.rest.api.model.configuration.identity.am;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.gravitee.rest.api.model.configuration.identity.IdentityProviderType;
import io.gravitee.rest.api.model.configuration.identity.SocialIdentityProviderEntity;

import java.util.List;
import java.util.Map;

public class AMIdentityProviderEntity extends SocialIdentityProviderEntity {

    private static final String AUTHORIZATION_URL = "/oauth/authorize";
    private static final String TOKEN_URL = "/oauth/token";
    private static final String LOGOUT_URL = "/logout?target_url=";
    private static final String TOKEN_INTROSPECTION_URL = "/oauth/introspect";
    private static final String USER_INFO_URL = "/oidc/userinfo";
    private static final String ACCESS_TOKEN_PROPERTY = "access_token";
    private static final String AUTHORIZATION_HEADER = "Bearer %s";

    @JsonIgnore
    private String discoveryEndpoint;

    private List<String> scopes;

    private String color;

    @JsonIgnore
    private final String serverUrl;

    @JsonIgnore
    private Map<String, String> userProfileMapping;

    public AMIdentityProviderEntity(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    @Override
    public IdentityProviderType getType() {
        return IdentityProviderType.GRAVITEEIO_AM;
    }

    public String getDiscoveryEndpoint() {
        return discoveryEndpoint;
    }

    public void setDiscoveryEndpoint(String discoveryEndpoint) {
        this.discoveryEndpoint = discoveryEndpoint;
    }

    public String getTokenEndpoint() {
        return serverUrl + TOKEN_URL;
    }

    @Override
    public String getAuthorizationEndpoint() {
        return serverUrl + AUTHORIZATION_URL;
    }

    @Override
    public String getUserLogoutEndpoint() {
        return serverUrl + LOGOUT_URL;
    }

    public String getUserInfoEndpoint() {
        return serverUrl + USER_INFO_URL;
    }

    @Override
    public String getTokenIntrospectionEndpoint() {
        return serverUrl + TOKEN_INTROSPECTION_URL;
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

    public Map<String, String> getUserProfileMapping() {
        return userProfileMapping;
    }

    public void setUserProfileMapping(Map<String, String> userProfileMapping) {
        this.userProfileMapping = userProfileMapping;
    }
}
