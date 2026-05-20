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
package io.gravitee.apim.core.api_product.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.domain_service.ApiAuthorizationDomainService;
import io.gravitee.apim.core.api_product.exception.ApiProductNotFoundException;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.apim.core.api_product.query_service.ApiProductSearchQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.Sortable;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetApiProductApisUseCase {

    private final ApiAuthorizationDomainService apiAuthorizationDomainService;
    private final ApiProductSearchQueryService apiProductSearchQueryService;
    private final ApiProductQueryService apiProductQueryService;

    public Output execute(Input input) {
        ApiProduct apiProduct = apiProductQueryService
            .findById(input.apiProductId())
            .orElseThrow(() -> new ApiProductNotFoundException(input.apiProductId()));
        if (!input.executionContext().getEnvironmentId().equals(apiProduct.getEnvironmentId())) {
            throw new ApiProductNotFoundException(input.apiProductId());
        }

        List<String> apiIds = Optional.ofNullable(apiProduct.getApiIds())
            .map(ids -> ids.stream().toList())
            .orElse(List.of());

        if (apiIds.isEmpty()) {
            return new Output(emptyPage(input.pageable()));
        }

        List<String> searchIds = apiIds;
        if (!input.isAdmin()) {
            Set<String> manageableIds = apiAuthorizationDomainService.findIdsByUser(
                input.executionContext(),
                input.userId(),
                io.gravitee.apim.core.api.model.ApiQueryCriteria.builder().ids(apiIds).build(),
                null,
                true
            );
            if (manageableIds == null || manageableIds.isEmpty()) {
                return new Output(emptyPage(input.pageable()));
            }
            searchIds = apiIds.stream().filter(manageableIds::contains).toList();
            if (searchIds.isEmpty()) {
                return new Output(emptyPage(input.pageable()));
            }
        }

        String query = (input.query() == null || input.query().isBlank()) ? null : input.query().trim();

        Page<GenericApiEntity> apis = apiProductSearchQueryService.searchApis(
            input.executionContext(),
            input.userId(),
            input.isAdmin(),
            searchIds,
            query,
            input.sortable(),
            input.pageable()
        );

        return new Output(apis);
    }

    private static Page<GenericApiEntity> emptyPage(Pageable pageable) {
        return new Page<>(List.of(), pageable.getPageNumber(), pageable.getPageSize(), 0);
    }

    public record Input(
        ExecutionContext executionContext,
        String apiProductId,
        String query,
        Pageable pageable,
        Sortable sortable,
        String userId,
        boolean isAdmin
    ) {}

    public record Output(Page<GenericApiEntity> apisPage) {}
}
