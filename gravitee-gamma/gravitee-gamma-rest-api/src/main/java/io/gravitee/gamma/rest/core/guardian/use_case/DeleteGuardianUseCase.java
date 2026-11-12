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
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.gamma.rest.core.guardian.domain_service.GuardianDeploymentDomainService;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianAlreadyDeployedException;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianNotFoundException;
import io.gravitee.gamma.rest.core.guardian.model.Guardian;
import io.gravitee.gamma.rest.core.guardian.model.GuardianAuditEvent;
import io.gravitee.gamma.rest.core.guardian.model.GuardianDeployment;
import io.gravitee.gamma.rest.core.guardian.port.repository.GuardianRepository;
import java.util.Map;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

/**
 * Deletes a Guardian of an environment.
 *
 * <p>The Guardian is read before being deleted, both to refuse an id that belongs to another
 * environment and to hand the audit trail what was removed.
 *
 * <p>A Guardian with an active publication is refused: deleting its draft would leave the gateways
 * running a revision nothing could withdraw anymore. Withdrawing it first is the caller's choice.
 *
 * <p>Not yet implemented, and deliberately so: the PRD refuses the deletion of a Guardian that is
 * enabled on an API, listing those APIs. There is nothing to look that up in until the API-Guardian
 * association lands with AIAM-1533, so the rule belongs to that story rather than to a half-check
 * here.
 *
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@UseCase
public class DeleteGuardianUseCase {

    private final GuardianRepository guardianRepository;
    private final AuditDomainService auditDomainService;
    private final GuardianDeploymentDomainService deployments;

    public Output execute(Input input) {
        Guardian guardianToDelete = guardianRepository
            .findByEnvironmentId(input.auditInfo().environmentId(), input.guardianId())
            .orElseThrow(() -> new GuardianNotFoundException(input.guardianId()));

        if (deployments.current(guardianToDelete) instanceof GuardianDeployment.Published) {
            throw new GuardianAlreadyDeployedException(guardianToDelete.getId());
        }
        guardianRepository.delete(guardianToDelete.getId());
        auditDomainService.createEnvironmentAuditLog(
            EnvironmentAuditLogEntity.builder()
                .organizationId(input.auditInfo().organizationId())
                .environmentId(input.auditInfo().environmentId())
                .event(GuardianAuditEvent.GUARDIAN_DELETED)
                .actor(input.auditInfo().actor())
                .oldValue(guardianToDelete)
                .createdAt(TimeProvider.now())
                .properties(Map.of(AuditProperties.GUARDIAN, guardianToDelete.getId()))
                .build()
        );

        return new Output();
    }

    @Builder
    public record Input(AuditInfo auditInfo, String guardianId) {}

    public record Output() {}
}
