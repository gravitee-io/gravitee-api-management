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
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.api_product.query_service.ApiProductQueryService;
import io.gravitee.apim.core.api_product.query_service.ApiProductSearchQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetApiProductApisUseCase {

    private final ApiProductQueryService apiProductQueryService;
    private final ApiAuthorizationDomainService apiAuthorizationDomainService;
    private final ApiProductSearchQueryService apiProductSearchQueryService;

    public Output execute(Input input) {
        Optional<ApiProduct> apiProduct = apiProductQueryService.findById(input.apiProductId());

        if (apiProduct.isEmpty()) {
            return new Output(Optional.empty(), null);
        }

        List<String> apiIds = Optional.ofNullable(apiProduct.get().getApiIds())
            .map(ids -> ids.stream().toList())
            .orElse(List.of());

        if (apiIds.isEmpty()) {
            return new Output(apiProduct, emptyPage(input.pageable()));
        }

        List<String> searchIds = apiIds;
        if (!input.isAdmin()) {
            Set<String> manageableIds = apiAuthorizationDomainService.findIdsByUser(
                input.executionContext(),
                input.userId(),
                io.gravitee.apim.core.api.model.ApiQueryCriteria.builder().ids(apiIds).build(),
                null,
                true // manageOnly
            );
            if (manageableIds == null || manageableIds.isEmpty()) {
                return new Output(apiProduct, emptyPage(input.pageable()));
            }
            searchIds = new ArrayList<>(manageableIds);
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

        return new Output(apiProduct, apis);
    }

    private static Page<GenericApiEntity> emptyPage(Pageable pageable) {
        return new Page<>(List.of(), pageable.getPageNumber(), pageable.getPageSize(), 0);
    }

    public record Input(
        ExecutionContext executionContext,
        String apiProductId,
        String query,
        Pageable pageable,
        io.gravitee.rest.api.model.common.Sortable sortable,
        String userId,
        boolean isAdmin
    ) {}

    public record Output(Optional<ApiProduct> apiProduct, Page<GenericApiEntity> apisPage) {}
}
