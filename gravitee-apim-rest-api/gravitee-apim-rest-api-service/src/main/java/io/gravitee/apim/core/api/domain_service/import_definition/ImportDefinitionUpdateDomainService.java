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
import static io.gravitee.apim.core.utils.CollectionUtils.isEmpty;

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
import io.gravitee.apim.core.api.model.property.EncryptableProperty;
import io.gravitee.apim.core.api.model.property.PropertyClassificationValidator;
import io.gravitee.apim.core.api.service_provider.ApiImagesServiceProvider;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.group.domain_service.ImportApiGroupsDomainService;
import io.gravitee.apim.core.media.model.Media;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.definition.model.v4.AbstractApi;
import io.gravitee.definition.model.v4.nativeapi.NativeApi;
import io.gravitee.definition.model.v4.nativeapi.NativeEndpointGroup;
import io.gravitee.definition.model.v4.nativeapi.NativeFlow;
import io.gravitee.definition.model.v4.nativeapi.NativeListener;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.HashSet;
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
    private final ImportApiGroupsDomainService importApiGroupsDomainService;

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
        ApiImportDomainService apiImportDomainService,
        ImportApiGroupsDomainService importApiGroupsDomainService
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
        this.importApiGroupsDomainService = importApiGroupsDomainService;
    }

    public Api update(ImportDefinition importDefinition, Api existingPromotedApi, AuditInfo auditInfo) {
        Objects.requireNonNull(existingPromotedApi, "An existing API is required to update from an import definition");
        var apiId = existingPromotedApi.getId();
        // Ids are recalculated against the API we are updating (not against a crossId lookup) so that the update always
        // targets this API even when the imported definition crossId differs from the existing one.
        var apiWithIds = apiIdsCalculatorDomainService.recalculateApiDefinitionIds(
            auditInfo.environmentId(),
            importDefinition,
            existingPromotedApi
        );
        var apiExport = apiWithIds.getApiExport();

        if (
            (apiExport.getProperties() == null || apiExport.getProperties().isEmpty()) &&
            existingPromotedApi.getApiDefinitionValue() instanceof AbstractApi existingDefinition &&
            existingDefinition.getProperties() != null &&
            !existingDefinition.getProperties().isEmpty()
        ) {
            apiExport.setProperties(existingDefinition.getProperties());
        }

        PropertyClassificationValidator.rejectEncryptedToPlain(
            existingPromotedApi.getApiDefinitionValue(),
            toEncryptableProperties(apiExport.getProperties())
        );

        // Defer group resolution for NATIVE APIs: groups are resolved/created only after validation passes.
        // For PROXY/MESSAGE, validation is coupled in ApiService.update, so groups are resolved before update
        // to avoid breaking changes in the legacy service.
        //
        // Null or empty groupNames means "groups field absent / do not change": keep existing membership.
        // OpenAPI models default missing arrays to [], so empty must be treated like null on update.
        // Non-empty names are resolved (and missing names auto-created) then applied as the authoritative set.
        var groupNames = apiExport.getGroups();

        var updatedApi = switch (existingPromotedApi.getType()) {
            case PROXY, MESSAGE -> {
                if (isEmpty(groupNames)) {
                    apiExport.setGroups(null);
                } else {
                    apiExport.setGroups(importApiGroupsDomainService.resolveOrCreateGroupIds(groupNames, auditInfo));
                }
                yield updateApiDomainService.updateV4(
                    ApiModelFactory.fromApiExport(apiExport, auditInfo.environmentId()).toBuilder().id(apiId).build(),
                    auditInfo
                );
            }
            case NATIVE -> {
                apiExport.setGroups(null);
                yield updateNativeApi(apiId, apiExport, groupNames, auditInfo);
            }
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

    private Api updateNativeApi(String apiId, ApiExport apiExport, Set<String> groupNames, AuditInfo auditInfo) {
        var primaryOwner = apiPrimaryOwnerDomainService.getApiPrimaryOwner(auditInfo.organizationId(), apiId);
        var updateOperator = toNativeApiUpdateOperator(apiExport);
        return updateNativeApiDomainService.update(
            apiId,
            updateOperator,
            (existingApi, apiToUpdate) -> {
                // When import specifies groups, clear existing groups before validation so that
                // validated only contains what validation adds (e.g. primary-owner group), not old
                // membership groups. When groups are absent/empty, keep existing groups as-is.
                var apiForValidation = !isEmpty(groupNames) ? apiToUpdate.toBuilder().groups(null).build() : apiToUpdate;
                var validated = validateApiDomainService.validateAndSanitizeForUpdate(
                    existingApi,
                    apiForValidation,
                    primaryOwner,
                    auditInfo.environmentId(),
                    auditInfo.organizationId()
                );
                if (isEmpty(groupNames)) {
                    return validated;
                }
                // Merge resolved import groups with groups validation added (e.g. primary-owner group)
                var resolvedGroupIds = importApiGroupsDomainService.resolveOrCreateGroupIds(groupNames, auditInfo);
                var mergedGroups = mergeGroups(validated.getGroups(), resolvedGroupIds);
                return validated.toBuilder().groups(mergedGroups).build();
            },
            auditInfo,
            primaryOwner,
            oneShotIndexation(auditInfo)
        );
    }

    /**
     * Null-safe union of two group ID sets.
     * Validation may have added primary-owner group; resolved groups come from the import.
     */
    private Set<String> mergeGroups(Set<String> fromValidation, Set<String> fromImport) {
        if (fromValidation == null && fromImport == null) {
            return null;
        }
        var merged = new HashSet<String>();
        if (fromValidation != null) {
            merged.addAll(fromValidation);
        }
        if (fromImport != null) {
            merged.addAll(fromImport);
        }
        return merged.isEmpty() ? null : merged;
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

    private static List<EncryptableProperty> toEncryptableProperties(List<Property> properties) {
        if (properties == null) {
            return null;
        }
        return properties.stream().filter(Objects::nonNull).map(EncryptableProperty::fromProperty).toList();
    }
}
