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
import io.gravitee.gamma.rest.core.guardian.model.Guardian;
import io.gravitee.gamma.rest.core.guardian.model.GuardianAuditEvent;
import io.gravitee.gamma.rest.core.guardian.model.UpdateGuardianCommand;
import io.gravitee.gamma.rest.core.guardian.port.repository.GuardianRepository;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * Replaces the draft of a Guardian, whether the console sends it or a deployment it rolls back to: both go through the
 * same checks, since an old revision may carry a name another Guardian took since, or a guardrail no longer installed.
 *
 * <p>One thing separates them, {@link DraftOrigin}: a submitted draft is stored as its policies accept it, where a
 * restored revision is stored as it was published.
 */
@DomainService
@RequiredArgsConstructor
public class GuardianDraftDomainService {

    private final GuardianRepository guardianRepository;
    private final ValidateGuardianDomainService validateGuardian;
    private final AuditDomainService auditDomainService;
    private final GuardianDeploymentDomainService deployments;

    /** Where a draft being stored comes from, which decides what is done with a configuration its policy would correct. */
    public enum DraftOrigin {
        /** Written by the caller: what is stored is the configuration each policy accepts, corrections included. */
        SUBMITTED,
        /**
         * Restored from a revision the gateways ran: it is stored as it was published, so a configuration a
         * policy would correct is refused rather than rewritten. Storing the corrected revision instead would
         * leave a draft that no longer matches the publication, and the Guardian {@code PENDING} straight
         * after the rollback.
         */
        RESTORED,
    }

    /**
     * Stores the draft the command describes in place of the existing one, and audits it.
     *
     * @param auditProperties what the audit records beside the Guardian
     * @param origin where the draft comes from, see {@link DraftOrigin}
     * @throws ValidationDomainException when the definition is invalid or names a guardrail that is not installed
     */
    public Guardian replace(
        Guardian existingGuardian,
        UpdateGuardianCommand command,
        AuditInfo auditInfo,
        GuardianAuditEvent event,
        Map<AuditProperties, String> auditProperties,
        DraftOrigin origin
    ) {
        var guardianToUpdate = existingGuardian.update(command);
        // after the merge, not before: the identity it needs comes from the path
        validateGuardian.validateDefinition(guardianToUpdate.getDefinition());

        // Only a rename is checked. An edit that leaves the name alone cannot newly break the rule, and
        // checking it anyway would make a Guardian unsaveable whenever the store already holds a case
        // variant of its name — "Foo" beside "foo", from data that predates the rule or from two creates
        // that raced past it. Nothing else about that Guardian could then be edited.
        //
        // After validateDefinition, which refuses a missing name: the command declares the definition
        // @NotNull but not @Valid, so the name may still be null here.
        if (!guardianToUpdate.getName().equalsIgnoreCase(existingGuardian.getName())) {
            validateGuardian.validateNameIsAvailable(auditInfo.environmentId(), guardianToUpdate.getName(), existingGuardian.getId());
        }

        switch (origin) {
            case SUBMITTED -> validateGuardian.validateAgainstInstalledPolicies(guardianToUpdate);
            case RESTORED -> validateGuardian.validateStoredAgainstInstalledPolicies(guardianToUpdate);
        }

        var publication = deployments.current(existingGuardian);
        var updatedGuardian = deployments.state(guardianRepository.update(guardianToUpdate), publication);
        var properties = new EnumMap<AuditProperties, String>(AuditProperties.class);
        properties.putAll(auditProperties);
        properties.put(AuditProperties.GUARDIAN, updatedGuardian.getId());
        auditDomainService.createEnvironmentAuditLog(
            EnvironmentAuditLogEntity.builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .event(event)
                .actor(auditInfo.actor())
                .oldValue(deployments.state(existingGuardian, publication))
                .newValue(updatedGuardian)
                .createdAt(updatedGuardian.getUpdatedAt().atZone(ZoneOffset.UTC))
                .properties(properties)
                .build()
        );
        return updatedGuardian;
    }
}
