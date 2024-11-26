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

import static io.gravitee.apim.core.api.domain_service.ApiIndexerDomainService.oneShotIndexation;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.domain_service.UpdateNativeApiDomainService;
import io.gravitee.apim.core.api.domain_service.ValidateApiDomainService;
import io.gravitee.apim.core.api.domain_service.property.PropertyDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.UpdateNativeApi;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.definition.model.v4.property.Property;
import java.util.List;
import java.util.function.UnaryOperator;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UpdateNativeApiUseCase {

    private final ApiPrimaryOwnerDomainService apiPrimaryOwnerDomainService;
    private final PropertyDomainService propertyDomainService;
    private final ValidateApiDomainService validateApiDomainService;
    private final UpdateNativeApiDomainService updateNativeApiDomainService;

    public Output execute(Input input) {
        var updateApi = input.apiToUpdate;
        var auditInfo = input.auditInfo;

        PrimaryOwnerEntity primaryOwnerEntity = apiPrimaryOwnerDomainService.getApiPrimaryOwner(
            auditInfo.organizationId(),
            updateApi.getId()
        );

        var encryptedProperties = propertyDomainService.encryptProperties(input.apiToUpdate().getProperties());

        var updating = update(input.apiToUpdate(), encryptedProperties);

        var updated = updateNativeApiDomainService.update(
            input.apiToUpdate.getId(),
            updating,
            (existingApi, apiToUpdate) ->
                validateApiDomainService.validateAndSanitizeForUpdate(
                    existingApi,
                    apiToUpdate,
                    primaryOwnerEntity,
                    auditInfo.environmentId(),
                    auditInfo.organizationId()
                ),
            auditInfo,
            primaryOwnerEntity,
            oneShotIndexation(auditInfo)
        );

        return new Output(updated, primaryOwnerEntity);
    }

    @Builder
    public record Input(UpdateNativeApi apiToUpdate, AuditInfo auditInfo) {}

    public record Output(Api updatedApi, PrimaryOwnerEntity primaryOwnerEntity) {}

    static UnaryOperator<Api> update(UpdateNativeApi updateNativeApi, List<Property> properties) {
        return currentApi ->
            currentApi
                .toBuilder()
                .name(updateNativeApi.getName())
                .description(updateNativeApi.getDescription())
                .version(updateNativeApi.getApiVersion())
                .apiLifecycleState(updateNativeApi.getLifecycleState())
                .visibility(updateNativeApi.getVisibility())
                .labels(updateNativeApi.getLabels())
                .categories(updateNativeApi.getCategories())
                .groups(updateNativeApi.getGroups())
                .disableMembershipNotifications(updateNativeApi.isDisableMembershipNotifications())
                .apiDefinitionNativeV4(
                    currentApi.getApiDefinitionNativeV4() != null
                        ? currentApi
                            .getApiDefinitionNativeV4()
                            .toBuilder()
                            .name(updateNativeApi.getName())
                            .apiVersion(updateNativeApi.getApiVersion())
                            .tags(updateNativeApi.getTags())
                            .resources(updateNativeApi.getResources())
                            .listeners(updateNativeApi.getListeners())
                            .endpointGroups(updateNativeApi.getEndpointGroups())
                            .flows(updateNativeApi.getFlows())
                            .services(updateNativeApi.getServices())
                            .properties(properties)
                            .build()
                        : null
                )
                .build();
    }
}
