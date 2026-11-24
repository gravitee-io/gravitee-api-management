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
package io.gravitee.gamma.rest.core.guardrail.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.gamma.rest.core.guardrail.exception.ApiCannotRunGuardrailsException;
import io.gravitee.gamma.rest.core.guardrail.model.ApiGuardrail;
import io.gravitee.gamma.rest.core.guardrail.model.ApiGuardrails;
import io.gravitee.gamma.rest.core.guardrail.model.GuardrailDeployment;
import io.gravitee.gamma.rest.core.guardrail.model.GuardrailPolicyEffects;
import io.gravitee.gamma.rest.core.guardrail.port.service_provider.GuardedApiPort;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/**
 * Names the Guardrails an API runs, the way the API lists them.
 *
 * <p>Everything but the order comes from the revision the gateways run, like the validation that let the Guardrail be
 * turned on: a name and effects read from a draft nobody deployed would describe something the calls of the API do not
 * meet. A Guardrail whose revision was withdrawn, or whose Guardrail the environment no longer holds, is therefore
 * listed without a name and without effects — the step is still there, and the API no longer deploys.
 *
 * @author GraviteeSource Team
 */
@DomainService
@RequiredArgsConstructor
public class DescribeApiGuardrailsDomainService {

    private final GuardrailDeploymentDomainService deployments;
    private final GuardedApiPort apis;

    /**
     * The Guardrails an API runs, resolving the API itself: a module owning an API surface names an API, not a sequence
     * of steps, and the refusals it has to relay are the ones setting the Guardrails answers.
     *
     * @throws ApiNotFoundException when the environment holds no such API.
     * @throws ApiCannotRunGuardrailsException when the API holds no internal flow to read the steps from.
     */
    public List<ApiGuardrail> describe(String environmentId, String apiId) {
        var api = apis.findById(environmentId, apiId).orElseThrow(() -> new ApiNotFoundException(apiId));
        return describe(environmentId, api.guardrails());
    }

    public List<ApiGuardrail> describe(String environmentId, ApiGuardrails guardrails) {
        var enabledGuardrails = guardrails.list();
        if (enabledGuardrails.isEmpty()) {
            return List.of();
        }
        var ids = enabledGuardrails.stream().map(ApiGuardrails.Enabled::guardrailId).collect(Collectors.toSet());
        var current = deployments.current(environmentId, ids);
        return enabledGuardrails
            .stream()
            .map(enabled ->
                switch (current.get(enabled.guardrailId())) {
                    case GuardrailDeployment.Published(var deployed, _, _) -> new ApiGuardrail(
                        enabled.guardrailId(),
                        deployed.getName(),
                        enabled.phase(),
                        enabled.order(),
                        GuardrailPolicyEffects.of(deployed)
                    );
                    case null, default -> new ApiGuardrail(enabled.guardrailId(), null, enabled.phase(), enabled.order(), List.of());
                }
            )
            .toList();
    }
}
