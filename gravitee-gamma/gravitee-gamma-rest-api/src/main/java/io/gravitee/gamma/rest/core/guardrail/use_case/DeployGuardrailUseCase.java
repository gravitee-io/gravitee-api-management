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
package io.gravitee.gamma.rest.core.guardrail.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.EnvironmentAuditLogEntity;
import io.gravitee.gamma.rest.core.guardrail.domain_service.GuardrailDeploymentDomainService;
import io.gravitee.gamma.rest.core.guardrail.domain_service.GuardrailUsageDomainService;
import io.gravitee.gamma.rest.core.guardrail.domain_service.ValidateGuardrailDomainService;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailNotFoundException;
import io.gravitee.gamma.rest.core.guardrail.model.Guardrail;
import io.gravitee.gamma.rest.core.guardrail.model.GuardrailAuditEvent;
import io.gravitee.gamma.rest.core.guardrail.model.GuardrailDeployment;
import io.gravitee.gamma.rest.core.guardrail.port.repository.GuardrailRepository;
import java.time.ZoneOffset;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * Publishes the stored draft of a Guardrail to the gateways.
 *
 * <p>A Guardrail with no enabled guardrail policy is refused here rather than when it is saved: a draft is
 * expected to be empty while its sequence is being designed, and only a deployment claims that the
 * APIs using it are protected. Publishing one would also undo the protection of a Guardrail already
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
 * leaving the Guardrail {@code PENDING} the moment it was deployed.
 */
@UseCase
@RequiredArgsConstructor
public class DeployGuardrailUseCase {

    private final GuardrailRepository guardrailRepository;
    private final GuardrailDeploymentDomainService deployments;
    private final AuditDomainService auditDomainService;
    private final ValidateGuardrailDomainService validateGuardrail;
    private final GuardrailUsageDomainService usage;

    public Output execute(Input input) {
        var guardrail = find(input);
        // the guardrail policy rule first: it names what the user has to do, where a validation error names
        // what is wrong with a sequence they may not have written in this session
        validateGuardrail.validateDeployable(guardrail);
        validateGuardrail.validateStoredAgainstInstalledPlugins(guardrail);
        var current = deployments.current(guardrail);
        usage.ensurePublishableWhereItRuns(guardrail, current);
        var publication = deployments.publish(guardrail, input.deploymentLabel(), input.auditInfo());
        if (current instanceof GuardrailDeployment.Published replaced) {
            usage.ensureStillPublishableWhereItRuns(guardrail, replaced, () -> deployments.restore(replaced, input.auditInfo()));
        }
        auditDomainService.createEnvironmentAuditLog(
            EnvironmentAuditLogEntity.builder()
                .organizationId(input.auditInfo().organizationId())
                .environmentId(input.auditInfo().environmentId())
                .actor(input.auditInfo().actor())
                .event(GuardrailAuditEvent.GUARDRAIL_DEPLOYED)
                .oldValue(deployments.state(guardrail))
                .newValue(deployments.state(guardrail, publication))
                .createdAt(publication.at().atZone(ZoneOffset.UTC))
                .properties(Map.of(AuditProperties.GUARDRAIL, guardrail.getId()))
                .build()
        );
        deployments.complete(guardrail, publication);
        return new Output(deployments.state(find(input), publication));
    }

    private Guardrail find(Input input) {
        return guardrailRepository
            .findByEnvironmentId(input.auditInfo().environmentId(), input.guardrailId())
            .orElseThrow(() -> new GuardrailNotFoundException(input.guardrailId()));
    }

    public record Input(AuditInfo auditInfo, String guardrailId, String deploymentLabel) {}

    public record Output(Guardrail guardrail) {}
}
