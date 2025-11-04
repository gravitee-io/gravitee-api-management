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

import static io.gravitee.apim.core.api.domain_service.ApiIndexerDomainService.oneShotIndexation;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.factory.ApiModelFactory;
import io.gravitee.apim.core.api.model.import_definition.ApiExport;
import io.gravitee.apim.core.api.model.import_definition.ImportDefinition;
import io.gravitee.apim.core.api.service_provider.ApiImagesServiceProvider;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.definition.model.v4.nativeapi.NativeApi;
import io.gravitee.definition.model.v4.nativeapi.NativeEndpointGroup;
import io.gravitee.definition.model.v4.nativeapi.NativeFlow;
import io.gravitee.definition.model.v4.nativeapi.NativeListener;
import java.util.List;
import java.util.function.UnaryOperator;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@DomainService
public class ImportDefinitionUpdateDomainService {

    private final UpdateApiDomainService updateApiDomainService;
    private final ApiImagesServiceProvider apiImagesServiceProvider;
    private final ApiIdsCalculatorDomainService apiIdsCalculatorDomainService;
    private final UpdateNativeApiDomainService updateNativeApiDomainService;
    private final ValidateApiDomainService validateApiDomainService;
    private final ApiPrimaryOwnerDomainService apiPrimaryOwnerDomainService;

    public ImportDefinitionUpdateDomainService(
        UpdateApiDomainService updateApiDomainService,
        ApiImagesServiceProvider apiImagesServiceProvider,
        ApiIdsCalculatorDomainService apiIdsCalculatorDomainService,
        UpdateNativeApiDomainService updateNativeApiDomainService,
        ValidateApiDomainService validateApiDomainService,
        ApiPrimaryOwnerDomainService apiPrimaryOwnerDomainService
    ) {
        this.updateApiDomainService = updateApiDomainService;
        this.apiImagesServiceProvider = apiImagesServiceProvider;
        this.apiIdsCalculatorDomainService = apiIdsCalculatorDomainService;
        this.updateNativeApiDomainService = updateNativeApiDomainService;
        this.validateApiDomainService = validateApiDomainService;
        this.apiPrimaryOwnerDomainService = apiPrimaryOwnerDomainService;
    }

    public Api update(ImportDefinition importDefinition, Api existingPromotedApi, AuditInfo auditInfo) {
        var apiWithIds = apiIdsCalculatorDomainService.recalculateApiDefinitionIds(auditInfo.environmentId(), importDefinition);
        var apiExport = apiWithIds.getApiExport();

        var updatedApi = switch (existingPromotedApi.getType()) {
            case PROXY, MESSAGE -> updateApiDomainService.updateV4(
                ApiModelFactory.fromApiExport(apiExport, auditInfo.environmentId()),
                auditInfo
            );
            case NATIVE -> updateNativeApi(existingPromotedApi.getId(), apiWithIds.getApiExport(), auditInfo);
            default -> throw new IllegalStateException("Unsupported API type: " + existingPromotedApi.getType());
        };

        apiImagesServiceProvider.updateApiPicture(apiExport.getId(), apiExport.getPicture(), auditInfo);
        apiImagesServiceProvider.updateApiBackground(apiExport.getId(), apiExport.getBackground(), auditInfo);

        return updatedApi;
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
