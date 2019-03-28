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
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class Dictionary {

    public enum AuditEvent implements Audit.ApiAuditEvent {
        DICTIONARY_CREATED, DICTIONARY_UPDATED, DICTIONARY_DELETED
    }

    /**
     * Dictionary ID
     */
    private String id;

    /**
     * Dictionary name
     */
    private String name;

    /**
     * Dictionary description
     */
    private String description;

    /**
     * Dictionary type
     */
    private DictionaryType type;

    /**
     * Dictionary creation date
     */
    private Date createdAt;

    /**
     * Dictionary last updated date
     */
    private Date updatedAt;

    /**
     * Dictionary last deployment date
     */
    private Date deployedAt;

    /**
     * Dictionary lifecycle state.
     */
    private LifecycleState state;

    /**
     * For {@code DictionaryType.MANUAL} dictionary;
     */
    private Map<String, String> properties;

    /**
     * For {@code DictionaryType.DYNAMIC} dictionary;
     */
    private DictionaryProvider provider;

    /**
     * For {@code DictionaryType.DYNAMIC} dictionary;
     */
    private DictionaryTrigger trigger;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DictionaryType getType() {
        return type;
    }

    public void setType(DictionaryType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Date getDeployedAt() {
        return deployedAt;
    }

    public void setDeployedAt(Date deployedAt) {
        this.deployedAt = deployedAt;
    }

    public DictionaryProvider getProvider() {
        return provider;
    }

    public void setProvider(DictionaryProvider provider) {
        this.provider = provider;
    }

    public DictionaryTrigger getTrigger() {
        return trigger;
    }

    public void setTrigger(DictionaryTrigger trigger) {
        this.trigger = trigger;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LifecycleState getState() {
        return state;
    }

    public void setState(LifecycleState state) {
        this.state = state;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Dictionary that = (Dictionary) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
