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
import io.gravitee.gamma.rest.core.guardrail.domain_service.GuardrailDraftDomainService;
import io.gravitee.gamma.rest.core.guardrail.domain_service.ValidateGuardrailDomainService;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailNotFoundException;
import io.gravitee.gamma.rest.core.guardrail.model.DraftPrecondition;
import io.gravitee.gamma.rest.core.guardrail.model.Guardrail;
import io.gravitee.gamma.rest.core.guardrail.model.GuardrailAuditEvent;
import io.gravitee.gamma.rest.core.guardrail.model.UpdateGuardrailCommand;
import io.gravitee.gamma.rest.core.guardrail.port.repository.GuardrailRepository;
import java.util.Map;
import java.util.Objects;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

/**
 * Replaces the definition of a Guardrail with the one the console sends.
 *
 * <p>A {@code PUT} sets everything: the name, the phase, the API types, the guardrail policy sequence and
 * the resources all come from the payload, and whatever it leaves out is left out of the stored
 * definition too. {@link Guardrail#update} keeps back only what the caller does not own — the
 * identity, which comes from the path, and the deployment stamp.
 *
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@UseCase
public class UpdateGuardrailUseCase {

    private final GuardrailRepository guardrailRepository;
    private final ValidateGuardrailDomainService validateGuardrail;
    private final GuardrailDraftDomainService drafts;

    public Output execute(Input input) {
        // the shape first, so a malformed payload reaches neither the product rule nor the store
        validateGuardrail.validateStructure(input.guardrailToUpdate());

        var existingGuardrail = guardrailRepository
            .findByEnvironmentId(input.auditInfo().environmentId(), input.guardrailId())
            .orElseThrow(() -> new GuardrailNotFoundException(input.guardrailId()));
        return new Output(
            drafts.replace(
                existingGuardrail,
                input.guardrailToUpdate(),
                input.precondition(),
                input.auditInfo(),
                GuardrailAuditEvent.GUARDRAIL_UPDATED,
                Map.of(),
                GuardrailDraftDomainService.DraftOrigin.SUBMITTED
            )
        );
    }

    @Builder
    public record Input(AuditInfo auditInfo, String guardrailId, UpdateGuardrailCommand guardrailToUpdate, DraftPrecondition precondition) {
        public Input {
            // stating none and asking to overwrite are opposite intents: a caller has to say which
            Objects.requireNonNull(precondition, "The version an update is built on must be stated");
        }
    }

    public record Output(Guardrail guardrail) {}
}
