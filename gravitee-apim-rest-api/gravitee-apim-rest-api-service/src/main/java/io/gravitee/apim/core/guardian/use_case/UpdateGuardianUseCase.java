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
import io.gravitee.apim.core.guardian.domain_service.ValidateUpdateGuardianDomainService;
import io.gravitee.apim.core.guardian.model.Guardian;
import io.gravitee.apim.core.guardian.model.GuardianAuditEvent;
import io.gravitee.apim.core.guardian.model.UpdateGuardian;
import java.util.Map;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

/**
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@UseCase
public class UpdateGuardianUseCase {

    private final GuardianCrudService guardianCrudService;
    private final ValidateUpdateGuardianDomainService validateUpdateGuardian;
    private final AuditDomainService auditService;

    public Output execute(Input input) {
        var existingGuardian = guardianCrudService.getByEnvironmentId(input.auditInfo().environmentId(), input.guardianId());
        var guardianToUpdate = existingGuardian.update(input.guardianToUpdate());

        validateUpdateGuardian.validate(guardianToUpdate);

        var updatedGuardian = guardianCrudService.update(guardianToUpdate);
        createAuditLog(existingGuardian, updatedGuardian, input.auditInfo());

        return new Output(updatedGuardian);
    }

    @Builder
    public record Input(String guardianId, UpdateGuardian guardianToUpdate, AuditInfo auditInfo) {}

    public record Output(Guardian guardian) {}

    private void createAuditLog(Guardian oldGuardian, Guardian guardian, AuditInfo auditInfo) {
        auditService.createEnvironmentAuditLog(
            EnvironmentAuditLogEntity.builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .event(GuardianAuditEvent.GUARDIAN_UPDATED)
                .actor(auditInfo.actor())
                .oldValue(oldGuardian)
                .newValue(guardian)
                .createdAt(guardian.getUpdatedAt())
                .properties(Map.of(AuditProperties.GUARDIAN, guardian.getId()))
                .build()
        );
    }
}
