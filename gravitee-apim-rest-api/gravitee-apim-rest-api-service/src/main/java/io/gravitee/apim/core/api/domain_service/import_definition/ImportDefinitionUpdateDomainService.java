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
package io.gravitee.apim.core.api.domain_service.import_definition;

import static io.gravitee.apim.core.api.domain_service.ApiIndexerDomainService.oneShotIndexation;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.domain_service.ApiIdsCalculatorDomainService;
import io.gravitee.apim.core.api.domain_service.ApiImportDomainService;
import io.gravitee.apim.core.api.domain_service.UpdateApiDomainService;
import io.gravitee.apim.core.api.domain_service.UpdateNativeApiDomainService;
import io.gravitee.apim.core.api.domain_service.ValidateApiDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.factory.ApiModelFactory;
import io.gravitee.apim.core.api.model.import_definition.ApiExport;
import io.gravitee.apim.core.api.model.import_definition.ApiMember;
import io.gravitee.apim.core.api.model.import_definition.ImportDefinition;
import io.gravitee.apim.core.api.model.import_definition.ImportDefinitionSubEntityProcessor;
import io.gravitee.apim.core.api.service_provider.ApiImagesServiceProvider;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.media.model.Media;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.definition.model.v4.AbstractApi;
import io.gravitee.definition.model.v4.nativeapi.NativeApi;
import io.gravitee.definition.model.v4.nativeapi.NativeEndpointGroup;
import io.gravitee.definition.model.v4.nativeapi.NativeFlow;
import io.gravitee.definition.model.v4.nativeapi.NativeListener;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;

@DomainService
public class ImportDefinitionUpdateDomainService {

    private final UpdateApiDomainService updateApiDomainService;
    private final ApiImagesServiceProvider apiImagesServiceProvider;
    private final ApiIdsCalculatorDomainService apiIdsCalculatorDomainService;
    private final UpdateNativeApiDomainService updateNativeApiDomainService;
    private final ValidateApiDomainService validateApiDomainService;
    private final ApiPrimaryOwnerDomainService apiPrimaryOwnerDomainService;
    private final ImportDefinitionMetadataDomainService importDefinitionMetadataDomainService;
    private final ImportDefinitionPlanDomainService importDefinitionPlanDomainService;
    private final ImportDefinitionPageDomainService importDefinitionPageDomainService;
    private final ApiImportDomainService apiImportDomainService;

    ImportDefinitionUpdateDomainService(
        UpdateApiDomainService updateApiDomainService,
        ApiImagesServiceProvider apiImagesServiceProvider,
        ApiIdsCalculatorDomainService apiIdsCalculatorDomainService,
        UpdateNativeApiDomainService updateNativeApiDomainService,
        ValidateApiDomainService validateApiDomainService,
        ApiPrimaryOwnerDomainService apiPrimaryOwnerDomainService,
        ImportDefinitionMetadataDomainService importDefinitionMetadataDomainService,
        ImportDefinitionPlanDomainService importDefinitionPlanDomainService,
        ImportDefinitionPageDomainService importDefinitionPageDomainService,
        ApiImportDomainService apiImportDomainService
    ) {
        this.updateApiDomainService = updateApiDomainService;
        this.apiImagesServiceProvider = apiImagesServiceProvider;
        this.apiIdsCalculatorDomainService = apiIdsCalculatorDomainService;
        this.updateNativeApiDomainService = updateNativeApiDomainService;
        this.validateApiDomainService = validateApiDomainService;
        this.apiPrimaryOwnerDomainService = apiPrimaryOwnerDomainService;
        this.importDefinitionMetadataDomainService = importDefinitionMetadataDomainService;
        this.importDefinitionPlanDomainService = importDefinitionPlanDomainService;
        this.importDefinitionPageDomainService = importDefinitionPageDomainService;
        this.apiImportDomainService = apiImportDomainService;
    }

