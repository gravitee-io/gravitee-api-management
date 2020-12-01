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
package io.gravitee.definition.model.flow;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Guillaume CUSNIEUX (guillaume.cusnieux@graviteesource.com)
 * @author GraviteeSource Team
 */
public class Step implements Serializable {

    private String name;
    private String policy;
    private String description;
    private Object configuration;
    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }

    public String getPolicy() {
        return policy;
    }

    @JsonIgnore
    public String getDescription() {
        return description;
    }

    @JsonGetter("description")
    public String getDescriptionJson() {
        if (description == null) {
            return "";
        }
        return description;
    }

    @JsonSetter("description")
    public void setDescription(String description) {
        this.description = description;
    }

    @Schema(implementation = Object.class)
    @JsonRawValue
    public String getConfiguration() {
        return configuration == null ? null : configuration.toString();
    }

    @JsonIgnore
    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    @JsonSetter
    public void setConfiguration(JsonNode configuration) {
        this.configuration = configuration;
    }
}
