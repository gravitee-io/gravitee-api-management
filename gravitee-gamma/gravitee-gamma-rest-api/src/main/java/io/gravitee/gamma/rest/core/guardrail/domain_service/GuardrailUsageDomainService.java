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
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailInUseException;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailInUseException.RefusedChange;
import io.gravitee.gamma.rest.core.guardrail.model.Guardrail;
import io.gravitee.gamma.rest.core.guardrail.model.GuardrailDeployment;
import io.gravitee.gamma.rest.core.guardrail.port.service_provider.GuardedApiPort;
import lombok.RequiredArgsConstructor;

/**
 * Refuses the changes an API running a Guardrail cannot follow. An API runs a Guardrail as soon as its definition names
 * it, since turning one on is applied to the gateways by the same call: withdrawing it then stops the traffic of an
 * API nobody touched, which is the failure this refusal exists for.
 *
 * <p>An API the gateways do not run — never deployed, stopped or unpublished — names the Guardrail without executing
 * it, and is counted all the same: the refusal errs towards keeping a Guardrail an API will run again. Nothing is
 * lost if it does not, since a Guardrail no API names can be withdrawn at any time.
 *
 * @author GraviteeSource Team
 */
@DomainService
@RequiredArgsConstructor
public class GuardrailUsageDomainService {

    private final GuardedApiPort apis;

    /**
     * @param refusedChange what is refused, which the exception turns into words.
     * @throws GuardrailInUseException counting the APIs running it. It does not name them: this runs in the domain,
     *     which has no caller to filter the names against, and the Guardrail permission of an environment is not a
     *     right to read every API of it.
     */
    public void ensureUnused(Guardrail guardrail, RefusedChange refusedChange) {
        // One row is enough to refuse; the count comes with it either way.
        var using = apis.findByGuardrail(guardrail.getEnvironmentId(), guardrail.getId(), 1, 1);
        if (using.getTotalElements() > 0) {
            throw new GuardrailInUseException(guardrail.getId(), refusedChange, using.getTotalElements());
        }
    }

    /**
     * Refuses a change already written when an API turned the Guardrail on meanwhile, undoing the change first.
     *
     * <p>The check made before the write cannot see an API that turns the Guardrail on between that check and the
     * write. The API side checks the Guardrail again after its own write, so whichever of the two writes lands second,
     * its check sees the other one.
     */
    public void ensureStillUnused(Guardrail guardrail, RefusedChange refusedChange, Runnable undo) {
        try {
            ensureUnused(guardrail, refusedChange);
        } catch (GuardrailInUseException inUse) {
            undo.run();
            throw inUse;
        }
    }

    /** {@link #ensurePublishableWhereItRuns} once the publication is written, undoing it the same way. */
    public void ensureStillPublishableWhereItRuns(Guardrail guardrail, GuardrailDeployment.Published replaced, Runnable undo) {
        if (!guardrail.runsWhere(replaced.definition())) {
            ensureStillUnused(guardrail, RefusedChange.PUBLICATION_ELSEWHERE, undo);
        }
    }

    /**
     * Refuses to publish a revision that would not run where the one the APIs run runs. The steps of an API name the
     * phase and the API types its deployed revision declared, so publishing a draft that moved elsewhere turns those
     * APIs into APIs the gateways refuse. Writing the draft is guarded the same way, but the draft of an unused
     * Guardrail may move freely and be published later, once an API has turned it on.
     */
    public void ensurePublishableWhereItRuns(Guardrail guardrail, GuardrailDeployment current) {
        if (current instanceof GuardrailDeployment.Published published && !guardrail.runsWhere(published.definition())) {
            ensureUnused(guardrail, RefusedChange.PUBLICATION_ELSEWHERE);
        }
    }
}
