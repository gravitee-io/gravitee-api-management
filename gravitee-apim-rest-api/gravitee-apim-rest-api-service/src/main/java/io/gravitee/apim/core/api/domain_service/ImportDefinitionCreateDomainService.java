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
import io.gravitee.apim.core.api.exception.ApiCreatedWithErrorException;
import io.gravitee.apim.core.api.model.ApiWithFlows;
import io.gravitee.apim.core.api.model.NewApiMetadata;
import io.gravitee.apim.core.api.model.factory.ApiModelFactory;
import io.gravitee.apim.core.api.model.import_definition.ApiMember;
import io.gravitee.apim.core.api.model.import_definition.ImportDefinition;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.documentation.domain_service.CreateApiDocumentationDomainService;
import io.gravitee.apim.core.documentation.domain_service.DocumentationValidationDomainService;
import io.gravitee.apim.core.documentation.exception.InvalidPageParentException;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.apim.core.media.model.Media;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerFactory;
import io.gravitee.apim.core.metadata.crud_service.MetadataCrudService;
import io.gravitee.apim.core.metadata.model.Metadata;
import io.gravitee.apim.core.metadata.model.MetadataId;
import io.gravitee.apim.core.plan.domain_service.CreatePlanDomainService;
import io.gravitee.apim.core.plan.model.PlanWithFlows;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.service.common.UuidString;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@DomainService
public class ImportDefinitionCreateDomainService {

    private final ApiImportDomainService apiImportDomainService;
    private final ApiPrimaryOwnerFactory apiPrimaryOwnerFactory;
    private final CreateApiDomainService createApiDomainService;
    private final ValidateApiDomainService validateApiDomainService;
    private final ApiMetadataDomainService apiMetadataDomainService;
    private final CreatePlanDomainService createPlanDomainService;
    private final CreateApiDocumentationDomainService createApiDocumentationDomainService;
    private final ApiIdsCalculatorDomainService apiIdsCalculatorDomainService;
    private final MetadataCrudService metadataCrudService;
    private final DocumentationValidationDomainService documentationValidationDomainService;

    public ImportDefinitionCreateDomainService(
        ApiImportDomainService apiImportDomainService,
        ApiPrimaryOwnerFactory apiPrimaryOwnerFactory,
        CreateApiDomainService createApiDomainService,
        ValidateApiDomainService validateApiDomainService,
        ApiMetadataDomainService apiMetadataDomainService,
        CreatePlanDomainService createPlanDomainService,
        CreateApiDocumentationDomainService createApiDocumentationDomainService,
        ApiIdsCalculatorDomainService apiIdsCalculatorDomainService,
        MetadataCrudService metadataCrudService,
        DocumentationValidationDomainService documentationValidationDomainService
    ) {
        this.apiImportDomainService = apiImportDomainService;
        this.apiPrimaryOwnerFactory = apiPrimaryOwnerFactory;
        this.createApiDomainService = createApiDomainService;
        this.validateApiDomainService = validateApiDomainService;
        this.apiMetadataDomainService = apiMetadataDomainService;
        this.createPlanDomainService = createPlanDomainService;
        this.createApiDocumentationDomainService = createApiDocumentationDomainService;
        this.apiIdsCalculatorDomainService = apiIdsCalculatorDomainService;
        this.metadataCrudService = metadataCrudService;
        this.documentationValidationDomainService = documentationValidationDomainService;
    }

    public ApiWithFlows create(AuditInfo auditInfo, ImportDefinition importDefinition) {
        var environmentId = auditInfo.environmentId();
        var organizationId = auditInfo.organizationId();
        var primaryOwner = apiPrimaryOwnerFactory.createForNewApi(organizationId, environmentId, auditInfo.actor().userId());
        var apiWithIds = apiIdsCalculatorDomainService.recalculateApiDefinitionIds(environmentId, importDefinition);

        var createdApi = createApiDomainService.create(
            ApiModelFactory.fromApiExport(apiWithIds.getApiExport(), environmentId),
            primaryOwner,
            auditInfo,
            api -> validateApiDomainService.validateAndSanitizeForCreation(api, primaryOwner, environmentId, organizationId)
        );

        new ApiSubEntityCreator(createdApi.getId())
            .addSubEntity("Metadata", () -> createMetadata(importDefinition.getMetadata(), createdApi.getId(), auditInfo))
            .addSubEntity("Pages", () -> createPages(importDefinition.getPages(), createdApi.getId(), auditInfo))
            .addSubEntity("Plans", () -> createPlans(importDefinition.getPlans(), createdApi, auditInfo))
            .addSubEntity("Media", () -> createMedias(importDefinition.getApiMedia(), createdApi.getId()))
            .addSubEntity("Members", () -> createMembers(importDefinition.getMembers(), createdApi.getId()))
            .createAll();

        return createdApi;
    }

