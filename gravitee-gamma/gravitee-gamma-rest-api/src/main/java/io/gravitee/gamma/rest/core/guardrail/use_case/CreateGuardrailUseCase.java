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
import io.gravitee.apim.core.secret.domain_service.SecretReferencePermissionDomainService;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.gamma.rest.core.guardrail.domain_service.ValidateGuardrailDomainService;
import io.gravitee.gamma.rest.core.guardrail.model.CreateGuardrail;
import io.gravitee.gamma.rest.core.guardrail.model.Guardrail;
import io.gravitee.gamma.rest.core.guardrail.model.GuardrailAuditEvent;
import io.gravitee.gamma.rest.core.guardrail.model.GuardrailLifecycleState;
import io.gravitee.gamma.rest.core.guardrail.port.repository.GuardrailRepository;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

/**
 * Creates a Guardrail in an environment, from the whole definition the console sends plus the
 * description the control plane keeps beside it.
 *
 * <p>The Guardrail is born {@link GuardrailLifecycleState#UNDEPLOYED}: creating one stores a
 * definition, it does not put anything on a gateway.
 *
 * <p>Two fields are not the caller's to set: the environment, which comes from the path, and the
 * deployment stamp, which only a deployment writes and which {@link CreateGuardrail} therefore does
 * not even declare. The id does come from the request when the caller supplies one, as AGENTS.md §9
 * asks of creation endpoints, and is generated otherwise so the console does not have to invent one
 * — a plain {@link UUID} rather than the platform's {@code UuidString}, which the core may not reach
 * (AGENTS.md §5).
 *
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@UseCase
public class CreateGuardrailUseCase {

    private final GuardrailRepository guardrailRepository;
    private final ValidateGuardrailDomainService validateGuardrail;
    private final AuditDomainService auditDomainService;
    private final SecretReferencePermissionDomainService secretReferencePermissionDomainService;

    public Output execute(Input input) {
        var now = TimeProvider.instantNow();
        var payload = input
            .guardrailToCreate()
            .toBuilder()
            // a blank id counts as none: Optional.ofNullable("") is present, so without the filter an
            // empty string would be stored and no /guardrails/{guardrailId} route could reach it again
            .id(
                Optional.ofNullable(input.guardrailToCreate().getId())
                    .filter(id -> !id.isBlank())
                    .orElseGet(() -> UUID.randomUUID().toString())
            )
            // before the structure check, so @NotBlank and @Size judge the name that will be stored: a
            // name of only whitespace is a 400 whatever whitespace it is made of
            .name(Guardrail.withoutOuterWhitespace(input.guardrailToCreate().getName()))
            .build();

        // after the id is resolved, not before: a blank id counts as none, so it becomes a generated
        // UUID that satisfies the pattern, while anything else the caller sent is still refused
        validateGuardrail.validateStructure(payload);

        var guardrailToCreate = Guardrail.from(payload, input.auditInfo().environmentId())
            .toBuilder()
            .lifecycleState(GuardrailLifecycleState.UNDEPLOYED)
            .createdAt(now)
            .updatedAt(now)
            .version(1)
            .build();

        // on the assembled Guardrail, not the payload: what is compared has to be what will be stored
        validateGuardrail.validateNameIsAvailable(
            input.auditInfo().environmentId(),
            guardrailToCreate.getName(),
            guardrailToCreate.getId()
        );

        validateGuardrail.validateDefinition(guardrailToCreate.getDefinition());
        secretReferencePermissionDomainService.validate(
            input.auditInfo().organizationId(),
            input.auditInfo().environmentId(),
            input.auditInfo().actor().userId(),
            null,
            guardrailToCreate.getDefinition()
        );

        validateGuardrail.validateAgainstInstalledPlugins(guardrailToCreate);

        var createdGuardrail = guardrailRepository.create(guardrailToCreate);
        auditDomainService.createEnvironmentAuditLog(
            EnvironmentAuditLogEntity.builder()
                .organizationId(input.auditInfo().organizationId())
                .environmentId(input.auditInfo().environmentId())
                .event(GuardrailAuditEvent.GUARDRAIL_CREATED)
                .actor(input.auditInfo().actor())
                .newValue(createdGuardrail)
                .createdAt(createdGuardrail.getCreatedAt().atZone(ZoneOffset.UTC))
                .properties(Map.of(AuditProperties.GUARDRAIL, createdGuardrail.getId()))
                .build()
        );

        return new Output(createdGuardrail);
    }

    @Builder
    public record Input(AuditInfo auditInfo, CreateGuardrail guardrailToCreate) {}

    public record Output(Guardrail guardrail) {}
}
