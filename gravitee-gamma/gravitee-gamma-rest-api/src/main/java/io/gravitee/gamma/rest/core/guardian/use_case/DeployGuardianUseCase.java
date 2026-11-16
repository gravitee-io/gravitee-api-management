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
import io.gravitee.gamma.rest.core.guardian.domain_service.ValidateGuardianDomainService;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianNotFoundException;
import io.gravitee.gamma.rest.core.guardian.model.Guardian;
import io.gravitee.gamma.rest.core.guardian.model.GuardianAuditEvent;
import io.gravitee.gamma.rest.core.guardian.port.repository.GuardianRepository;
import java.time.ZoneOffset;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * Publishes the stored draft of a Guardian to the gateways.
 *
 * <p>A Guardian with no enabled guardrail is refused here rather than when it is saved: a draft is
 * expected to be empty while its sequence is being designed, and only a deployment claims that the
 * APIs using it are protected. Publishing one would also undo the protection of a Guardian already
 * deployed, since the empty revision replaces the one the gateways run.
 *
 * <p>That rule is the only check a deployment makes against the installed plugins, and it asks them
 * one thing: whether a policy is a guardrail. The rest of the draft was validated when it was written
 * and is published as is, so a guardrail that has become incompatible since is still only refused
 * when the gateways start the Guardian.
 */
@UseCase
@RequiredArgsConstructor
public class DeployGuardianUseCase {

    private final GuardianRepository guardianRepository;
    private final GuardianDeploymentDomainService deployments;
    private final AuditDomainService auditDomainService;
    private final ValidateGuardianDomainService validateGuardian;

    public Output execute(Input input) {
        var guardian = find(input);
        validateGuardian.validateDeployable(guardian);
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
