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

import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RedisClientRegistrationProvider {

    /**
     * Client registration provider ID
     */
    private String id;

    /**
     * Client registration provider name
     */
    private String name;

    /**
     * Client registration provider description
     */
    private String description;

    /**
     * Client registration OIDC discovery endpoint
     */
    private String discoveryEndpoint;

    private String initialAccessTokenType;

    /**
     * Client registration OIDC Client_ID
     */
    private String clientId;

    /**
     * Client registration OIDC Client_secret
     */
    private String clientSecret;

    /**
     * Client registration OIDC scopes
     */
    private List<String> scopes;

    private String initialAccessToken;

    private boolean renewClientSecretSupport;

    private String renewClientSecretEndpoint;

    private String renewClientSecretMethod;

    /**
     * Client registration provider creation date
     */
    private long createdAt;

    /**
     * Client registration provider last updated date
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

    public String getDiscoveryEndpoint() {
        return discoveryEndpoint;
    }

    public void setDiscoveryEndpoint(String discoveryEndpoint) {
        this.discoveryEndpoint = discoveryEndpoint;
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

    public String getInitialAccessTokenType() {
        return initialAccessTokenType;
    }

    public void setInitialAccessTokenType(String initialAccessTokenType) {
        this.initialAccessTokenType = initialAccessTokenType;
    }

    public String getInitialAccessToken() {
        return initialAccessToken;
    }

    public void setInitialAccessToken(String initialAccessToken) {
        this.initialAccessToken = initialAccessToken;
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

        RedisClientRegistrationProvider redisPlan = (RedisClientRegistrationProvider) o;

        return id.equals(redisPlan.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
