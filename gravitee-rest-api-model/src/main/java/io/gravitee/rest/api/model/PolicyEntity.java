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
package io.gravitee.rest.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class PolicyEntity {

    /**
     * The policy identifier
     */
    private String id;

    /**
     * The policy name
     */
    private String name;

    /**
     * The policy description
     */
    private String description;

    /**
     * The policy version
     */
    private String version;

    private PolicyType type;

    @JsonProperty("plugin")
    private PluginEntity plugin;

    @JsonProperty("dev")
    private PolicyDevelopmentEntity development;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public PolicyType getType() {
        return type;
    }

    public void setType(PolicyType type) {
        this.type = type;
    }

    public PluginEntity getPlugin() {
        return plugin;
    }

    public void setPlugin(PluginEntity plugin) {
        this.plugin = plugin;
    }

    public PolicyDevelopmentEntity getDevelopment() {
        return development;
    }

    public void setDevelopment(PolicyDevelopmentEntity development) {
        this.development = development;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PolicyEntity{");
        sb.append("id='").append(id).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", version='").append(version).append('\'');
        sb.append(", type='").append(type).append('\'');
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PolicyEntity that = (PolicyEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
