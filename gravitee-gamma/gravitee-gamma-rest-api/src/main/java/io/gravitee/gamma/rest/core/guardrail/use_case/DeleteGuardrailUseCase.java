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
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.gamma.rest.core.guardrail.domain_service.GuardrailDeploymentDomainService;
import io.gravitee.gamma.rest.core.guardrail.domain_service.GuardrailUsageDomainService;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailAlreadyDeployedException;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailInUseException.RefusedChange;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailNotFoundException;
import io.gravitee.gamma.rest.core.guardrail.model.Guardrail;
import io.gravitee.gamma.rest.core.guardrail.model.GuardrailAuditEvent;
import io.gravitee.gamma.rest.core.guardrail.model.GuardrailDeployment;
import io.gravitee.gamma.rest.core.guardrail.port.repository.GuardrailRepository;
import java.util.Map;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

/**
 * Deletes a Guardrail of an environment.
 *
 * <p>The Guardrail is read before being deleted, both to refuse an id that belongs to another
 * environment and to hand the audit trail what was removed.
 *
 * <p>A Guardrail with an active publication is refused: deleting its draft would leave the gateways
 * running a revision nothing could withdraw anymore. Withdrawing it first is the caller's choice.
 *
 * <p>A Guardrail enabled on an API is refused as well, counting those APIs: their next deployment would ship a
 * step running a Guardrail that no longer exists.
 *
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@UseCase
public class DeleteGuardrailUseCase {

    private final GuardrailRepository guardrailRepository;
    private final AuditDomainService auditDomainService;
    private final GuardrailDeploymentDomainService deployments;
    private final GuardrailUsageDomainService usage;

    public Output execute(Input input) {
        Guardrail guardrailToDelete = guardrailRepository
            .findByEnvironmentId(input.auditInfo().environmentId(), input.guardrailId())
            .orElseThrow(() -> new GuardrailNotFoundException(input.guardrailId()));

        if (deployments.current(guardrailToDelete) instanceof GuardrailDeployment.Published) {
            throw new GuardrailAlreadyDeployedException(guardrailToDelete.getId());
        }
        usage.ensureUnused(guardrailToDelete, RefusedChange.DELETION);
        guardrailRepository.delete(guardrailToDelete.getId());
        auditDomainService.createEnvironmentAuditLog(
            EnvironmentAuditLogEntity.builder()
                .organizationId(input.auditInfo().organizationId())
                .environmentId(input.auditInfo().environmentId())
                .event(GuardrailAuditEvent.GUARDRAIL_DELETED)
                .actor(input.auditInfo().actor())
                .oldValue(guardrailToDelete)
                .createdAt(TimeProvider.now())
                .properties(Map.of(AuditProperties.GUARDRAIL, guardrailToDelete.getId()))
                .build()
        );

        return new Output();
    }

    @Builder
    public record Input(AuditInfo auditInfo, String guardrailId) {}

    public record Output() {}
}
