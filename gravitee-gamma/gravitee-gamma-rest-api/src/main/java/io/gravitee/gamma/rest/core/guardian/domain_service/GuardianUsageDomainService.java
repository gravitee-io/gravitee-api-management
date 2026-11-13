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
import io.gravitee.gamma.rest.core.guardian.exception.GuardianInUseException.RefusedChange;
import io.gravitee.gamma.rest.core.guardian.model.Guardian;
import io.gravitee.gamma.rest.core.guardian.model.GuardianDeployment;
import io.gravitee.gamma.rest.core.guardian.port.service_provider.GuardedApiPort;
import lombok.RequiredArgsConstructor;

/**
 * Refuses the changes an API running a Guardian cannot follow. An API runs a Guardian as soon as its definition
 * names it: withdrawing it then stops the traffic of an API nobody touched, which is the failure this refusal exists
 * for.
 *
 * @author GraviteeSource Team
 */
@DomainService
@RequiredArgsConstructor
public class GuardianUsageDomainService {

    private final GuardedApiPort apis;

    /**
     * @param refusedChange what is refused, which the exception turns into words.
     * @throws GuardianInUseException counting the APIs running it. It does not name them: this runs in the domain,
     *     which has no caller to filter the names against, and the Guardian permission of an environment is not a
     *     right to read every API of it.
     */
    public void ensureUnused(Guardian guardian, RefusedChange refusedChange) {
        // One row is enough to refuse; the count comes with it either way.
        var using = apis.findByGuardian(guardian.getEnvironmentId(), guardian.getId(), 1, 1);
        if (using.getTotalElements() > 0) {
            throw new GuardianInUseException(guardian.getId(), refusedChange, using.getTotalElements());
        }
    }

    /**
     * Refuses to publish a revision that would not run where the one the APIs run runs. The steps of an API name the
     * phase and the API types its deployed revision declared, so publishing a draft that moved elsewhere turns those
     * APIs into APIs the gateways refuse. Writing the draft is guarded the same way, but the draft of an unused
     * Guardian may move freely and be published later, once an API has turned it on.
     */
    public void ensurePublishableWhereItRuns(Guardian guardian, GuardianDeployment current) {
        if (current instanceof GuardianDeployment.Published published && !guardian.runsWhere(published.definition())) {
            ensureUnused(guardian, RefusedChange.PUBLICATION_ELSEWHERE);
        }
    }
}
