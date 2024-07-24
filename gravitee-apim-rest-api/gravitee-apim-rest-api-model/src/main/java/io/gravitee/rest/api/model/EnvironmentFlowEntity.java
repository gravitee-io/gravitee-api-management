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

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.EnumSet;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "EnvironmentFlow")
public class EnvironmentFlowEntity {

    @Schema(description = "Environment flow's id", example = "00f8c9e7-78fc-4907-b8c9-e778fc790750")
    @EqualsAndHashCode.Include
    private String id;

    @Schema(description = "Environment flow's name.", example = "My Environment flow")
    private String name;

    @Schema(description = "Version", example = "1")
    @EqualsAndHashCode.Include
    private String version;

    @Schema(description = "Phase", example = "REQUEST, PUBLISH")
    private EnumSet<EnvironmentFlowPhase> phase;

    @Schema(description = "List of policies (steps)")
    private List<EnvironmentFlowStep> policies;
}
