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
package io.gravitee.definition.model.v4.endpointgroup;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.definition.model.Plugin;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class AbstractEndpoint implements Serializable {

    private static final long serialVersionUID = 7139083731513897591L;
    private static final int DEFAULT_WEIGHT = 1;

    @NotBlank
    private String name;

    @JsonProperty(required = true)
    @NotBlank
    private String type;

    private boolean secondary;

    private List<String> tenants;

    @Builder.Default
    private int weight = DEFAULT_WEIGHT;

    private boolean inheritConfiguration;

    @Schema(implementation = Object.class)
    @JsonRawValue
    private String configuration;

    @Schema(implementation = Object.class)
    @JsonRawValue
    private String sharedConfigurationOverride;

    @JsonSetter
    public void setConfiguration(final JsonNode configuration) {
        if (configuration != null) {
            this.configuration = configuration.toString();
        }
    }

    public void setConfiguration(final String configuration) {
        this.configuration = configuration;
    }

    @JsonSetter
    public void setSharedConfigurationOverride(final JsonNode overriddenSharedConfiguration) {
        if (overriddenSharedConfiguration != null) {
            this.sharedConfigurationOverride = overriddenSharedConfiguration.toString();
        }
    }

    public void setSharedConfigurationOverride(final String overriddenSharedConfiguration) {
        this.sharedConfigurationOverride = overriddenSharedConfiguration;
    }

    @JsonIgnore
    public List<Plugin> getPlugins() {
        return List.of(new Plugin("endpoint-connector", type));
    }
}