    public Api update(ImportDefinition importDefinition, Api existingPromotedApi, AuditInfo auditInfo) {
        var apiId = existingPromotedApi.getId();
        var apiWithIds = apiIdsCalculatorDomainService.recalculateApiDefinitionIds(auditInfo.environmentId(), importDefinition);
        var apiExport = apiWithIds.getApiExport();

        if (
            (apiExport.getProperties() == null || apiExport.getProperties().isEmpty()) &&
            existingPromotedApi.getApiDefinitionValue() instanceof AbstractApi existingDefinition &&
            existingDefinition.getProperties() != null &&
            !existingDefinition.getProperties().isEmpty()
        ) {
            apiExport.setProperties(existingDefinition.getProperties());
        }

        var updatedApi = switch (existingPromotedApi.getType()) {
            case PROXY, MESSAGE -> updateApiDomainService.updateV4(
                ApiModelFactory.fromApiExport(apiExport, auditInfo.environmentId()).toBuilder().id(apiId).build(),
                auditInfo
            );
            case NATIVE -> updateNativeApi(apiId, apiWithIds.getApiExport(), auditInfo);
            default -> throw new IllegalStateException("Unsupported API type: " + existingPromotedApi.getType());
        };

        apiImagesServiceProvider.updateApiPicture(apiId, apiExport.getPicture(), auditInfo);
        apiImagesServiceProvider.updateApiBackground(apiId, apiExport.getBackground(), auditInfo);

        new ImportDefinitionSubEntityProcessor(updatedApi.getId())
            .addSubEntity("Members", () -> importMembersIfPresent(importDefinition.getMembers(), apiId))
            .addSubEntity("Metadata", () ->
                importDefinitionMetadataDomainService.upsertMetadata(apiId, importDefinition.getMetadata(), auditInfo)
            )
            .addSubEntity("Pages", () -> importDefinitionPageDomainService.upsertPages(apiId, apiWithIds.getPages(), auditInfo))
            .addSubEntity("Plans", () ->
                importDefinitionPlanDomainService.upsertPlanWithFlows(
                    existingPromotedApi,
                    Objects.requireNonNullElse(importDefinition.getPlans(), Set.of()),
                    auditInfo
                )
            )
            .addSubEntity("Media", () -> importMediaIfPresent(importDefinition.getApiMedia(), apiId, auditInfo))
            .process();

        return updatedApi;
    }

    private void importMembersIfPresent(Set<ApiMember> members, String apiId) {
        if (members == null || members.isEmpty()) {
            return;
        }
        apiImportDomainService.createMembers(members, apiId);
    }

    private void importMediaIfPresent(List<Media> apiMedia, String apiId, AuditInfo auditInfo) {
        if (apiMedia == null || apiMedia.isEmpty()) {
            return;
        }
        apiImportDomainService.createMedias(apiMedia, apiId, new ExecutionContext(auditInfo.organizationId(), auditInfo.environmentId()));
    }

    private Api updateNativeApi(String apiId, ApiExport apiExport, AuditInfo auditInfo) {
        var primaryOwner = apiPrimaryOwnerDomainService.getApiPrimaryOwner(auditInfo.organizationId(), apiId);
        var updateOperator = toNativeApiUpdateOperator(apiExport);
        return updateNativeApiDomainService.update(
            apiId,
            updateOperator,
            (existingApi, apiToUpdate) ->
                validateApiDomainService.validateAndSanitizeForUpdate(
                    existingApi,
                    apiToUpdate,
                    primaryOwner,
                    auditInfo.environmentId(),
                    auditInfo.organizationId()
                ),
            auditInfo,
            primaryOwner,
            oneShotIndexation(auditInfo)
        );
    }

    private UnaryOperator<Api> toNativeApiUpdateOperator(ApiExport apiExport) {
        return currentApi ->
            currentApi
                .toBuilder()
                .name(apiExport.getName())
                .description(apiExport.getDescription())
                .version(apiExport.getApiVersion())
                .visibility(
                    apiExport.getVisibility() != null
                        ? Api.Visibility.valueOf(apiExport.getVisibility().toString())
                        : Api.Visibility.PRIVATE
                )
                .labels(apiExport.getLabels())
                .disableMembershipNotifications(apiExport.isDisableMembershipNotifications())
                .allowMultiJwtOauth2Subscriptions(apiExport.isAllowMultiJwtOauth2Subscriptions())
                .apiDefinitionValue(
                    currentApi.getApiDefinitionValue() instanceof NativeApi nativeApi
                        ? nativeApi
                            .toBuilder()
                            .name(apiExport.getName())
                            .apiVersion(apiExport.getApiVersion())
                            .tags(apiExport.getTags())
                            .resources(apiExport.getResources())
                            .listeners(apiExport.getListeners() != null ? (List<NativeListener>) apiExport.getListeners() : null)
                            .endpointGroups(
                                apiExport.getEndpointGroups() != null ? (List<NativeEndpointGroup>) apiExport.getEndpointGroups() : null
                            )
                            .flows(apiExport.getFlows() != null ? (List<NativeFlow>) apiExport.getFlows() : null)
                            .properties(apiExport.getProperties())
                            .build()
                        : null
                )
                .build();
    }
}
