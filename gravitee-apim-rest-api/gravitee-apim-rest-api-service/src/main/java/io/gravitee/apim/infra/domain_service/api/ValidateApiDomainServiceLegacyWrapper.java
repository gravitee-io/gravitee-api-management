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
import io.gravitee.apim.core.api.domain_service.ValidateApiDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.flow.domain_service.FlowValidationDomainService;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.infra.adapter.ApiAdapter;
import io.gravitee.apim.infra.adapter.PrimaryOwnerAdapter;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.v4.validation.ApiValidationService;
import org.springframework.stereotype.Service;

@Service
public class ValidateApiDomainServiceLegacyWrapper implements ValidateApiDomainService {

    private final ApiValidationService apiValidationService;
    private final CategoryDomainService categoryDomainService;
    private final FlowValidationDomainService flowValidationDomainService;

    public ValidateApiDomainServiceLegacyWrapper(
        ApiValidationService apiValidationService,
        CategoryDomainService categoryDomainService,
        FlowValidationDomainService flowValidationDomainService
    ) {
        this.apiValidationService = apiValidationService;
        this.categoryDomainService = categoryDomainService;
        this.flowValidationDomainService = flowValidationDomainService;
    }

    @Override
    public Api validateAndSanitizeForCreation(Api api, PrimaryOwnerEntity primaryOwner, String environmentId, String organizationId) {
        var newApiEntity = ApiAdapter.INSTANCE.toNewApiEntity(api);

        apiValidationService.validateAndSanitizeNewApi(
            new ExecutionContext(organizationId, environmentId),
            newApiEntity,
            PrimaryOwnerAdapter.INSTANCE.toRestEntity(primaryOwner)
        );

        apiValidationService.validateDynamicProperties(
            api.getApiDefinitionV4().getServices() != null ? api.getApiDefinitionV4().getServices().getDynamicProperty() : null
        );

        api.setName(newApiEntity.getName());
        api.setVersion(newApiEntity.getApiVersion());
        api.setType(newApiEntity.getType());
        api.setDescription(newApiEntity.getDescription());
        api.setGroups(newApiEntity.getGroups());
        api.setTag(newApiEntity.getTags());
        api.setCategories(categoryDomainService.toCategoryId(api, environmentId));
        api.getApiDefinitionV4().setListeners(newApiEntity.getListeners());
        api.getApiDefinitionV4().setEndpointGroups(newApiEntity.getEndpointGroups());
        api.getApiDefinitionV4().setAnalytics(newApiEntity.getAnalytics());
        api.getApiDefinitionV4().setFlowExecution(newApiEntity.getFlowExecution());

        var sanitizedFlows = flowValidationDomainService.validateAndSanitize(api.getType(), newApiEntity.getFlows());
        api.getApiDefinitionV4().setFlows(sanitizedFlows);
        api.getApiDefinitionV4().setFailover(newApiEntity.getFailover());
        api.getApiDefinitionV4().setResources(apiValidationService.validateAndSanitize(api.getApiDefinitionV4().getResources()));

        return api;
    }
}
