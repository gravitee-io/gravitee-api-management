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
package io.gravitee.gamma.rest.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;

public record UpdateResourceRequest(
    @NotBlank String name,
    @NotBlank String type,
    @JsonRawValue @NotBlank String configuration,
    boolean enabled
) {
    @JsonCreator
    public static UpdateResourceRequest of(
        @JsonProperty("name") String name,
        @JsonProperty("type") String type,
        @JsonProperty("configuration") JsonNode configuration,
        @JsonProperty("enabled") boolean enabled
    ) {
        return new UpdateResourceRequest(name, type, configuration == null ? null : configuration.toString(), enabled);
    }
}
