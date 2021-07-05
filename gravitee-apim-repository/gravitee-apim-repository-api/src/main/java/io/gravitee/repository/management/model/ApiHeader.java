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
package io.gravitee.repository.management.model;

import java.util.Date;
import java.util.Objects;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiHeader {
    public enum AuditEvent implements Audit.AuditEvent {
        API_HEADER_CREATED, API_HEADER_UPDATED, API_HEADER_DELETED
    }
    /**
     * The api ID.
     */
    private String id;
    
    /**
     * The ID of the environment the api is attached to
     */
    private String environmentId;
    
    /**
     * The api name.
     */
    private String name;

    /**
     * the api description.
     */
    private String value;

    /**
     * The api version.
     */
    private int order;

    /**
     * The Api creation date
     */
    private Date createdAt;

    /**
     * The Api last updated date
     */
    private Date updatedAt;

    public ApiHeader(){}

    public ApiHeader(ApiHeader cloned) {
        this.id = cloned.id;
        this.environmentId = cloned.environmentId;
        this.name = cloned.name;
        this.value = cloned.value;
        this.order = cloned.order;
        this.createdAt = cloned.createdAt;
        this.updatedAt = cloned.updatedAt;
    }

    public String getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(String environmentId) {
        this.environmentId = environmentId;
    }

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

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApiHeader api = (ApiHeader) o;
        return Objects.equals(id, api.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public String toString() {
        return "ApiHeader{" +
                "id='" + id + '\'' +
                ", environmentId='" + environmentId + '\'' +
                ", name='" + name + '\'' +
                ", value='" + value + '\'' +
                ", order='" + order + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
