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
package io.gravitee.apim.core.api.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.domain_service.ApiIdsCalculatorDomainService;
import io.gravitee.apim.core.api.domain_service.ApiImportDomainService;
import io.gravitee.apim.core.api.domain_service.ApiMetadataDomainService;
import io.gravitee.apim.core.api.domain_service.CreateApiDomainService;
import io.gravitee.apim.core.api.domain_service.ValidateApiDomainService;
import io.gravitee.apim.core.api.model.ApiWithFlows;
import io.gravitee.apim.core.api.model.NewApiMetadata;
import io.gravitee.apim.core.api.model.factory.ApiModelFactory;
import io.gravitee.apim.core.api.model.import_definition.ApiExport;
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
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.ApiAlreadyExistsException;
import io.gravitee.rest.api.service.exceptions.ApiDefinitionVersionNotSupportedException;
import java.util.Date;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@UseCase
@Slf4j
public class ImportApiDefinitionUseCase {

    public record Input(ImportDefinition importDefinition, AuditInfo auditInfo) {}

    public record Output(ApiWithFlows apiWithFlows) {}

    private final ApiCrudService apiCrudService;
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

    public ImportApiDefinitionUseCase(
        ApiCrudService apiCrudService,
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
        this.apiCrudService = apiCrudService;
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

    public Output execute(Input input) {
        ensureIsV4Api(input.importDefinition().getApiExport());
        ensureApiDoesNotExist(input);

        var createdApi = this.create(input);
        return new Output(createdApi);
    }

    /**
     * Check no API with the same ID already exist, else throw an exception. Then create the API with the ID from input.
     */
    private void ensureApiDoesNotExist(Input input) {
        var apiId = input.importDefinition().getApiExport().getId();
        if (apiId != null && apiCrudService.existsById(apiId)) {
            throw new ApiAlreadyExistsException(apiId);
        }
    }

    private ApiWithFlows create(Input input) {
        var auditInfo = input.auditInfo;
        var environmentId = auditInfo.environmentId();
        var organizationId = auditInfo.organizationId();
        var primaryOwner = apiPrimaryOwnerFactory.createForNewApi(organizationId, environmentId, auditInfo.actor().userId());
        var apiWithIds = apiIdsCalculatorDomainService.recalculateApiDefinitionIds(environmentId, input.importDefinition);

        var createdApi = createApiDomainService.create(
            ApiModelFactory.fromApiExport(apiWithIds.getApiExport(), environmentId),
            primaryOwner,
            auditInfo,
            api -> validateApiDomainService.validateAndSanitizeForCreation(api, primaryOwner, environmentId, organizationId)
        );

        createMetadata(input.importDefinition.getMetadata(), createdApi.getId(), auditInfo);
        createPages(input.importDefinition.getPages(), createdApi.getId(), auditInfo);
        createPlans(input.importDefinition.getPlans(), createdApi, auditInfo);
        createMedias(input.importDefinition.getApiMedia(), createdApi.getId());
        createMembers(input.importDefinition.getMembers(), createdApi.getId());
        return createdApi;
    }

    private void ensureIsV4Api(ApiExport api) {
        if (api.getDefinitionVersion() != DefinitionVersion.V4) {
            throw new ApiDefinitionVersionNotSupportedException(api.getDefinitionVersion().getLabel());
        }
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
