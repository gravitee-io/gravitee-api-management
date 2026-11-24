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
import io.gravitee.gamma.rest.core.guardrail.domain_service.GuardrailDeploymentDomainService;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailNotFoundException;
import io.gravitee.gamma.rest.core.guardrail.model.Guardrail;
import io.gravitee.gamma.rest.core.guardrail.port.repository.GuardrailRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

/**
 * Reads one Guardrail of an environment.
 *
 * <p>The environment is part of the lookup, not a filter applied afterwards: asking for a Guardrail
 * of another environment answers "not found" rather than leaking that it exists.
 *
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@UseCase
public class GetGuardrailUseCase {

    private final GuardrailRepository guardrailRepository;
    private final GuardrailDeploymentDomainService deployments;

    public Output execute(Input input) {
        var guardrail = guardrailRepository
            .findByEnvironmentId(input.environmentId(), input.guardrailId())
            .orElseThrow(() -> new GuardrailNotFoundException(input.guardrailId()));
        return new Output(deployments.state(guardrail));
    }

    @Builder
    public record Input(String environmentId, String guardrailId) {}

    public record Output(Guardrail guardrail) {}
}
