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
package io.gravitee.gamma.rest.core.guardian.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.gamma.rest.core.guardian.exception.ApiCannotRunGuardiansException;
import io.gravitee.gamma.rest.core.guardian.model.ApiGuardian;
import io.gravitee.gamma.rest.core.guardian.model.ApiGuardians;
import io.gravitee.gamma.rest.core.guardian.model.GuardianDeployment;
import io.gravitee.gamma.rest.core.guardian.model.GuardrailEffects;
import io.gravitee.gamma.rest.core.guardian.port.service_provider.GuardedApiPort;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/**
 * Names the Guardians an API runs, the way the API lists them.
 *
 * <p>Everything but the order comes from the revision the gateways run, like the validation that let the Guardian be
 * turned on: a name and effects read from a draft nobody deployed would describe something the calls of the API do not
 * meet. A Guardian whose revision was withdrawn, or whose Guardian the environment no longer holds, is therefore
 * listed without a name and without effects — the step is still there, and the API no longer deploys.
 *
 * @author GraviteeSource Team
 */
@DomainService
@RequiredArgsConstructor
public class DescribeApiGuardiansDomainService {

    private final GuardianDeploymentDomainService deployments;
    private final GuardedApiPort apis;

    /**
     * The Guardians an API runs, resolving the API itself: a module owning an API surface names an API, not a sequence
     * of steps, and the refusals it has to relay are the ones setting the Guardians answers.
     *
     * @throws ApiNotFoundException when the environment holds no such API.
     * @throws ApiCannotRunGuardiansException when the API holds no internal flow to read the steps from.
     */
    public List<ApiGuardian> describe(String environmentId, String apiId) {
        var api = apis.findById(environmentId, apiId).orElseThrow(() -> new ApiNotFoundException(apiId));
        return describe(environmentId, api.guardians());
    }

    public List<ApiGuardian> describe(String environmentId, ApiGuardians guardians) {
        var enabledGuardians = guardians.list();
        if (enabledGuardians.isEmpty()) {
            return List.of();
        }
        var ids = enabledGuardians.stream().map(ApiGuardians.Enabled::guardianId).collect(Collectors.toSet());
        var current = deployments.current(environmentId, ids);
        return enabledGuardians
            .stream()
            .map(enabled ->
                switch (current.get(enabled.guardianId())) {
                    case GuardianDeployment.Published(var deployed, _, _) -> new ApiGuardian(
                        enabled.guardianId(),
                        deployed.getName(),
                        enabled.phase(),
                        enabled.order(),
                        GuardrailEffects.of(deployed)
                    );
                    case null, default -> new ApiGuardian(enabled.guardianId(), null, enabled.phase(), enabled.order(), List.of());
                }
            )
            .toList();
    }
}
