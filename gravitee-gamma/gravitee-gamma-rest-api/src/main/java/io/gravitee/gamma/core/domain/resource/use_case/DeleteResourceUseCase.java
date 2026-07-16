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
package io.gravitee.gamma.core.domain.resource.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.EnvironmentAuditLogEntity;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.gamma.core.domain.resource.crud_service.ResourceCrudService;
import io.gravitee.gamma.core.domain.resource.exception.ResourceNotFoundException;
import io.gravitee.gamma.core.domain.resource.model.Resource;
import io.gravitee.gamma.core.domain.resource.model.ResourceAuditEvent;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class DeleteResourceUseCase {

    private final ResourceCrudService resourceCrudService;
    private final AuditDomainService auditDomainService;

    public void execute(Input input) {
        Resource existing = resourceCrudService
            .findById(input.id())
            .filter(r -> r.referenceId().equals(input.auditInfo().environmentId()))
            .orElseThrow(() -> new ResourceNotFoundException(input.id()));

        resourceCrudService.delete(existing.id());

        createAuditLog(existing, input.auditInfo());
    }

    public record Input(AuditInfo auditInfo, String id) {}

    private void createAuditLog(Resource deleted, AuditInfo auditInfo) {
        auditDomainService.createEnvironmentAuditLog(
            EnvironmentAuditLogEntity.builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .actor(auditInfo.actor())
                .event(ResourceAuditEvent.RESOURCE_DELETED)
                .createdAt(TimeProvider.now())
                .oldValue(deleted)
                .properties(Map.of(AuditProperties.RESOURCE, deleted.id()))
                .build()
        );
    }
}
