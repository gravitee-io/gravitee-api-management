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
package io.gravitee.repository.management.model;

import java.util.Date;
import java.util.Objects;

/**
 * @author GraviteeSource Team
 */
public class AmConnection {

    private String organizationId;
    private String baseUrl;
    private String serviceAccountAccessTokenEncrypted;
    private String environmentId;
    private String defaultDomainId;
    private String defaultDomainHrid;
    private String gatewayUrl;
    private Date updatedAt;

    public AmConnection() {}

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getServiceAccountAccessTokenEncrypted() {
        return serviceAccountAccessTokenEncrypted;
    }

    public void setServiceAccountAccessTokenEncrypted(String serviceAccountAccessTokenEncrypted) {
        this.serviceAccountAccessTokenEncrypted = serviceAccountAccessTokenEncrypted;
    }

    public String getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(String environmentId) {
        this.environmentId = environmentId;
    }

    public String getDefaultDomainId() {
        return defaultDomainId;
    }

    public void setDefaultDomainId(String defaultDomainId) {
        this.defaultDomainId = defaultDomainId;
    }

    public String getDefaultDomainHrid() {
        return defaultDomainHrid;
    }

    public void setDefaultDomainHrid(String defaultDomainHrid) {
        this.defaultDomainHrid = defaultDomainHrid;
    }

    public String getGatewayUrl() {
        return gatewayUrl;
    }

    public void setGatewayUrl(String gatewayUrl) {
        this.gatewayUrl = gatewayUrl;
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
        AmConnection that = (AmConnection) o;
        return Objects.equals(organizationId, that.organizationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(organizationId);
    }
}
