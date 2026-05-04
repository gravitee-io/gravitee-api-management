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
package io.gravitee.definition.model.v4.resource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.definition.model.Plugin;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Schema(name = "ResourceV4")
@Builder
public class Resource implements Serializable {

    /**
     * Optional reference to an environment-level resource. When set, {@link #name}, {@link #type} and
     * {@link #configuration} may be omitted and are resolved at deploy-time against the environment resource registry.
     */
    @Schema(
        description = "Optional reference to an environment-level resource. When set, name/type/configuration may be omitted and will be resolved at deploy-time."
    )
    private String id;

    @JsonProperty
    private String name;

    @JsonProperty
    private String type;

    @Schema(implementation = Object.class)
    @JsonRawValue
    private String configuration;

    @Builder.Default
    private boolean enabled = true;

    @JsonSetter
    public void setConfiguration(final JsonNode configuration) {
        if (configuration != null) {
            this.configuration = configuration.toString();
        }
    }

    public void setConfiguration(final String configuration) {
        this.configuration = configuration;
    }

    @JsonIgnore
    public List<Plugin> getPlugins() {
        if (type == null) {
            return List.of();
        }
        return List.of(new Plugin("resource", type));
    }

    @JsonIgnore
    public boolean isReference() {
        return id != null && !id.isBlank();
    }

    @AssertTrue(message = "Resource must either reference an environment resource by id, or declare name, type and configuration inline.")
    @JsonIgnore
    public boolean isValidReferenceOrInline() {
        boolean hasReference = isReference();
        boolean hasInline =
            name != null && !name.isBlank() && type != null && !type.isBlank() && configuration != null && !configuration.isBlank();
        return hasReference || hasInline;
    }
}
