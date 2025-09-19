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
package io.gravitee.apim.infra.crud_service.audit;

import io.gravitee.apim.core.audit.crud_service.AuditCrudService;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.AuditRepository;
import io.gravitee.repository.management.model.Audit;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Date;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class AuditCrudServiceImpl implements AuditCrudService {

    private final AuditRepository auditRepository;

    public AuditCrudServiceImpl(@Lazy AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Override
    public void create(AuditEntity auditEntity) throws TechnicalManagementException {
        var entity = Audit.builder()
            .id(auditEntity.getId())
            .organizationId(auditEntity.getOrganizationId())
            .environmentId(auditEntity.getEnvironmentId())
            .createdAt(Date.from(auditEntity.getCreatedAt().toInstant()))
            .user(auditEntity.getUser())
            .properties(auditEntity.getProperties())
            .referenceType(Audit.AuditReferenceType.valueOf(auditEntity.getReferenceType().name()))
            .referenceId(auditEntity.getReferenceId())
            .event(auditEntity.getEvent())
            .patch(auditEntity.getPatch())
            .build();

        try {
            auditRepository.create(entity);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(e);
        }
    }
}
