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
import io.gravitee.apim.core.api.domain_service.ApiAuthorizationDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiFieldFilter;
import io.gravitee.apim.core.api.model.ApiQueryCriteria;
import io.gravitee.apim.core.api.model.ApiSearchCriteria;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.category.domain_service.ValidateCategoryDomainService;
import io.gravitee.apim.core.category.model.ApiCategoryOrder;
import io.gravitee.apim.core.category.query_service.ApiCategoryOrderQueryService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetCategoryApisUseCase {

    private final ValidateCategoryDomainService validateCategoryDomainService;
    private final ApiAuthorizationDomainService apiAuthorizationDomainService;
    private final ApiCategoryOrderQueryService apiCategoryOrderQueryService;
    private final ApiQueryService apiQueryService;

    public Output execute(Input input) {
        var categoryId = validateAndGetCategoryId(input.categoryIdOrKey(), input.executionContext().getEnvironmentId());

        // Get api category orders by category id
        var apiCategoryOrders = this.apiCategoryOrderQueryService.findAllByCategoryId(categoryId);

        if (apiCategoryOrders.isEmpty()) {
            return new Output(List.of());
        }

        var apiCategoryOrderApiIds = extractApiIds(apiCategoryOrders);

        var apis =
            this.getUserApisByCategoryAndApis(
                    input.executionContext(),
                    input.isAdmin(),
                    input.currentUserId(),
                    categoryId,
                    apiCategoryOrderApiIds
                )
                .toList();

        var results = mapToResult(apis, apiCategoryOrders).sorted(Comparator.comparingInt(o -> o.apiCategoryOrder.getOrder())).toList();

        return new Output(results);
    }

    private static Stream<Result> mapToResult(List<Api> apis, Set<ApiCategoryOrder> apiCategoryOrders) {
        final Map<String, ApiCategoryOrder> apiCategoryOrderByApi = apiCategoryOrders
            .stream()
            .collect(Collectors.toMap(ApiCategoryOrder::getApiId, Function.identity()));
        return apis
            .stream()
            .map(api ->
                Optional
                    .ofNullable(apiCategoryOrderByApi.get(api.getId()))
                    .map(apiCategoryOrder -> new Result(apiCategoryOrder, api))
                    .orElse(null)
            )
            .filter(Objects::nonNull);
    }

    public record Input(ExecutionContext executionContext, String categoryIdOrKey, String currentUserId, boolean isAdmin) {}

    public record Output(List<Result> results) {}

    public record Result(ApiCategoryOrder apiCategoryOrder, Api api) {}

    private Stream<Api> getUserApisByCategoryAndApis(
        ExecutionContext executionContext,
        boolean isAdmin,
        String userId,
        String categoryId,
        List<String> apiIds
    ) {
        var apiSearchCriteria = ApiSearchCriteria.builder().category(categoryId);

        if (isAdmin) {
            apiSearchCriteria.ids(apiIds);
        } else {
            var apiIdsInUserScope =
                this.apiAuthorizationDomainService.findIdsByUser(
                        executionContext,
                        userId,
                        ApiQueryCriteria.builder().ids(apiIds).category(categoryId).build(),
                        null,
                        false
                    );
            apiSearchCriteria.ids(new ArrayList<>(apiIdsInUserScope));
        }

        return this.apiQueryService.search(apiSearchCriteria.build(), null, ApiFieldFilter.builder().pictureExcluded(true).build());
    }

    private static List<String> extractApiIds(Set<ApiCategoryOrder> apiCategoryOrders) {
        return apiCategoryOrders.stream().map(ApiCategoryOrder::getApiId).toList();
    }

    private String validateAndGetCategoryId(String categoryIdOrKey, String environmentId) {
        return this.validateCategoryDomainService.validateCategoryIdOrKey(categoryIdOrKey, environmentId);
    }
}
