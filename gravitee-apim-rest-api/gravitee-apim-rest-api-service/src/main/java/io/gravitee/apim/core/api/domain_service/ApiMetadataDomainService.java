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
package io.gravitee.apim.core.api.domain_service;

import io.gravitee.apim.core.api.model.ApiMetadata;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.event.ApiAuditEvent;
import io.gravitee.apim.core.datetime.TimeProvider;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.metadata.crud_service.MetadataCrudService;
import io.gravitee.apim.core.metadata.model.Metadata;
import io.gravitee.common.utils.IdGenerator;
import java.util.List;
import java.util.Map;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiMetadataDomainService {

    private final MetadataCrudService metadataCrudService;
    private final AuditDomainService auditService;

    public ApiMetadataDomainService(MetadataCrudService metadataCrudService, AuditDomainService auditService) {
        this.metadataCrudService = metadataCrudService;
        this.auditService = auditService;
    }

    /**
     * Create all default metadata for an API that has been created
     *
     * <p>
     *     Currently, it only creates the email-support metadata.
     * </p>
     * @param apiId The created API id
     * @param auditInfo The audit information
     */
    public void createDefaultApiMetadata(String apiId, AuditInfo auditInfo) {
        var now = TimeProvider.now();
        String name = "email-support";
        var emailSupportMetadata = metadataCrudService.create(
            Metadata
                .builder()
                .key(IdGenerator.generate(name))
                .format(Metadata.MetadataFormat.MAIL)
                .name(name)
                .value("${(api.primaryOwner.email)!''}")
                .referenceType(Metadata.ReferenceType.API)
                .referenceId(apiId)
                .createdAt(now)
                .updatedAt(now)
                .build()
        );
        createAuditLog(emailSupportMetadata, auditInfo);
    }

    public void saveApiMetadata(String apiId, List<ApiMetadata> metadata, AuditInfo auditInfo) {
        throw new TechnicalDomainException("Not yet implemented");
    }

    private void createAuditLog(Metadata created, AuditInfo auditInfo) {
        auditService.createApiAuditLog(
            ApiAuditLogEntity
                .builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .apiId(created.getReferenceId())
                .event(ApiAuditEvent.METADATA_CREATED)
                .actor(auditInfo.actor())
                .newValue(created)
                .createdAt(created.getCreatedAt())
                .properties(Map.of(AuditProperties.METADATA, created.getKey()))
                .build()
        );
    }
}
