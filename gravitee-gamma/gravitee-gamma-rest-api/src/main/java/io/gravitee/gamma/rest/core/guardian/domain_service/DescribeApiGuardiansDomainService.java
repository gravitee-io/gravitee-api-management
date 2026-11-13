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
import io.gravitee.gamma.rest.core.guardian.model.ApiGuardian;
import io.gravitee.gamma.rest.core.guardian.model.ApiGuardians;
import io.gravitee.gamma.rest.core.guardian.model.Guardian;
import io.gravitee.gamma.rest.core.guardian.port.repository.GuardianRepository;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/**
 * Names the Guardians an API runs, the way the API lists them.
 *
 * @author GraviteeSource Team
 */
@DomainService
@RequiredArgsConstructor
public class DescribeApiGuardiansDomainService {

    private final GuardianRepository guardianRepository;

    public List<ApiGuardian> describe(String environmentId, ApiGuardians guardians) {
        var enabledGuardians = guardians.list();
        if (enabledGuardians.isEmpty()) {
            return List.of();
        }
        var ids = enabledGuardians.stream().map(ApiGuardians.Enabled::guardianId).collect(Collectors.toSet());
        var byId = guardianRepository
            .findByEnvironmentIdAndIds(environmentId, ids)
            .stream()
            .collect(Collectors.toMap(Guardian::getId, Function.identity()));
        return enabledGuardians
            .stream()
            .map(enabled ->
                new ApiGuardian(
                    enabled.guardianId(),
                    byId.containsKey(enabled.guardianId()) ? byId.get(enabled.guardianId()).getName() : null,
                    enabled.phase(),
                    enabled.order()
                )
            )
            .toList();
    }
}
