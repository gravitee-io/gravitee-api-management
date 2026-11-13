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
import io.gravitee.gamma.rest.core.guardian.exception.GuardianInUseException;
import io.gravitee.gamma.rest.core.guardian.model.Guardian;
import io.gravitee.gamma.rest.core.guardian.port.service_provider.GuardedApiPort;
import lombok.RequiredArgsConstructor;

/**
 * Refuses the changes an API running a Guardian cannot follow. An API runs a Guardian as soon as its current
 * definition enables it, deployed or not: the next deployment of the API would otherwise ship a step the gateways
 * refuse.
 *
 * @author GraviteeSource Team
 */
@DomainService
@RequiredArgsConstructor
public class GuardianUsageDomainService {

    private final GuardedApiPort apis;

    /** @param refusedChange what is refused, completing "Guardian 'id' cannot be …". */
    public void ensureUnused(Guardian guardian, String refusedChange) {
        var using = apis.findByGuardian(guardian.getEnvironmentId(), guardian.getId());
        if (!using.isEmpty()) {
            throw new GuardianInUseException(guardian.getId(), refusedChange, using);
        }
    }
}
