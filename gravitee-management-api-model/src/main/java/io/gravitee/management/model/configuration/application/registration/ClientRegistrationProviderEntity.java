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
package io.gravitee.management.model.configuration.application.registration;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ClientRegistrationProviderEntity {

    private String id;

    private String name;

    private String description;

    @JsonProperty("discovery_endpoint")
    private String discoveryEndpoint;

    @JsonProperty("initial_access_token_type")
    private InitialAccessTokenType initialAccessTokenType;

    @JsonProperty("client_id")
    private String clientId;

    @JsonProperty("client_secret")
    private String clientSecret;

    private List<String> scopes;

    @JsonProperty("created_at")
    private Date createdAt;

    @JsonProperty("updated_at")
    private Date updatedAt;

    @JsonProperty("initial_access_token")
    private String initialAccessToken;

    @JsonProperty("renew_client_secret_support")
    private boolean renewClientSecretSupport;

    @JsonProperty("renew_client_secret_endpoint")
    private String renewClientSecretEndpoint;

    @JsonProperty("renew_client_secret_method")
    private String renewClientSecretMethod;

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

    public String getDiscoveryEndpoint() {
        return discoveryEndpoint;
    }

    public void setDiscoveryEndpoint(String discoveryEndpoint) {
        this.discoveryEndpoint = discoveryEndpoint;
    }

    public InitialAccessTokenType getInitialAccessTokenType() {
        return initialAccessTokenType;
    }

    public void setInitialAccessTokenType(InitialAccessTokenType initialAccessTokenType) {
        this.initialAccessTokenType = initialAccessTokenType;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    public String getInitialAccessToken() {
        return initialAccessToken;
    }

    public void setInitialAccessToken(String initialAccessToken) {
        this.initialAccessToken = initialAccessToken;
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

    public boolean isRenewClientSecretSupport() {
        return renewClientSecretSupport;
    }

    public void setRenewClientSecretSupport(boolean renewClientSecretSupport) {
        this.renewClientSecretSupport = renewClientSecretSupport;
    }

    public String getRenewClientSecretEndpoint() {
        return renewClientSecretEndpoint;
    }

    public void setRenewClientSecretEndpoint(String renewClientSecretEndpoint) {
        this.renewClientSecretEndpoint = renewClientSecretEndpoint;
    }

    public String getRenewClientSecretMethod() {
        return renewClientSecretMethod;
    }

    public void setRenewClientSecretMethod(String renewClientSecretMethod) {
        this.renewClientSecretMethod = renewClientSecretMethod;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientRegistrationProviderEntity that = (ClientRegistrationProviderEntity) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
