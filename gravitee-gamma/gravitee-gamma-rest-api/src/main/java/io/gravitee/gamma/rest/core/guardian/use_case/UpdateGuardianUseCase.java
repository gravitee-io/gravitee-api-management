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
package io.gravitee.gamma.rest.core.guardian.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.gamma.rest.core.guardian.domain_service.GuardianDraftDomainService;
import io.gravitee.gamma.rest.core.guardian.domain_service.ValidateGuardianDomainService;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianNotFoundException;
import io.gravitee.gamma.rest.core.guardian.model.DraftPrecondition;
import io.gravitee.gamma.rest.core.guardian.model.Guardian;
import io.gravitee.gamma.rest.core.guardian.model.GuardianAuditEvent;
import io.gravitee.gamma.rest.core.guardian.model.UpdateGuardianCommand;
import io.gravitee.gamma.rest.core.guardian.port.repository.GuardianRepository;
import java.util.Map;
import java.util.Objects;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

/**
 * Replaces the definition of a Guardian with the one the console sends.
 *
 * <p>A {@code PUT} sets everything: the name, the phase, the API types, the guardrail sequence and
 * the resources all come from the payload, and whatever it leaves out is left out of the stored
 * definition too. {@link Guardian#update} keeps back only what the caller does not own — the
 * identity, which comes from the path, and the deployment stamp.
 *
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@UseCase
public class UpdateGuardianUseCase {

    private final GuardianRepository guardianRepository;
    private final ValidateGuardianDomainService validateGuardian;
    private final GuardianDraftDomainService drafts;

    public Output execute(Input input) {
        // the shape first, so a malformed payload reaches neither the product rule nor the store
        validateGuardian.validateStructure(input.guardianToUpdate());

        var existingGuardian = guardianRepository
            .findByEnvironmentId(input.auditInfo().environmentId(), input.guardianId())
            .orElseThrow(() -> new GuardianNotFoundException(input.guardianId()));
        return new Output(
            drafts.replace(
                existingGuardian,
                input.guardianToUpdate(),
                input.precondition(),
                input.auditInfo(),
                GuardianAuditEvent.GUARDIAN_UPDATED,
                Map.of(),
                GuardianDraftDomainService.DraftOrigin.SUBMITTED
            )
        );
    }

    @Builder
    public record Input(AuditInfo auditInfo, String guardianId, UpdateGuardianCommand guardianToUpdate, DraftPrecondition precondition) {
        public Input {
            // stating none and asking to overwrite are opposite intents: a caller has to say which
            Objects.requireNonNull(precondition, "The version an update is built on must be stated");
        }
    }

    public record Output(Guardian guardian) {}
}
