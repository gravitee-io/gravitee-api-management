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
import io.gravitee.gamma.rest.core.guardian.model.GuardianDeployment;
import io.gravitee.gamma.rest.core.guardian.port.repository.GuardianRepository;
import java.time.ZoneOffset;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UndeployGuardianUseCase {

    private final GuardianRepository guardianRepository;
    private final GuardianDeploymentDomainService deployments;
    private final AuditDomainService auditDomainService;

    public Output execute(Input input) {
        var guardian = find(input);
        return switch (deployments.current(guardian)) {
            case null -> new Output(deployments.state(guardian, null));
            case GuardianDeployment.Withdrawn withdrawn -> complete(input, guardian, withdrawn);
            case GuardianDeployment.Published published -> complete(input, guardian, withdraw(guardian, published, input.auditInfo()));
        };
    }

    private Output complete(Input input, Guardian guardian, GuardianDeployment.Withdrawn withdrawal) {
        deployments.complete(guardian, withdrawal);
        return new Output(deployments.state(find(input), withdrawal));
    }

    private GuardianDeployment.Withdrawn withdraw(Guardian guardian, GuardianDeployment.Published published, AuditInfo auditInfo) {
        var withdrawal = deployments.withdraw(guardian, published, auditInfo);
        auditDomainService.createEnvironmentAuditLog(
            EnvironmentAuditLogEntity.builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .actor(auditInfo.actor())
                .properties(Map.of(AuditProperties.GUARDIAN, guardian.getId()))
                .event(GuardianAuditEvent.GUARDIAN_UNDEPLOYED)
                .createdAt(withdrawal.at().atZone(ZoneOffset.UTC))
                .oldValue(deployments.state(guardian, published))
                .newValue(deployments.state(guardian, withdrawal))
                .build()
        );
        return withdrawal;
    }

    private Guardian find(Input input) {
        return guardianRepository
            .findByEnvironmentId(input.auditInfo().environmentId(), input.guardianId())
            .orElseThrow(() -> new GuardianNotFoundException(input.guardianId()));
    }

    public record Input(AuditInfo auditInfo, String guardianId) {}

    public record Output(Guardian guardian) {}
}
