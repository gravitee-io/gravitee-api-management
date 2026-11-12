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
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.gamma.rest.core.guardian.domain_service.ValidateGuardianDomainService;
import io.gravitee.gamma.rest.core.guardian.model.CreateGuardian;
import io.gravitee.gamma.rest.core.guardian.model.Guardian;
import io.gravitee.gamma.rest.core.guardian.model.GuardianAuditEvent;
import io.gravitee.gamma.rest.core.guardian.model.GuardianLifecycleState;
import io.gravitee.gamma.rest.core.guardian.port.repository.GuardianRepository;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

/**
 * Creates a Guardian in an environment, from the whole definition the console sends plus the
 * description the control plane keeps beside it.
 *
 * <p>The Guardian is born {@link GuardianLifecycleState#UNDEPLOYED}: creating one stores a
 * definition, it does not put anything on a gateway.
 *
 * <p>Two fields are not the caller's to set: the environment, which comes from the path, and the
 * deployment stamp, which only a deployment writes and which {@link CreateGuardian} therefore does
 * not even declare. The id does come from the request when the caller supplies one, as AGENTS.md §9
 * asks of creation endpoints, and is generated otherwise so the console does not have to invent one
 * — a plain {@link UUID} rather than the platform's {@code UuidString}, which the core may not reach
 * (AGENTS.md §5).
 *
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@UseCase
public class CreateGuardianUseCase {

    private final GuardianRepository guardianRepository;
    private final ValidateGuardianDomainService validateGuardian;
    private final AuditDomainService auditDomainService;

    public Output execute(Input input) {
        var now = TimeProvider.instantNow();
        var payload = input
            .guardianToCreate()
            .toBuilder()
            // a blank id counts as none: Optional.ofNullable("") is present, so without the filter an
            // empty string would be stored and no /guardians/{guardianId} route could reach it again
            .id(
                Optional.ofNullable(input.guardianToCreate().getId())
                    .filter(id -> !id.isBlank())
                    .orElseGet(() -> UUID.randomUUID().toString())
            )
            .build();

        // after the id is resolved, not before: a blank id counts as none, so it becomes a generated
        // UUID that satisfies the pattern, while anything else the caller sent is still refused
        validateGuardian.validateStructure(payload);

        validateGuardian.validateNameIsAvailable(input.auditInfo().environmentId(), payload.getName(), payload.getId());

        var guardianToCreate = Guardian.from(payload, input.auditInfo().environmentId())
            .toBuilder()
            .lifecycleState(GuardianLifecycleState.UNDEPLOYED)
            .createdAt(now)
            .updatedAt(now)
            .build();

        validateGuardian.validateDefinition(guardianToCreate.getDefinition());

        validateGuardian
            .validateAndSanitize(new ValidateGuardianDomainService.Input(guardianToCreate))
            .severe()
            .ifPresent(errors -> {
                throw new ValidationDomainException(errors.stream().map(Validator.Error::getMessage).collect(Collectors.joining(", ")));
            });

        var createdGuardian = guardianRepository.create(guardianToCreate);
        auditDomainService.createEnvironmentAuditLog(
            EnvironmentAuditLogEntity.builder()
                .organizationId(input.auditInfo().organizationId())
                .environmentId(input.auditInfo().environmentId())
                .event(GuardianAuditEvent.GUARDIAN_CREATED)
                .actor(input.auditInfo().actor())
                .newValue(createdGuardian)
                .createdAt(createdGuardian.getCreatedAt().atZone(ZoneOffset.UTC))
                .properties(Map.of(AuditProperties.GUARDIAN, createdGuardian.getId()))
                .build()
        );

        return new Output(createdGuardian);
    }

    @Builder
    public record Input(AuditInfo auditInfo, CreateGuardian guardianToCreate) {}

    public record Output(Guardian guardian) {}
}
