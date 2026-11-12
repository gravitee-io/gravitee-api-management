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
import io.gravitee.gamma.rest.core.guardian.domain_service.GuardianDeploymentDomainService;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianNotFoundException;
import io.gravitee.gamma.rest.core.guardian.model.Guardian;
import io.gravitee.gamma.rest.core.guardian.port.repository.GuardianRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

/**
 * Reads one Guardian of an environment.
 *
 * <p>The environment is part of the lookup, not a filter applied afterwards: asking for a Guardian
 * of another environment answers "not found" rather than leaking that it exists.
 *
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@UseCase
public class GetGuardianUseCase {

    private final GuardianRepository guardianRepository;
    private final GuardianDeploymentDomainService deployments;

    public Output execute(Input input) {
        var guardian = guardianRepository
            .findByEnvironmentId(input.environmentId(), input.guardianId())
            .orElseThrow(() -> new GuardianNotFoundException(input.guardianId()));
        return new Output(deployments.state(guardian));
    }

    @Builder
    public record Input(String environmentId, String guardianId) {}

    public record Output(Guardian guardian) {}
}