    private void createMetadata(Set<NewApiMetadata> metadataSet, String apiId, AuditInfo auditInfo) {
        if (metadataSet != null) {
            metadataSet
                .stream()
                .map(metadata -> metadata.toBuilder().apiId(apiId).build())
                .forEach(metadata ->
                    metadataCrudService
                        .findById(
                            MetadataId.builder().key(metadata.getKey()).referenceId(apiId).referenceType(Metadata.ReferenceType.API).build()
                        )
                        .ifPresentOrElse(
                            existingMetadata ->
                                apiMetadataDomainService.update(
                                    existingMetadata.toBuilder().name(metadata.getName()).value(metadata.getValue()).build(),
                                    auditInfo
                                ),
                            () -> apiMetadataDomainService.create(metadata, auditInfo)
                        )
                );
        }
    }

    private void createPlans(Set<PlanWithFlows> plans, ApiWithFlows api, AuditInfo auditInfo) {
        if (plans != null) {
            plans
                .stream()
                .map(plan -> plan.toBuilder().apiId(api.getId()).build())
                .forEach(plan -> createPlanDomainService.create(plan, plan.getFlows(), api.toApi(), auditInfo));
        }
    }

    private void createPages(List<Page> pages, String apiId, AuditInfo auditInfo) {
        if (pages != null) {
            var now = Date.from(TimeProvider.now().toInstant());
            pages
                .stream()
                .map(page -> {
                    if (page.getParentId() != null) {
                        validatePageParent(pages, page.getParentId());
                    }
                    return documentationValidationDomainService.validateAndSanitizeForCreation(
                        page
                            .toBuilder()
                            .id(page.getId() == null ? UuidString.generateRandom() : page.getId())
                            .referenceType(Page.ReferenceType.API)
                            .referenceId(apiId)
                            .createdAt(now)
                            .updatedAt(now)
                            .build(),
                        auditInfo.organizationId(),
                        false
                    );
                })
                .forEach(page -> createApiDocumentationDomainService.createPage(page, auditInfo));
        }
    }

    private void validatePageParent(List<Page> pages, String parentId) {
        pages
            .stream()
            .filter(parent -> parentId.equals(parent.getId()))
            .findFirst()
            .ifPresent(parent -> {
                if (!(parent.isFolder() || parent.isRoot())) {
                    throw new InvalidPageParentException(parent.getId());
                }
            });
    }

    private void createMedias(List<Media> mediaList, String apiId) {
        if (mediaList != null) {
            apiImportDomainService.createPageAndMedia(mediaList, apiId);
        }
    }

    private void createMembers(Set<ApiMember> members, String apiId) {
        if (members != null) {
            apiImportDomainService.createMembers(members, apiId);
        }
    }
}

class ApiSubEntityCreator {

    private final List<Map.Entry<String, Runnable>> creationParts = new ArrayList<>();
    private final ApiCreatedWithErrorException.ApiCreatedWithErrorExceptionBuilder apiCreatedWithErrorExceptionBuilder;

    ApiSubEntityCreator(String apiId) {
        this.apiCreatedWithErrorExceptionBuilder = new ApiCreatedWithErrorException.ApiCreatedWithErrorExceptionBuilder().apiId(apiId);
    }

    public ApiSubEntityCreator addSubEntity(String partName, Runnable creationPart) {
        creationParts.add(Map.entry(partName, creationPart));
        return this;
    }

    public void createAll() throws ApiCreatedWithErrorException {
        creationParts.forEach(entry -> {
            try {
                entry.getValue().run();
            } catch (Exception e) {
                apiCreatedWithErrorExceptionBuilder.addError(entry.getKey(), e);
            }
        });

        if (apiCreatedWithErrorExceptionBuilder.hasErrors()) {
            throw apiCreatedWithErrorExceptionBuilder.build();
        }
    }
}
