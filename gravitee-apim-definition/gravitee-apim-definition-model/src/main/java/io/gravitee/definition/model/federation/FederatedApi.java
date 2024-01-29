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

package io.gravitee.definition.model.federation;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.definition.model.DefinitionVersion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class FederatedApi implements Serializable {

    @JsonProperty(required = true)
    @NotBlank
    private String id;

    @JsonProperty(required = true)
    @NotBlank
    private String name;

    @JsonProperty(required = true)
    @NotBlank
    private String apiVersion;

    @JsonProperty(required = true)
    @NotNull
    private DefinitionVersion definitionVersion = DefinitionVersion.FEDERATED;

    @JsonProperty(required = true)
    @NotBlank
    private String accessPoint;

    private Set<@NotBlank String> tags;

    public static FederatedApiBuilder builder() {
        return new FederatedApiBuilder();
    }
}
