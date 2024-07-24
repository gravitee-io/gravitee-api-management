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
package io.gravitee.apim.core.audit.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.audit.crud_service.AuditCrudService;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.ApplicationAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.EnvironmentAuditLogEntity;
import io.gravitee.apim.core.json.JsonDiffProcessor;
import io.gravitee.apim.core.user.crud_service.UserCrudService;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@DomainService
public class AuditDomainService {

    private final AuditCrudService auditCrudService;
    private final UserCrudService userCrudService;
    private final JsonDiffProcessor jsonDiffProcessor;

    public AuditDomainService(AuditCrudService auditCrudService, UserCrudService userCrudService, JsonDiffProcessor jsonDiffProcessor) {
        this.auditCrudService = auditCrudService;
        this.userCrudService = userCrudService;
        this.jsonDiffProcessor = jsonDiffProcessor;
    }

    public void createApiAuditLog(ApiAuditLogEntity audit) {
        try {
            var entity = AuditEntity
                .builder()
                .id(UuidString.generateRandom())
                .organizationId(audit.organizationId())
                .environmentId(audit.environmentId())
                .createdAt(audit.createdAt())
                .user(createActor(audit.actor()))
                .properties(adaptAuditLogProperties(audit.properties()))
                .referenceType(AuditEntity.AuditReferenceType.API)
                .referenceId(audit.apiId())
                .event(audit.event().name())
                .patch(jsonDiffProcessor.diff(audit.oldValue(), audit.newValue()))
                .build();

            auditCrudService.create(entity);
        } catch (TechnicalManagementException e) {
            log.error("Error occurs during the creation of an API Audit Log.", e);
        }
    }

    public void createApplicationAuditLog(ApplicationAuditLogEntity audit) {
        try {
            var entity = AuditEntity
                .builder()
                .id(UuidString.generateRandom())
                .organizationId(audit.organizationId())
                .environmentId(audit.environmentId())
                .createdAt(audit.createdAt())
                .user(createActor(audit.actor()))
                .properties(adaptAuditLogProperties(audit.properties()))
                .referenceType(AuditEntity.AuditReferenceType.APPLICATION)
                .referenceId(audit.applicationId())
                .event(audit.event().name())
                .patch(jsonDiffProcessor.diff(audit.oldValue(), audit.newValue()))
                .build();

            auditCrudService.create(entity);
        } catch (TechnicalManagementException e) {
            log.error("Error occurs during the creation of an Application Audit Log.", e);
        }
    }

    public void createEnvironmentAuditLog(EnvironmentAuditLogEntity audit) {
        try {
            var entity = AuditEntity
                .builder()
                .id(UuidString.generateRandom())
                .organizationId(audit.organizationId())
                .environmentId(audit.environmentId())
                .createdAt(audit.createdAt())
                .user(createActor(audit.actor()))
                .properties(adaptAuditLogProperties(audit.properties()))
                .referenceType(AuditEntity.AuditReferenceType.ENVIRONMENT)
                .referenceId(audit.environmentId())
                .event(audit.event().name())
                .patch(jsonDiffProcessor.diff(audit.oldValue(), audit.newValue()))
                .build();

            auditCrudService.create(entity);
        } catch (TechnicalManagementException e) {
            log.error("Error occurs during the creation of an Environment Audit Log.", e);
        }
    }

    private String createActor(AuditActor actor) {
        String user = actor.userId();
        if ("token".equals(actor.userSource())) {
            user =
                userCrudService.getBaseUser(actor.userId()).displayName() + String.format(" - (using token \"%s\")", actor.userSourceId());
        }
        return user;
    }

    private Map<String, String> adaptAuditLogProperties(Map<AuditProperties, String> audit) {
        return audit
            .entrySet()
            .stream()
            .map(entry -> Map.entry(entry.getKey().name(), entry.getValue()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
