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

import javax.validation.constraints.NotNull;

import io.gravitee.rest.api.model.application.ApplicationSettings;

import java.util.Set;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class NewApplicationEntity {

    @NotNull(message = "Application's name must not be null")
    private String name;

    @NotNull(message = "Application's description must not be null")
    private String description;

    private ApplicationSettings settings;

    /**
     * @deprecated Only for backward compatibility at the API level.
     *             Will be remove in a future version.
     */
    @Deprecated
    private String type;

    /**
     * @deprecated Only for backward compatibility at the API level.
     *             Will be remove in a future version.
     */
    @Deprecated
    private String clientId;

    private Set<String> groups;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getGroups() {
        return groups;
    }

    public void setGroups(Set<String> groups) {
        this.groups = groups;
    }

    public ApplicationSettings getSettings() {
        return settings;
    }

    public void setSettings(ApplicationSettings settings) {
        this.settings = settings;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Application{");
        sb.append("description='").append(description).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", groups='").append(groups).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
