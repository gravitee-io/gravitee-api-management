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
package io.gravitee.apim.infra.domain_service.api;

import io.gravitee.apim.core.api.domain_service.CategoryDomainService;
import io.gravitee.apim.core.api.domain_service.GroupValidationService;
import io.gravitee.apim.core.api.domain_service.ValidateApiDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.flow.domain_service.FlowValidationDomainService;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.infra.adapter.ApiAdapter;
import io.gravitee.apim.infra.adapter.PrimaryOwnerAdapter;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.sanitizer.HtmlSanitizer;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.v4.validation.ApiValidationService;
import io.gravitee.rest.api.service.v4.validation.EndpointGroupsValidationService;
import io.gravitee.rest.api.service.v4.validation.ListenerValidationService;
import io.gravitee.rest.api.service.v4.validation.TagsValidationService;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class ValidateApiDomainServiceLegacyWrapper implements ValidateApiDomainService {

    private final ApiValidationService apiValidationService;
    private final CategoryDomainService categoryDomainService;
    private final FlowValidationDomainService flowValidationDomainService;
    private final TagsValidationService tagsValidationService;
    private final GroupValidationService groupValidationService;
    private final ListenerValidationService listenerValidationService;
    private final EndpointGroupsValidationService endpointGroupsValidationService;

    @Override
    public Api validateAndSanitizeForCreation(Api api, PrimaryOwnerEntity primaryOwner, String environmentId, String organizationId) {
        if (api.getDefinitionVersion() != DefinitionVersion.V4) {
            throw new ValidationDomainException("Definition not supported, should be V4");
        }

        if (api.getType() == ApiType.NATIVE) {
            this.validateAndSanitizeNativeV4(api, primaryOwner, environmentId, organizationId);
        } else {
            this.validateAndSanitizeHttpV4(api, primaryOwner, environmentId, organizationId);
        }

        api.setCategories(categoryDomainService.toCategoryId(api, environmentId));

        return api;
    }

    private void validateAndSanitizeHttpV4(final Api api, PrimaryOwnerEntity primaryOwner, String environmentId, String organizationId) {
        var newApiEntity = ApiAdapter.INSTANCE.toNewApiEntity(api);

        apiValidationService.validateAndSanitizeNewApi(
            new ExecutionContext(organizationId, environmentId),
            newApiEntity,
            PrimaryOwnerAdapter.INSTANCE.toRestEntity(primaryOwner)
        );

        api.setName(newApiEntity.getName());
        api.setVersion(newApiEntity.getApiVersion());
        api.setType(newApiEntity.getType());
        api.setDescription(newApiEntity.getDescription());
        api.setGroups(newApiEntity.getGroups());
        api.setTags(newApiEntity.getTags());

        api.getApiDefinitionHttpV4().setListeners(newApiEntity.getListeners());
        api.getApiDefinitionHttpV4().setEndpointGroups(newApiEntity.getEndpointGroups());
        api.getApiDefinitionHttpV4().setAnalytics(newApiEntity.getAnalytics());
        api.getApiDefinitionHttpV4().setFlowExecution(newApiEntity.getFlowExecution());
        api.getApiDefinitionHttpV4().setFailover(newApiEntity.getFailover());

        api.getApiDefinitionHttpV4().setResources(apiValidationService.validateAndSanitize(api.getApiDefinitionHttpV4().getResources()));

        var sanitizedFlows = flowValidationDomainService.validateAndSanitizeHttpV4(api.getType(), newApiEntity.getFlows());
        api.getApiDefinitionHttpV4().setFlows(sanitizedFlows);

        apiValidationService.validateDynamicProperties(
            api.getApiDefinitionHttpV4().getServices() != null ? api.getApiDefinitionHttpV4().getServices().getDynamicProperty() : null
        );
    }

    private void validateAndSanitizeNativeV4(
        final Api newApi,
        PrimaryOwnerEntity primaryOwner,
        String environmentId,
        String organizationId
    ) {
        if (newApi.getType() != ApiType.NATIVE) {
            throw new ValidationDomainException("Api type not supported, should be NATIVE");
        }

        var executionContext = new ExecutionContext(organizationId, environmentId);

        // Clean description
        newApi.setDescription(HtmlSanitizer.sanitize(newApi.getDescription()));

        // Validate tags
        newApi.setTags(tagsValidationService.validateAndSanitize(executionContext, null, newApi.getTags()));

        // Validate groups
        newApi.setGroups(groupValidationService.validateAndSanitize(Set.of(), newApi.getEnvironmentId(), primaryOwner));

        // Validate and clean listeners
        var apiDefinition = newApi.getApiDefinitionNativeV4();

        apiDefinition.setListeners(
            listenerValidationService.validateAndSanitizeNativeV4(
                executionContext,
                null,
                apiDefinition.getListeners(),
                apiDefinition.getEndpointGroups()
            )
        );

        // Validate and clean endpoints
        apiDefinition.setEndpointGroups(endpointGroupsValidationService.validateAndSanitizeNativeV4(apiDefinition.getEndpointGroups()));

        // Validate and clean flows
        apiDefinition.setFlows(flowValidationDomainService.validateAndSanitizeNativeV4(apiDefinition.getFlows()));

        apiValidationService.validateDynamicProperties(
            newApi.getApiDefinitionNativeV4().getServices() != null
                ? newApi.getApiDefinitionNativeV4().getServices().getDynamicProperty()
                : null
        );
    }
}
