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
package io.gravitee.gamma.rest.core.guardian.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.user.crud_service.UserCrudService;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.gamma.rest.core.guardian.domain_service.GuardianPublicationDomainService;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianDeploymentNotFoundException;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianNotFoundException;
import io.gravitee.gamma.rest.core.guardian.model.GuardianRevision;
import io.gravitee.gamma.rest.core.guardian.port.repository.GuardianRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * One deployment of a Guardian with the definition it put on the gateways, so that two deployments, or a deployment
 * and the current draft, can be compared.
 */
@UseCase
@RequiredArgsConstructor
public class GetGuardianDeploymentUseCase {

    private final GuardianRepository guardianRepository;
    private final GuardianPublicationDomainService publications;
    private final UserCrudService userCrudService;

    public Output execute(Input input) {
        guardianRepository
            .findByEnvironmentId(input.environmentId(), input.guardianId())
            .orElseThrow(() -> new GuardianNotFoundException(input.guardianId()));

        var revision = publications
            .revision(input.environmentId(), input.guardianId(), input.deploymentId())
            .orElseThrow(() -> new GuardianDeploymentNotFoundException(input.deploymentId()));
        return new Output(revision, deployer(revision.deployerId()));
    }

    private BaseUserEntity deployer(String deployerId) {
        if (deployerId == null) {
            return null;
        }
        // a deleted user still shows by id rather than vanishing from the history
        return userCrudService
            .findBaseUsersByIds(List.of(deployerId))
            .stream()
            .findFirst()
            .orElse(BaseUserEntity.builder().id(deployerId).build());
    }

    public record Input(String environmentId, String guardianId, String deploymentId) {}

    /** @param deployer {@code null} when the revision does not record who deployed it */
    public record Output(GuardianRevision revision, BaseUserEntity deployer) {}
}
