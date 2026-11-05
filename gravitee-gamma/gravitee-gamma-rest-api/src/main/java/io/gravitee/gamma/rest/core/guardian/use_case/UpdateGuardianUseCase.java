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
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.EnvironmentAuditLogEntity;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.gamma.rest.core.guardian.domain_service.ValidateGuardianDomainService;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianNotFoundException;
import io.gravitee.gamma.rest.core.guardian.model.Guardian;
import io.gravitee.gamma.rest.core.guardian.model.GuardianAuditEvent;
import io.gravitee.gamma.rest.core.guardian.model.UpdateGuardianCommand;
import io.gravitee.gamma.rest.core.guardian.port.repository.GuardianRepository;
import java.util.Map;
import java.util.stream.Collectors;
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
    private final AuditDomainService auditDomainService;

    public Output execute(Input input) {
        // the shape first, so a malformed payload reaches neither the product rule nor the store
        validateGuardian.validateStructure(input.guardianToUpdate());

        var existingGuardian = guardianRepository
            .findByEnvironmentId(input.auditInfo().environmentId(), input.guardianId())
            .orElseThrow(() -> new GuardianNotFoundException(input.guardianId()));

        var guardianToUpdate = existingGuardian.update(input.guardianToUpdate());
        // after the merge, not before: the identity it needs comes from the path
        validateGuardian.validateDefinition(guardianToUpdate.getDefinition());

        validateGuardian
            .validateAndSanitize(new ValidateGuardianDomainService.Input(guardianToUpdate))
            .severe()
            .ifPresent(errors -> {
                throw new ValidationDomainException(errors.stream().map(Validator.Error::getMessage).collect(Collectors.joining(", ")));
            });

        var updatedGuardian = guardianRepository.update(guardianToUpdate);
        auditDomainService.createEnvironmentAuditLog(
            EnvironmentAuditLogEntity.builder()
                .organizationId(input.auditInfo().organizationId())
                .environmentId(input.auditInfo().environmentId())
                .event(GuardianAuditEvent.GUARDIAN_UPDATED)
                .actor(input.auditInfo().actor())
                .oldValue(existingGuardian)
                .newValue(updatedGuardian)
                .createdAt(updatedGuardian.getUpdatedAt())
                .properties(Map.of(AuditProperties.GUARDIAN, updatedGuardian.getId()))
                .build()
        );

        return new Output(updatedGuardian);
    }

    @Builder
    public record Input(AuditInfo auditInfo, String guardianId, UpdateGuardianCommand guardianToUpdate) {}

    public record Output(Guardian guardian) {}
}
