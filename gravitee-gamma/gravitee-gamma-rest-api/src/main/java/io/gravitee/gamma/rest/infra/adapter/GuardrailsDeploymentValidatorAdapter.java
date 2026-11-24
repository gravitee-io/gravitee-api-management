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
package io.gravitee.gamma.rest.infra.adapter;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.deployment.service_provider.ApiDeploymentValidator;
import io.gravitee.apim.core.exception.NotFoundDomainException;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.definition.model.v4.guardrail.Guardrail.Phase;
import io.gravitee.gamma.rest.core.guardrail.domain_service.ValidateApiGuardrailsDomainService;
import io.gravitee.rest.api.service.exceptions.ApiNotDeployableException;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/**
 * Refuses to hand the gateways an API running a Guardrail they could not run: one the environment no longer holds, one
 * that was undeployed, or one whose deployed revision left the phase or the API type the definition counts on.
 *
 * <p>The same check runs when the Guardrails are set on the API. It runs again here because the two happen at different
 * times: a Guardrail can be withdrawn between the day a publisher turns it on and the day the API is redeployed, and
 * the gateway's own fail-closed check would then block the API's traffic rather than the deployment.
 *
 * <p>It reads the definition it is handed rather than the stored API: a deployment does not always deploy what the
 * store holds, and judging the stored definition would let an unrunnable one through.
 *
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class GuardrailsDeploymentValidatorAdapter implements ApiDeploymentValidator {

    private final ValidateApiGuardrailsDomainService validateApiGuardrails;

    @Override
    public void validateDeployable(Api api) {
        validateInternalFlowDeployable(api);
    }

    /**
     * The whole of what this adapter judges: the Guardrails an API runs live in its internal flow, so a write that
     * republishes nothing but that flow gets the same answer as a full deployment.
     */
    @Override
    public void validateInternalFlowDeployable(Api api) {
        if (!api.holdsInternalFlow()) {
            return;
        }
        var guarded = GuardedApis.of(api);
        var guardrails = guarded.guardrails();
        if (guardrails.list().isEmpty()) {
            return;
        }
        try {
            // Every phase in one call: a definition that is wrong on both is refused once, naming both.
            validateApiGuardrails.validate(
                guarded,
                Arrays.stream(Phase.values()).collect(Collectors.toMap(Function.identity(), guardrails::idsOf))
            );
        } catch (ValidationDomainException | NotFoundDomainException e) {
            // The two families the validation declares, and those only: a technical failure of the Guardrail store is
            // not a refusal of the API, and goes through as what it is rather than as a 400 naming the API.
            throw new ApiNotDeployableException("API '" + api.getId() + "' cannot be deployed: " + e.getMessage(), e);
        }
    }
}
