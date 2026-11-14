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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.gravitee.gamma.rest.core.guardian.use_case.ListGuardianDeploymentsUseCase;
import java.time.Instant;

/**
 * A revision of a Guardian in its deployment history. {@code deployedAt} is ISO 8601 in UTC, for the same reason
 * as in {@link GuardianDto}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GuardianDeploymentDto(
    String id,
    Long version,
    String label,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant deployedAt,
    Deployer deployedBy
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Deployer(String id, String displayName) {}

    public static GuardianDeploymentDto from(ListGuardianDeploymentsUseCase.Deployment deployment) {
        var revision = deployment.revision();
        var deployer = deployment.deployer() == null
            ? null
            : new Deployer(deployment.deployer().getId(), deployment.deployer().displayName());
        return new GuardianDeploymentDto(revision.id(), revision.version(), revision.label(), revision.deployedAt(), deployer);
    }
}
