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
package io.gravitee.rest.api.model;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@Schema(name = "EnvironmentFlowStep")
public class EnvironmentFlowStep {

    @NotEmpty
    @Schema(description = "Policy (step) name", example = "My Policy")
    private String name;

    @Schema(description = "Brief description", example = "This is a policy.")
    private String description;

    @Schema(description = "Toggle policy enabled/disabled. Default true", example = "true, false")
    private boolean enabled = true;

    @NotEmpty
    @Schema(description = "Policy")
    private String policy;

    @NotNull
    @Schema(description = "Policy configuration")
    private JsonNode configuration;

    @Schema(description = "Condition")
    private String condition;

    @Schema(description = "Condition for message")
    private String messageCondition;
}
