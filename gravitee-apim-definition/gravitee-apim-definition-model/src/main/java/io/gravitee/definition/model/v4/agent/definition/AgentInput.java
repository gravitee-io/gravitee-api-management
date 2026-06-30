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
package io.gravitee.definition.model.v4.agent.definition;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * A named value an agent consumes from the shared scope. On the <b>root</b> node the inputs form the agent's
 * external contract — the entrypoint maps an incoming request to these scope keys, applies {@code default}
 * fallbacks, checks {@code required} and coerces {@code type}. On inner nodes only {@code name} is typically
 * set (the upstream scope key it reads), with an optional {@code default} fallback.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentInput {

    /** The scope key. */
    @JsonProperty(required = true)
    @NotBlank
    private String name;

    /** Optional declared type for coercion / schema generation (e.g. {@code string}, {@code number}). */
    private String type;

    /** Whether the entrypoint must receive this input (root contract only). */
    private Boolean required;

    /** Fallback value used when the input is absent. */
    @JsonProperty("default")
    private Object defaultValue;

    /** Human-readable description (schema / AgentCard generation). */
    private String description;
}
