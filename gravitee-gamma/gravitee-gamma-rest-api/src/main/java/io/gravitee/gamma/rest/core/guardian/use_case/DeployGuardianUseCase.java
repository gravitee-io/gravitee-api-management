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
import io.gravitee.gamma.rest.core.guardian.domain_service.GuardianUsageDomainService;
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
 * <p>The stored draft is then validated again against the plugins installed <em>now</em>, rather than
 * against the ones it was written under. A deployment is the moment a stored definition becomes a
 * revision the gateways must run, and nothing guarantees it still can: the draft may predate a check,
 * name a policy or a resource type uninstalled since, or carry a configuration the schema of an upgraded
 * plugin no longer accepts. Publishing it anyway is what AIAM-1750 reports — the API answers {@code DEPLOYED} and the
 * console shows "Deployed" for a revision no gateway ever starts, with nothing to tell the user.
 *
 * <p>That pass reads the draft without rewriting it: a deployment publishes what is stored, and
 * sanitizing the configurations here would publish a revision the stored draft no longer matches,
 * leaving the Guardian {@code PENDING} the moment it was deployed.
 */
@UseCase
@RequiredArgsConstructor
public class DeployGuardianUseCase {

    private final GuardianRepository guardianRepository;
    private final GuardianDeploymentDomainService deployments;
    private final AuditDomainService auditDomainService;
    private final ValidateGuardianDomainService validateGuardian;
    private final GuardianUsageDomainService usage;

    public Output execute(Input input) {
        var guardian = find(input);
        // the guardrail rule first: it names what the user has to do, where a validation error names
        // what is wrong with a sequence they may not have written in this session
        validateGuardian.validateDeployable(guardian);
        validateGuardian.validateStoredAgainstInstalledPlugins(guardian);
        usage.ensurePublishableWhereItRuns(guardian, deployments.current(guardian));
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
