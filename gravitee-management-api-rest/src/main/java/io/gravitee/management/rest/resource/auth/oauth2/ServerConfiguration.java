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
package io.gravitee.management.rest.resource.auth.oauth2;

import java.util.Collections;
import java.util.List;

/**
 * @author Christophe LANNOY (chrislannoy.java at gmail.com)
 */
public class ServerConfiguration {

    private String clientSecret;
    private String tokenEndpoint;
    private String accessTokenProperty;
    private String userInfoEndpoint;
    private String authorizationHeader;
    private UserMapping userMapping = new UserMapping();
    private List<ExpressionMapping> groupsMapping = Collections.emptyList();

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public void setTokenEndpoint(String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    public String getAccessTokenProperty() {
        return accessTokenProperty;
    }

    public void setAccessTokenProperty(String accessTokenProperty) {
        this.accessTokenProperty = accessTokenProperty;
    }

    public String getUserInfoEndpoint() {
        return userInfoEndpoint;
    }

    public void setUserInfoEndpoint(String userInfoEndpoint) {
        this.userInfoEndpoint = userInfoEndpoint;
    }

    public String getAuthorizationHeader() {
        return authorizationHeader;
    }

    public void setAuthorizationHeader(String authorizationHeader) {
        this.authorizationHeader = authorizationHeader;
    }

    public UserMapping getUserMapping() {
        return userMapping;
    }

    public void setUserMapping(UserMapping userMapping) {
        this.userMapping = userMapping;
    }

    public List<ExpressionMapping> getGroupsMapping() {
        return groupsMapping;
    }

    public void setGroupsMapping(List<ExpressionMapping> groupsMapping) {
        this.groupsMapping = groupsMapping;
    }

}
