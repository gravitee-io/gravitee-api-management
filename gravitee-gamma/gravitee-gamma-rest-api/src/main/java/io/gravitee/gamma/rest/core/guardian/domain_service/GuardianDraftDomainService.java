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
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.EnvironmentAuditLogEntity;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.gamma.rest.core.guardian.model.Guardian;
import io.gravitee.gamma.rest.core.guardian.model.GuardianAuditEvent;
import io.gravitee.gamma.rest.core.guardian.model.UpdateGuardianCommand;
import io.gravitee.gamma.rest.core.guardian.port.repository.GuardianRepository;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/**
 * Replaces the draft of a Guardian, whether the console sends it or a deployment it rolls back to: both go through the
 * same checks, since an old revision may carry a name another Guardian took since, or a guardrail no longer installed.
 */
@DomainService
@RequiredArgsConstructor
public class GuardianDraftDomainService {

    private final GuardianRepository guardianRepository;
    private final ValidateGuardianDomainService validateGuardian;
    private final AuditDomainService auditDomainService;
    private final GuardianDeploymentDomainService deployments;

    /**
     * {@link Guardian#update} keeps back only what the caller does not own — the identity, which comes from the path,
     * and the deployment stamp.
     */
    public Guardian replace(Guardian existingGuardian, UpdateGuardianCommand command, AuditInfo auditInfo, GuardianAuditEvent event) {
        var guardianToUpdate = existingGuardian.update(command);
        // after the merge, not before: the identity it needs comes from the path
        validateGuardian.validateDefinition(guardianToUpdate.getDefinition());

        // Only a rename is checked. An edit that leaves the name alone cannot newly break the rule, and
        // checking it anyway would make a Guardian unsaveable whenever the store already holds a case
        // variant of its name — "Foo" beside "foo", from data that predates the rule or from two creates
        // that raced past it. Nothing else about that Guardian could then be edited.
        //
        // After validateDefinition, which is what refuses a missing name: UpdateGuardianCommand declares
        // the definition @NotNull but not @Valid, so a body omitting the name reaches here carrying one,
        // and asking the store about a null name answered 500 where the payload deserved a 400.
        if (!guardianToUpdate.getName().equalsIgnoreCase(existingGuardian.getName())) {
            validateGuardian.validateNameIsAvailable(auditInfo.environmentId(), guardianToUpdate.getName(), existingGuardian.getId());
        }

        validateGuardian
            .validateAndSanitize(new ValidateGuardianDomainService.Input(guardianToUpdate))
            .severe()
            .ifPresent(errors -> {
                throw new ValidationDomainException(errors.stream().map(Validator.Error::getMessage).collect(Collectors.joining(", ")));
            });

        var publication = deployments.current(existingGuardian);
        var updatedGuardian = deployments.state(guardianRepository.update(guardianToUpdate), publication);
        auditDomainService.createEnvironmentAuditLog(
            EnvironmentAuditLogEntity.builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .event(event)
                .actor(auditInfo.actor())
                .oldValue(deployments.state(existingGuardian, publication))
                .newValue(updatedGuardian)
                .createdAt(updatedGuardian.getUpdatedAt().atZone(ZoneOffset.UTC))
                .properties(Map.of(AuditProperties.GUARDIAN, updatedGuardian.getId()))
                .build()
        );
        return updatedGuardian;
    }
}
