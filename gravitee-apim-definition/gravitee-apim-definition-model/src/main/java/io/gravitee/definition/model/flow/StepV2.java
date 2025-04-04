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
package io.gravitee.definition.model.flow;

import com.fasterxml.jackson.annotation.*;
import io.gravitee.definition.model.Plugin;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Guillaume CUSNIEUX (guillaume.cusnieux@graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode
@Setter
@Getter
public class StepV2 implements Serializable {

    @JsonProperty("name")
    private String name;

    @JsonProperty("policy")
    private String policy;

    @JsonProperty("description")
    private String description;

    @Schema(implementation = Object.class)
    @JsonRawValue
    @JsonProperty("configuration")
    private Object configuration;

    @JsonProperty("enabled")
    @Builder.Default
    private boolean enabled = true;

    @JsonProperty("condition")
    private String condition;

    public String getConfiguration() {
        return configuration == null ? null : configuration.toString();
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    @JsonIgnore
    public List<Plugin> getPlugins() {
        return List.of(new Plugin("policy", policy));
    }
}
