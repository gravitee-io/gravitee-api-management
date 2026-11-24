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
package io.gravitee.gamma.rest.core.guardrail.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.gamma.rest.core.guardrail.domain_service.GuardrailPublicationDomainService;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailNotFoundException;
import io.gravitee.gamma.rest.core.guardrail.model.GuardrailRevision;
import io.gravitee.gamma.rest.core.guardrail.port.repository.GuardrailRepository;
import lombok.RequiredArgsConstructor;

/**
 * One deployment of a Guardrail with the definition it put on the gateways, so that two deployments, or a deployment
 * and the current draft, can be compared.
 */
@UseCase
@RequiredArgsConstructor
public class GetGuardrailDeploymentUseCase {

    private final GuardrailRepository guardrailRepository;
    private final GuardrailPublicationDomainService publications;

    public Output execute(Input input) {
        guardrailRepository
            .findByEnvironmentId(input.environmentId(), input.guardrailId())
            .orElseThrow(() -> new GuardrailNotFoundException(input.guardrailId()));
        return new Output(publications.revision(input.environmentId(), input.guardrailId(), input.deploymentId()));
    }

    public record Input(String environmentId, String guardrailId, String deploymentId) {}

    public record Output(GuardrailRevision.Deployed deployment) {}
}
