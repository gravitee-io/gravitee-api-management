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
import io.gravitee.gamma.rest.core.guardian.domain_service.GuardianDeploymentDomainService;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianNotFoundException;
import io.gravitee.gamma.rest.core.guardian.model.Guardian;
import io.gravitee.gamma.rest.core.guardian.model.GuardianAuditEvent;
import io.gravitee.gamma.rest.core.guardian.port.repository.GuardianRepository;
import java.time.ZoneOffset;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class DeployGuardianUseCase {

    private final GuardianRepository guardianRepository;
    private final GuardianDeploymentDomainService deployments;
    private final AuditDomainService auditDomainService;

    public Output execute(Input input) {
        var guardian = find(input);
        var publication = deployments.publish(guardian, input.deploymentLabel(), input.auditInfo());
        auditDomainService.createEnvironmentAuditLog(
            EnvironmentAuditLogEntity.builder()
                .organizationId(input.auditInfo().organizationId())
                .environmentId(input.auditInfo().environmentId())
                .actor(input.auditInfo().actor())
                .event(GuardianAuditEvent.GUARDIAN_DEPLOYED)
                .oldValue(deployments.state(guardian))
                .newValue(deployments.state(guardian, publication))
                .createdAt(publication.at().atZone(ZoneOffset.UTC))
                .properties(Map.of(AuditProperties.GUARDIAN, guardian.getId()))
                .build()
        );
        deployments.complete(guardian, publication);
        return new Output(deployments.state(find(input), publication));
    }

    private Guardian find(Input input) {
        return guardianRepository
            .findByEnvironmentId(input.auditInfo().environmentId(), input.guardianId())
            .orElseThrow(() -> new GuardianNotFoundException(input.guardianId()));
    }

    public record Input(AuditInfo auditInfo, String guardianId, String deploymentLabel) {}

    public record Output(Guardian guardian) {}
}
