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

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.model.ApiMetadata;
import io.gravitee.apim.core.api.model.NewApiMetadata;
import io.gravitee.apim.core.api.query_service.ApiMetadataQueryService;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.event.ApiAuditEvent;
import io.gravitee.apim.core.metadata.crud_service.MetadataCrudService;
import io.gravitee.apim.core.metadata.model.Metadata;
import io.gravitee.apim.core.metadata.model.MetadataId;
import io.gravitee.common.utils.IdGenerator;
import io.gravitee.common.utils.TimeProvider;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DomainService
@Slf4j
public class ApiMetadataDomainService {

    private static final String EMAIL_SUPPORT_NAME = "email-support";

    private final MetadataCrudService metadataCrudService;
    private final ApiMetadataQueryService apiMetadataQueryService;
    private final AuditDomainService auditService;

    public ApiMetadataDomainService(
        MetadataCrudService metadataCrudService,
        ApiMetadataQueryService apiMetadataQueryService,
        AuditDomainService auditService
    ) {
        this.metadataCrudService = metadataCrudService;
        this.apiMetadataQueryService = apiMetadataQueryService;
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
        var emailSupportMetadata = metadataCrudService.create(
            Metadata
                .builder()
                .key(IdGenerator.generate(EMAIL_SUPPORT_NAME))
                .format(Metadata.MetadataFormat.MAIL)
                .name(EMAIL_SUPPORT_NAME)
                .value("${(api.primaryOwner.email)!''}")
                .referenceType(Metadata.ReferenceType.API)
                .referenceId(apiId)
                .createdAt(now)
                .updatedAt(now)
                .build()
        );
        createAuditLog(emailSupportMetadata, ApiAuditEvent.METADATA_CREATED, auditInfo);
    }

    /**
     * Save the metadata. This method will save or update the metadata without remove anything.
     */
    public void saveApiMetadata(String apiId, List<ApiMetadata> metadata, AuditInfo auditInfo) {
        var previousMetadata = apiMetadataQueryService.findApiMetadata(auditInfo.environmentId(), apiId);

        if (metadata != null) {
            for (var metadataEntry : metadata) {
                metadataEntry.setApiId(apiId);
                createOrUpdateApiMetadataEntry(apiId, metadataEntry, auditInfo);
                previousMetadata.remove(metadataEntry.getKey());
            }
        }

        previousMetadata.remove(EMAIL_SUPPORT_NAME);

        for (var metadataEntry : previousMetadata.values()) {
            var id = MetadataId.builder().key(metadataEntry.getKey()).referenceId(apiId).referenceType(Metadata.ReferenceType.API).build();
            metadataCrudService.delete(id);
        }
    }

    private void createOrUpdateApiMetadataEntry(String apiId, ApiMetadata metadataEntry, AuditInfo auditInfo) {
        var id = MetadataId.builder().key(metadataEntry.getKey()).referenceId(apiId).referenceType(Metadata.ReferenceType.API).build();
        metadataCrudService
            .findById(id)
            .ifPresentOrElse(
                existing -> update(existing.toBuilder().name(metadataEntry.getName()).value(metadataEntry.getValue()).build(), auditInfo),
                () ->
                    create(
                        NewApiMetadata
                            .builder()
                            .apiId(apiId)
                            .key(metadataEntry.getKey())
                            .value(metadataEntry.getValue())
                            .name(metadataEntry.getName())
                            .format(metadataEntry.getFormat())
                            .build(),
                        auditInfo
                    )
            );
    }

    public ApiMetadata create(NewApiMetadata newApiMetadata, AuditInfo auditInfo) {
        var now = TimeProvider.now();
        var createdMetadata = metadataCrudService.create(
            Metadata
                .builder()
                .key(newApiMetadata.getKey() != null ? newApiMetadata.getKey() : IdGenerator.generate(newApiMetadata.getName()))
                .format(newApiMetadata.getFormat())
                .name(newApiMetadata.getName())
                .value(newApiMetadata.getValue())
                .referenceType(Metadata.ReferenceType.API)
                .referenceId(newApiMetadata.getApiId())
                .createdAt(now)
                .updatedAt(now)
                .build()
        );

        createAuditLog(createdMetadata, ApiAuditEvent.METADATA_CREATED, auditInfo);
        return toApiMetadata(auditInfo.environmentId(), createdMetadata);
    }

    public ApiMetadata update(Metadata metadata, AuditInfo auditInfo) {
        log.info("Update metadata [{}] for API [{}]", metadata.getKey(), metadata.getReferenceId());
        var updatedMetadata = metadataCrudService.update(metadata.toBuilder().updatedAt(TimeProvider.now()).build());
        createAuditLog(updatedMetadata, ApiAuditEvent.METADATA_UPDATED, auditInfo);
        return toApiMetadata(auditInfo.environmentId(), updatedMetadata);
    }

    private void createAuditLog(Metadata created, ApiAuditEvent apiAuditEvent, AuditInfo auditInfo) {
        auditService.createApiAuditLog(
            ApiAuditLogEntity
                .builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .apiId(created.getReferenceId())
                .event(apiAuditEvent)
                .actor(auditInfo.actor())
                .newValue(created)
                .createdAt(created.getCreatedAt())
                .properties(Map.of(AuditProperties.METADATA, created.getKey()))
                .build()
        );
    }

    private String findDefaultValue(String environmentId, String key) {
        return this.metadataCrudService.findById(
                MetadataId.builder().key(key).referenceId(environmentId).referenceType(Metadata.ReferenceType.ENVIRONMENT).build()
            )
            .map(Metadata::getValue)
            .orElse(null);
    }

    private ApiMetadata toApiMetadata(String environmentId, Metadata metadata) {
        return ApiMetadata
            .builder()
            .key(metadata.getKey())
            .format(metadata.getFormat())
            .name(metadata.getName())
            .value(metadata.getValue())
            .defaultValue(findDefaultValue(environmentId, metadata.getKey()))
            .apiId(metadata.getReferenceId())
            .build();
    }

    public void deleteApiMetadata(String apiId, AuditInfo auditInfo) {
        var metadataToDelete = apiMetadataQueryService.findApiMetadata(auditInfo.environmentId(), apiId);
        metadataToDelete.forEach((keyId, apiMetadata) -> {
            metadataCrudService.delete(toMetadataId(apiMetadata));
            createMetadataDeletedAuditLog(apiMetadata, auditInfo);
        });
    }

    private MetadataId toMetadataId(ApiMetadata apiMetadata) {
        return MetadataId
            .builder()
            .key(apiMetadata.getKey())
            .referenceId(apiMetadata.getApiId())
            .referenceType(Metadata.ReferenceType.API)
            .build();
    }

    private void createMetadataDeletedAuditLog(ApiMetadata apiMetadata, AuditInfo auditInfo) {
        auditService.createApiAuditLog(
            ApiAuditLogEntity
                .builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .apiId(apiMetadata.getApiId())
                .event(ApiAuditEvent.METADATA_DELETED)
                .actor(auditInfo.actor())
                .oldValue(apiMetadata)
                .createdAt(TimeProvider.now())
                .properties(Map.of(AuditProperties.METADATA, apiMetadata.getKey()))
                .build()
        );
    }
}
