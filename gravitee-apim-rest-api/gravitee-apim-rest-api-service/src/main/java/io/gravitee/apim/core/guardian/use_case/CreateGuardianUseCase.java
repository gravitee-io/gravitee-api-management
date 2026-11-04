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
package io.gravitee.apim.core.guardian.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.EnvironmentAuditLogEntity;
import io.gravitee.apim.core.guardian.crud_service.GuardianCrudService;
import io.gravitee.apim.core.guardian.domain_service.ValidateCreateGuardianDomainService;
import io.gravitee.apim.core.guardian.model.CreateGuardian;
import io.gravitee.apim.core.guardian.model.Guardian;
import io.gravitee.apim.core.guardian.model.GuardianAuditEvent;
import io.gravitee.apim.core.guardian.model.GuardianLifecycleState;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.service.common.UuidString;
import java.util.Map;
import java.util.Optional;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

/**
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@UseCase
public class CreateGuardianUseCase {

    private final GuardianCrudService guardianCrudService;
    private final ValidateCreateGuardianDomainService validateCreateGuardian;
    private final AuditDomainService auditService;

    public Output execute(Input input) {
        var now = TimeProvider.now();
        var guardianToCreate = Guardian.from(input.guardianToCreate())
            .toBuilder()
            .id(Optional.ofNullable(input.guardianToCreate().getId()).orElse(UuidString.generateRandom()))
            .environmentId(input.auditInfo().environmentId())
            .lifecycleState(GuardianLifecycleState.UNDEPLOYED)
            .createdAt(now)
            .updatedAt(now)
            .build();

        validateCreateGuardian.validate(guardianToCreate);

        var createdGuardian = guardianCrudService.create(guardianToCreate);
        createAuditLog(createdGuardian, input.auditInfo());

        return new Output(createdGuardian);
    }

    @Builder
    public record Input(CreateGuardian guardianToCreate, AuditInfo auditInfo) {}

    public record Output(Guardian guardian) {}

    private void createAuditLog(Guardian guardian, AuditInfo auditInfo) {
        auditService.createEnvironmentAuditLog(
            EnvironmentAuditLogEntity.builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .event(GuardianAuditEvent.GUARDIAN_CREATED)
                .actor(auditInfo.actor())
                .newValue(guardian)
                .createdAt(guardian.getCreatedAt())
                .properties(Map.of(AuditProperties.GUARDIAN, guardian.getId()))
                .build()
        );
    }
}
