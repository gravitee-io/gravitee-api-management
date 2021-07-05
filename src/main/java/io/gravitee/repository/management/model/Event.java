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

import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Event implements Serializable {

    /**
     * The event ID
     */
    private String id;

    /**
     * The list of ids of the environments the event is attached to
     */
    private Set<String> environments;

    /**
     * The event Type
     */
    private EventType type;

    /**
     * The event payload
     */
    private String payload;

    /**
     * The event parent
     */
    private String parentId;

    /**
     * The event properties
     */
    private Map<String, String> properties;

    /**
     * The event creation date
     */
    private Date createdAt;

    /**
     * The event last updated date
     */
    private Date updatedAt;

    public Set<String> getEnvironments() {
        return environments;
    }

    public void setEnvironments(Set<String> environments) {
        this.environments = environments;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
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

    public enum EventProperties {
        ID("id"),
        API_ID("api_id"),
        DICTIONARY_ID("dictionary_id"),
        ORIGIN("origin"),
        USER("user"),
        DEPLOYMENT_LABEL("deployment_label"),
        DEPLOYMENT_NUMBER("deployment_number"),
        ORGANIZATION_ID("organization_id")
        ;

        private String value;

        EventProperties(String value) {
            this.value = value;
        }

        public String getValue() {
            return this.value;
        }
    }
}
