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
package io.gravitee.management.rest.model.configuration.identity;

import java.util.Objects;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SocialIdentityProvider {

    private String id;

    private String name;

    private String type;

    private String clientId;

    private String authorizationEndpoint;

    private String [] requiredUrlParams;

    private String scopeDelimiter;

    private String [] optionalUrlParams;

    private String [] scopes;

    private String display;

    private String state;

    private String color;

    private String icon;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    public void setAuthorizationEndpoint(String authorizationEndpoint) {
        this.authorizationEndpoint = authorizationEndpoint;
    }

    public String[] getRequiredUrlParams() {
        return requiredUrlParams;
    }

    public void setRequiredUrlParams(String[] requiredUrlParams) {
        this.requiredUrlParams = requiredUrlParams;
    }

    public String getScopeDelimiter() {
        return scopeDelimiter;
    }

    public void setScopeDelimiter(String scopeDelimiter) {
        this.scopeDelimiter = scopeDelimiter;
    }

    public String[] getOptionalUrlParams() {
        return optionalUrlParams;
    }

    public void setOptionalUrlParams(String[] optionalUrlParams) {
        this.optionalUrlParams = optionalUrlParams;
    }

    public String[] getScopes() {
        return scopes;
    }

    public void setScopes(String[] scopes) {
        this.scopes = scopes;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SocialIdentityProvider that = (SocialIdentityProvider) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id);
    }
}
