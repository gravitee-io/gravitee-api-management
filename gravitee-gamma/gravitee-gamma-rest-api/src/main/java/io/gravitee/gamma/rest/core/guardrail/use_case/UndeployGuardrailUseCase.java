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
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailInUseException.RefusedChange;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailNotFoundException;
import io.gravitee.gamma.rest.core.guardrail.model.Guardrail;
import io.gravitee.gamma.rest.core.guardrail.model.GuardrailAuditEvent;
import io.gravitee.gamma.rest.core.guardrail.model.GuardrailDeployment;
import io.gravitee.gamma.rest.core.guardrail.port.repository.GuardrailRepository;
import java.time.ZoneOffset;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UndeployGuardrailUseCase {

    private final GuardrailRepository guardrailRepository;
    private final GuardrailDeploymentDomainService deployments;
    private final AuditDomainService auditDomainService;
    private final GuardrailUsageDomainService usage;

    public Output execute(Input input) {
        var guardrail = find(input);
        return switch (deployments.current(guardrail)) {
            case null -> new Output(deployments.state(guardrail, null));
            case GuardrailDeployment.Withdrawn withdrawn -> complete(input, guardrail, withdrawn);
            case GuardrailDeployment.Published published -> {
                usage.ensureUnused(guardrail, RefusedChange.UNDEPLOYMENT);
                yield complete(input, guardrail, withdraw(guardrail, published, input.auditInfo()));
            }
        };
    }

    private Output complete(Input input, Guardrail guardrail, GuardrailDeployment.Withdrawn withdrawal) {
        deployments.complete(guardrail, withdrawal);
        return new Output(deployments.state(find(input), withdrawal));
    }

    private GuardrailDeployment.Withdrawn withdraw(Guardrail guardrail, GuardrailDeployment.Published published, AuditInfo auditInfo) {
        var withdrawal = deployments.withdraw(guardrail, published, auditInfo);
        usage.ensureStillUnused(guardrail, RefusedChange.UNDEPLOYMENT, () -> deployments.restore(published, auditInfo));
        auditDomainService.createEnvironmentAuditLog(
            EnvironmentAuditLogEntity.builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .actor(auditInfo.actor())
                .properties(Map.of(AuditProperties.GUARDRAIL, guardrail.getId()))
                .event(GuardrailAuditEvent.GUARDRAIL_UNDEPLOYED)
                .createdAt(withdrawal.at().atZone(ZoneOffset.UTC))
                .oldValue(deployments.state(guardrail, published))
                .newValue(deployments.state(guardrail, withdrawal))
                .build()
        );
        return withdrawal;
    }

    private Guardrail find(Input input) {
        return guardrailRepository
            .findByEnvironmentId(input.auditInfo().environmentId(), input.guardrailId())
            .orElseThrow(() -> new GuardrailNotFoundException(input.guardrailId()));
    }

    public record Input(AuditInfo auditInfo, String guardrailId) {}

    public record Output(Guardrail guardrail) {}
}
