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
import io.gravitee.gamma.rest.core.guardian.domain_service.GuardianPublicationDomainService;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianDeploymentNotFoundException;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianNotFoundException;
import io.gravitee.gamma.rest.core.guardian.model.Guardian;
import io.gravitee.gamma.rest.core.guardian.model.GuardianAuditEvent;
import io.gravitee.gamma.rest.core.guardian.model.UpdateGuardianCommand;
import io.gravitee.gamma.rest.core.guardian.port.repository.GuardianRepository;
import lombok.RequiredArgsConstructor;

/**
 * Replaces the draft of a Guardian with the definition one of its deployments put on the gateways, the way an API is
 * rolled back: nothing is deployed, the Guardian turns PENDING until it is deployed again, which numbers a new
 * version. The description is not part of a definition and is kept.
 */
@UseCase
@RequiredArgsConstructor
public class RollbackGuardianUseCase {

    private final GuardianRepository guardianRepository;
    private final GuardianPublicationDomainService publications;
    private final GuardianDraftDomainService drafts;

    public Output execute(Input input) {
        var environmentId = input.auditInfo().environmentId();
        var existingGuardian = guardianRepository
            .findByEnvironmentId(environmentId, input.guardianId())
            .orElseThrow(() -> new GuardianNotFoundException(input.guardianId()));
        var revision = publications
            .revision(environmentId, input.guardianId(), input.deploymentId())
            .orElseThrow(() -> new GuardianDeploymentNotFoundException(input.deploymentId()));

        var command = UpdateGuardianCommand.builder()
            .definition(revision.definition())
            .description(existingGuardian.getDescription())
            .build();
        return new Output(drafts.replace(existingGuardian, command, input.auditInfo(), GuardianAuditEvent.GUARDIAN_ROLLED_BACK));
    }

    public record Input(AuditInfo auditInfo, String guardianId, String deploymentId) {}

    public record Output(Guardian guardian) {}
}
