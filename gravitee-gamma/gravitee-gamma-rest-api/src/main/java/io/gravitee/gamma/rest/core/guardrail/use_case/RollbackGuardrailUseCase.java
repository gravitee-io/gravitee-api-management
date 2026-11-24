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
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.gamma.rest.core.guardrail.domain_service.GuardrailDraftDomainService;
import io.gravitee.gamma.rest.core.guardrail.domain_service.GuardrailPublicationDomainService;
import io.gravitee.gamma.rest.core.guardrail.domain_service.ValidateGuardrailDomainService;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailNotFoundException;
import io.gravitee.gamma.rest.core.guardrail.model.DraftPrecondition;
import io.gravitee.gamma.rest.core.guardrail.model.Guardrail;
import io.gravitee.gamma.rest.core.guardrail.model.GuardrailAuditEvent;
import io.gravitee.gamma.rest.core.guardrail.model.UpdateGuardrailCommand;
import io.gravitee.gamma.rest.core.guardrail.port.repository.GuardrailRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * Replaces the draft of a Guardrail with the definition one of its deployments put on the gateways, the way an API is
 * rolled back: nothing is deployed, the Guardrail turns PENDING until it is deployed again, which records a new
 * revision. The description is not part of a definition and is kept.
 *
 * <p>The revision is restored as it was published, never rewritten: a policy that has gained a default or tightened
 * its schema since makes the rollback answer a 400 naming the step, rather than storing a corrected definition the
 * gateways never ran. Going back then takes an edit, which is the only way to store a configuration its policy
 * accepts.
 */
@UseCase
@RequiredArgsConstructor
public class RollbackGuardrailUseCase {

    private final GuardrailRepository guardrailRepository;
    private final ValidateGuardrailDomainService validateGuardrail;
    private final GuardrailPublicationDomainService publications;
    private final GuardrailDraftDomainService drafts;

    public Output execute(Input input) {
        var environmentId = input.auditInfo().environmentId();
        var existingGuardrail = guardrailRepository
            .findByEnvironmentId(environmentId, input.guardrailId())
            .orElseThrow(() -> new GuardrailNotFoundException(input.guardrailId()));
        var deployed = publications.revision(environmentId, input.guardrailId(), input.deploymentId());

        var command = UpdateGuardrailCommand.builder()
            .definition(deployed.definition())
            .description(existingGuardrail.getDescription())
            .build();
        // a constraint may have tightened since the revision was deployed: an update would refuse it too
        validateGuardrail.validateStructure(command);
        return new Output(
            drafts.replace(
                existingGuardrail,
                command,
                // a rollback replaces the draft by design, whatever it holds
                DraftPrecondition.anyVersion(),
                input.auditInfo(),
                GuardrailAuditEvent.GUARDRAIL_ROLLED_BACK,
                Map.of(AuditProperties.GUARDRAIL_DEPLOYMENT, deployed.revision().id()),
                GuardrailDraftDomainService.DraftOrigin.RESTORED
            )
        );
    }

    public record Input(AuditInfo auditInfo, String guardrailId, String deploymentId) {}

    public record Output(Guardrail guardrail) {}
}
