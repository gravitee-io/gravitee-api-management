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
import java.util.Map;
import java.util.Objects;

public class Integration {

    public enum AuditEvent implements Audit.AuditEvent {
        INTEGRATION_CREATED,
        INTEGRATION_UPDATED,
        INTEGRATION_DELETED,
        INTEGRATION_ACTIVATED,
        INTEGRATION_DEACTIVATED,
    }

    /**
     * Integration ID
     */
    private String id;

    /**
     * The ID of the environment the integration is attached to
     */
    private String environmentId;

    /**
     * Integration name
     */
    private String name;

    /**
     * Integration description
     */
    private String description;

    /**
     * Integration type
     */
    private IntegrationType type;

    /**
     * Integration configuration
     */
    private Map<String, Object> configuration;

    /**
     * Integration creation date
     */
    private Date createdAt;

    /**
     * Integration last updated date
     */
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

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(String environmentId) {
        this.environmentId = environmentId;
    }

    public IntegrationType getType() {
        return type;
    }

    public void setType(IntegrationType type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Integration that = (Integration) o;
        return Objects.equals(id, that.id) && Objects.equals(environmentId, that.environmentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, environmentId);
    }
}
