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
package io.gravitee.apim.core.category.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.domain_service.ApiAuthorizationDomainService;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiQueryCriteria;
import io.gravitee.apim.core.category.crud_service.CategoryApiCrudService;
import io.gravitee.apim.core.category.domain_service.UpdateCategoryApiDomainService;
import io.gravitee.apim.core.category.domain_service.ValidateCategoryDomainService;
import io.gravitee.apim.core.category.model.ApiCategoryOrder;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UpdateCategoryApiOrderUseCase {

    private final ValidateCategoryDomainService validateCategoryDomainService;
    private final ApiAuthorizationDomainService apiAuthorizationDomainService;
    private final UpdateCategoryApiDomainService updateCategoryApiDomainService;
    private final ApiCrudService apiCrudService;
    private final CategoryApiCrudService categoryApiCrudService;

    public Output execute(Input input) {
        var categoryId = validateAndGetCategoryId(input.categoryIdOrKey(), input.executionContext().getEnvironmentId());

        this.validateUserHasAccessToApi(input.executionContext(), input.userId(), input.isAdmin(), categoryId, input.apiId());

        var apiCategoryOrder = validateAndGetApiCategoryAssociation(categoryId, input.apiId());

        // Change order of api category order
        this.updateCategoryApiDomainService.changeOrder(apiCategoryOrder, input.newOrder());

        return new Output(
            new Result(validateAndGetApiCategoryAssociation(categoryId, input.apiId()), this.apiCrudService.get(input.apiId()))
        );
    }

    public record Input(
        ExecutionContext executionContext,
        String categoryIdOrKey,
        String apiId,
        String userId,
        boolean isAdmin,
        int newOrder
    ) {}

    public record Output(Result result) {}

    public record Result(ApiCategoryOrder apiCategoryOrder, Api api) {}

    private void validateUserHasAccessToApi(
        ExecutionContext executionContext,
        String userId,
        boolean isAdmin,
        String categoryId,
        String apiId
    ) {
        if (!isAdmin) {
            var apiIdsInUserScope = this.apiAuthorizationDomainService.findIdsByUser(
                executionContext,
                userId,
                ApiQueryCriteria.builder().ids(List.of(apiId)).category(categoryId).build(),
                null,
                false
            );
            if (apiIdsInUserScope.contains(apiId)) {
                return;
            }
        } else if (this.apiCrudService.existsById(apiId)) {
            return;
        }

        throw new ApiNotFoundException(apiId);
    }

    private String validateAndGetCategoryId(String categoryIdOrKey, String environmentId) {
        return this.validateCategoryDomainService.validateCategoryIdOrKey(categoryIdOrKey, environmentId);
    }

    private ApiCategoryOrder validateAndGetApiCategoryAssociation(String categoryId, String apiId) {
        return this.categoryApiCrudService.get(apiId, categoryId);
    }
}
