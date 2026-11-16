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
package io.gravitee.gamma.rest.resources.guardian.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.guardian.Guardian;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.gamma.rest.core.guardian.model.GuardianRevision;
import java.util.List;
import java.util.Set;

/**
 * A revision of a Guardian with the definition it deployed. The definition fields carry the names of {@link GuardianDto}
 * so that a revision compares field by field with another one or with the current draft.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GuardianDeploymentDetailDto(
    @JsonUnwrapped GuardianDeploymentDto revision,
    String name,
    Guardian.Phase phase,
    Set<ApiType> apiTypes,
    List<Step> policies,
    List<Resource> resources
) {
    public static GuardianDeploymentDetailDto from(GuardianRevision.Deployed deployed) {
        var definition = deployed.definition();
        return new GuardianDeploymentDetailDto(
            GuardianDeploymentDto.from(deployed.revision()),
            definition.getName(),
            definition.getPhase(),
            definition.getApiTypes(),
            definition.getPolicies(),
            definition.getResources()
        );
    }
}
