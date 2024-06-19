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

    public ValidateApiDomainServiceLegacyWrapper(ApiValidationService apiValidationService, CategoryDomainService categoryDomainService) {
        this.apiValidationService = apiValidationService;
        this.categoryDomainService = categoryDomainService;
    }

    @Override
    public Api validateAndSanitizeForCreation(Api api, PrimaryOwnerEntity primaryOwner, String environmentId, String organizationId) {
        apiValidationService.validateAndSanitizeNewApi(
            new ExecutionContext(organizationId, environmentId),
            ApiAdapter.INSTANCE.toNewApiEntity(api),
            PrimaryOwnerAdapter.INSTANCE.toRestEntity(primaryOwner)
        );

        api.setCategories(categoryDomainService.toCategoryId(api, environmentId));

        return api;
    }
}
