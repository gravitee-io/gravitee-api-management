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
package io.gravitee.definition.model.v4.flow.step;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.io.Serializable;
import lombok.*;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Schema(name = "StepV4")
public class Step implements Serializable {

    @NotEmpty
    private String name;

    private String description;

    private boolean enabled = true;

    @JsonProperty(required = true)
    @NotEmpty
    private String policy;

    @Schema(implementation = Object.class)
    @JsonRawValue
    private Object configuration;

    private String condition;

    private String messageCondition;

    @JsonSetter
    public void setConfiguration(final JsonNode configuration) {
        if (configuration != null) {
            this.configuration = configuration.toString();
        }
    }

    public void setConfiguration(final String configuration) {
        this.configuration = configuration;
    }

    public String getConfiguration() {
        return (String) this.configuration;
    }
}
