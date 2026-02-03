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
package io.gravitee.rest.api.service;

import io.gravitee.common.data.domain.MetadataPage;
import io.gravitee.repository.management.model.Audit;
import io.gravitee.rest.api.model.audit.AuditEntity;
import io.gravitee.rest.api.model.audit.AuditQuery;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Date;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface AuditService {
    @Builder
    @AllArgsConstructor
    @Setter
    @Getter
    public class AuditLogData {

        Audit.AuditReferenceType referenceType;
        String referenceId;
        Map<Audit.AuditProperties, String> properties;
        Audit.AuditEvent event;
        Date createdAt;
        Object oldValue;
        Object newValue;
        List<String> pathsToAnonymize;
    }

    default void createApiAuditLog(ExecutionContext executionContext, AuditLogData auditLogData, String apiId) {
        auditLogData.setReferenceType(Audit.AuditReferenceType.API);
        auditLogData.setReferenceId(apiId);
        createAuditLog(executionContext, auditLogData);
    }

    default void createApplicationAuditLog(ExecutionContext executionContext, AuditLogData auditLogData, String applicationId) {
        auditLogData.setReferenceType(Audit.AuditReferenceType.APPLICATION);
        auditLogData.setReferenceId(applicationId);
        createAuditLog(executionContext, auditLogData);
    }

    default void createEnvironmentAuditLog(ExecutionContext executionContext, AuditLogData auditLogData) {
        auditLogData.setReferenceType(Audit.AuditReferenceType.ENVIRONMENT);
        auditLogData.setReferenceId(executionContext.getEnvironmentId());
        createAuditLog(executionContext, auditLogData);
    }

    default void createOrganizationAuditLog(ExecutionContext executionContext, AuditLogData auditLogData) {
        auditLogData.setReferenceType(Audit.AuditReferenceType.ORGANIZATION);
        auditLogData.setReferenceId(executionContext.getOrganizationId());
        createAuditLog(executionContext, auditLogData);
    }

    default void createApiProductAuditLog(ExecutionContext executionContext, AuditLogData auditLogData, String apiProductId) {
        auditLogData.setReferenceType(Audit.AuditReferenceType.API_PRODUCT);
        auditLogData.setReferenceId(apiProductId);
        createAuditLog(executionContext, auditLogData);
    }

    void createAuditLog(ExecutionContext executionContext, AuditLogData auditLogData);

    MetadataPage<AuditEntity> search(final ExecutionContext executionContext, AuditQuery query);
}
